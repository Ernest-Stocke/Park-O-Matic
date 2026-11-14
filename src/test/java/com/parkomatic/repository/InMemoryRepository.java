package com.parkomatic.repository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.parkomatic.model.Booking;
import com.parkomatic.model.ParkingSlot;
import com.parkomatic.model.User;
import com.parkomatic.model.Vehicle;
import com.parkomatic.model.VehicleType;

/** Test-only storage that keeps everything in memory (no database driver needed). */
public class InMemoryRepository implements Repository {
    private final Map<String, User> users = new LinkedHashMap<>();
    private final Map<String, Vehicle> vehicles = new LinkedHashMap<>();
    private final Map<String, ParkingSlot> slots = new LinkedHashMap<>();
    private final List<Booking> bookings = new ArrayList<>();
    private final Map<VehicleType, Integer> rates = new HashMap<>();
    private final Map<String, Integer> counters = new HashMap<>();

    public User findUser(String username) { return users.get(username); }
    public List<User> listUsers() { return new ArrayList<>(users.values()); }
    public void addUser(User user) { users.put(user.getUsername(), user); }
    public void updateUserPassword(String username, String passwordHash) {
        User old = users.get(username);
        users.put(username, new User(username, passwordHash, old.getRole()));
    }

    public Vehicle findVehicle(String reg) { return vehicles.get(reg); }
    public List<Vehicle> listVehicles() { return new ArrayList<>(vehicles.values()); }
    public void addVehicle(Vehicle v) { vehicles.put(v.getRegistrationNumber(), v); }

    public List<ParkingSlot> listSlots() { return new ArrayList<>(slots.values()); }
    public void applySlotChanges(List<ParkingSlot> toAdd, List<String> idsToRemove) {
        for (String id : idsToRemove) {
            if (slots.get(id).isOccupied()) {
                throw new IllegalStateException("occupied");
            }
            slots.remove(id);
        }
        for (ParkingSlot s : toAdd) {
            slots.put(s.getSlotId(), s);
        }
    }

    public List<Booking> listBookings() { return new ArrayList<>(bookings); }
    public void startBooking(Booking b) {
        bookings.add(b);
        slots.get(b.getSlot().getSlotId()).setOccupied(true);
    }
    public void completeBooking(Booking b) { slots.get(b.getSlot().getSlotId()).setOccupied(false); }
    public void markBillPaid(String billId) {
        for (Booking b : bookings) {
            if (b.getBill() != null && b.getBill().getBillId().equals(billId)) {
                b.getBill().markPaid();
            }
        }
    }

    public Integer getRate(VehicleType type) { return rates.get(type); }
    public void setRate(VehicleType type, int rate) { rates.put(type, rate); }
    public int nextCounter(String name) {
        int next = counters.getOrDefault(name, 0) + 1;
        counters.put(name, next);
        return next;
    }
}
