import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Core business logic: registration, slot allocation, bookings, exit, capacity and reports. */
public class ParkingService {
    public static final int MAX_SLOTS_PER_TYPE = 200;
    private static final Comparator<ParkingSlot> SLOT_ORDER =
            Comparator.comparing(ParkingSlot::getSupportedType).thenComparingInt(ParkingSlot::getNumber);

    private final Repository repo;
    private final BillingService billing;

    /** Totals shown on the admin dashboard. Money is in whole rupees. */
    public static class Summary {
        public int totalSlots;
        public int occupiedSlots;
        public int vehicles;
        public int bookings;
        public int activeBookings;
        public long collected;
        public long outstanding;
        public long billedToday;
    }

    public ParkingService(Repository repo, BillingService billing) {
        this.repo = repo;
        this.billing = billing;
        seedDefaultSlots();
    }

    /** First run only: 4 cars, 4 bikes, 2 VIP, 2 emergency. The counter makes sure admin changes are never undone. */
    private void seedDefaultSlots() {
        if (repo.nextCounter("default_slots_created") != 1) {
            return;
        }
        int[] defaults = {4, 4, 2, 2};
        List<ParkingSlot> toAdd = new ArrayList<>();
        for (VehicleType type : VehicleType.values()) {
            for (int n = 1; n <= defaults[type.ordinal()]; n++) {
                toAdd.add(new ParkingSlot(ParkingSlot.idFor(type, n), type));
            }
        }
        repo.applySlotChanges(toAdd, new ArrayList<>());
    }

    // ------------------------------------------------------------ vehicles

    public Vehicle registerVehicle(String regNo, String owner, VehicleType type, Role actor) {
        if (type != null && type.needsAdminToRegister() && actor != Role.ADMIN) {
            throw new IllegalStateException("Only an admin can register " + type + " vehicles.");
        }
        Vehicle vehicle = new Vehicle(regNo, owner, type); // validates plate, owner and type
        if (repo.findVehicle(vehicle.getRegistrationNumber()) != null) {
            throw new IllegalStateException("Vehicle " + vehicle.getRegistrationNumber() + " is already registered.");
        }
        repo.addVehicle(vehicle);
        return vehicle;
    }

    private Vehicle requireVehicle(String regNo) {
        Vehicle vehicle = repo.findVehicle(PlateValidator.normalize(regNo));
        if (vehicle == null) {
            throw new IllegalArgumentException("Vehicle not registered: " + PlateValidator.normalize(regNo));
        }
        return vehicle;
    }

    public List<Vehicle> getVehicles() {
        return repo.listVehicles();
    }

    // ------------------------------------------------------------- parking

    public Booking findActiveBooking(Vehicle vehicle) {
        for (Booking b : repo.listBookings()) {
            if (b.isActive() && b.getVehicle().getRegistrationNumber().equals(vehicle.getRegistrationNumber())) {
                return b;
            }
        }
        return null;
    }

    public Booking parkVehicle(String regNo) {
        Vehicle vehicle = requireVehicle(regNo);
        if (findActiveBooking(vehicle) != null) {
            throw new IllegalStateException("Vehicle " + vehicle.getRegistrationNumber() + " is already parked.");
        }
        ParkingSlot chosen = null;
        for (ParkingSlot slot : getSlots()) {
            if (slot.canPark(vehicle.getType())) {
                chosen = slot;
                break;
            }
        }
        if (chosen == null) {
            throw new IllegalStateException("No free " + vehicle.getType() + " slot available.");
        }
        Booking booking = new Booking(String.format("BK-%03d", repo.nextCounter("booking")),
                vehicle, chosen, LocalDateTime.now());
        repo.startBooking(booking);
        chosen.setOccupied(true);
        return booking;
    }

    public Booking exitVehicle(String regNo) {
        return exitVehicle(regNo, LocalDateTime.now());
    }

    public Booking exitVehicle(String regNo, LocalDateTime exitTime) {
        Vehicle vehicle = requireVehicle(regNo);
        Booking booking = findActiveBooking(vehicle);
        if (booking == null) {
            throw new IllegalStateException("Vehicle " + vehicle.getRegistrationNumber() + " is not currently parked.");
        }
        Bill bill = billing.generateBill(booking, exitTime); // throws before any state change
        booking.complete(exitTime, bill);
        repo.completeBooking(booking);
        booking.getSlot().setOccupied(false);
        return booking;
    }

    /** Demo helper: exit as if the vehicle had been parked for the given number of minutes. */
    public Booking exitVehicleAfterMinutes(String regNo, long minutes) {
        if (minutes < 0) {
            throw new IllegalArgumentException("Minutes cannot be negative.");
        }
        Vehicle vehicle = requireVehicle(regNo);
        Booking booking = findActiveBooking(vehicle);
        if (booking == null) {
            throw new IllegalStateException("Vehicle " + vehicle.getRegistrationNumber() + " is not currently parked.");
        }
        return exitVehicle(regNo, booking.getEntryTime().plusMinutes(minutes));
    }

    // --------------------------------------------------------------- bills

    public void markBillPaid(String billId) {
        for (Booking b : repo.listBookings()) {
            Bill bill = b.getBill();
            if (bill != null && bill.getBillId().equalsIgnoreCase(billId.trim())) {
                if (bill.isPaid()) {
                    throw new IllegalStateException("Bill " + bill.getBillId() + " is already paid.");
                }
                repo.markBillPaid(bill.getBillId());
                return;
            }
        }
        throw new IllegalArgumentException("No bill found with ID " + billId);
    }

    public int getRate(VehicleType type) {
        return billing.getRate(type);
    }

    public void setRate(VehicleType type, int rate, Role actor) {
        requireAdmin(actor);
        billing.setRate(type, rate);
    }

    // ------------------------------------------------------------ capacity

    /** Admin only. Grows or shrinks the pool of slots for a vehicle type. Occupied slots are never removed. */
    public void setCapacity(VehicleType type, int count, Role actor) {
        requireAdmin(actor);
        if (count < 0 || count > MAX_SLOTS_PER_TYPE) {
            throw new IllegalArgumentException("Slot count must be between 0 and " + MAX_SLOTS_PER_TYPE + ".");
        }
        List<ParkingSlot> mine = getSlots(type);
        int current = mine.size();
        List<ParkingSlot> toAdd = new ArrayList<>();
        List<String> toRemove = new ArrayList<>();
        if (count > current) {
            for (int n = current + 1; n <= count; n++) {
                toAdd.add(new ParkingSlot(ParkingSlot.idFor(type, n), type));
            }
        } else if (count < current) {
            // Slots are removed from the end (highest number first)
            for (ParkingSlot slot : mine.subList(count, current)) {
                if (slot.isOccupied()) {
                    throw new IllegalStateException("Cannot reduce " + type + " slots to " + count + ": slot "
                            + slot.getSlotId() + " is occupied. Wait for the vehicle to exit first.");
                }
                toRemove.add(slot.getSlotId());
            }
        }
        if (!toAdd.isEmpty() || !toRemove.isEmpty()) {
            repo.applySlotChanges(toAdd, toRemove);
        }
    }

    public int getCapacity(VehicleType type) {
        return getSlots(type).size();
    }

    public int getOccupiedCount(VehicleType type) {
        int n = 0;
        for (ParkingSlot s : getSlots(type)) {
            if (s.isOccupied()) {
                n++;
            }
        }
        return n;
    }

    // ------------------------------------------------------------- queries

    public List<ParkingSlot> getSlots() {
        List<ParkingSlot> slots = repo.listSlots();
        slots.sort(SLOT_ORDER);
        return slots;
    }

    public List<ParkingSlot> getSlots(VehicleType type) {
        List<ParkingSlot> out = new ArrayList<>();
        for (ParkingSlot s : getSlots()) {
            if (s.getSupportedType() == type) {
                out.add(s);
            }
        }
        return out;
    }

    public List<Booking> getAllBookings() {
        return repo.listBookings();
    }

    public List<Booking> getActiveBookings() {
        List<Booking> out = new ArrayList<>();
        for (Booking b : repo.listBookings()) {
            if (b.isActive()) {
                out.add(b);
            }
        }
        return out;
    }

    public Summary getSummary() {
        Summary s = new Summary();
        for (ParkingSlot slot : repo.listSlots()) {
            s.totalSlots++;
            if (slot.isOccupied()) {
                s.occupiedSlots++;
            }
        }
        s.vehicles = repo.listVehicles().size();
        LocalDate today = LocalDate.now();
        for (Booking b : repo.listBookings()) {
            s.bookings++;
            if (b.isActive()) {
                s.activeBookings++;
            }
            Bill bill = b.getBill();
            if (bill != null) {
                long amount = bill.getTotalAmount().longValue();
                if (bill.isPaid()) {
                    s.collected += amount;
                } else {
                    s.outstanding += amount;
                }
                if (b.getExitTime().toLocalDate().equals(today)) {
                    s.billedToday += amount;
                }
            }
        }
        return s;
    }

    private static void requireAdmin(Role actor) {
        if (actor != Role.ADMIN) {
            throw new IllegalStateException("Only an admin can do this.");
        }
    }
}
