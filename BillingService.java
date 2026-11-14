import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;

/** Calculates parking charges: hourly rate per vehicle type, per started hour, minimum one hour. */
public class BillingService {
    public static final int MAX_RATE = 10_000;
    private final Repository repo;

    public BillingService(Repository repo) {
        this.repo = repo;
    }

    /** Current hourly rate (whole rupees) for a vehicle type. */
    public int getRate(VehicleType type) {
        Integer saved = repo.getRate(type);
        return saved != null ? saved : type.getDefaultRate();
    }

    public void setRate(VehicleType type, int rate) {
        if (rate < 0 || rate > MAX_RATE) {
            throw new IllegalArgumentException("Rate must be between 0 and " + MAX_RATE + ".");
        }
        repo.setRate(type, rate);
    }

    /** Rounds partial hours up using integer arithmetic; minimum 1 hour. */
    public long calculateBillableHours(long durationMinutes) {
        long hours = (Math.max(durationMinutes, 0) + 59) / 60;
        return Math.max(hours, 1);
    }

    public Bill generateBill(Booking booking, LocalDateTime exitTime) {
        long minutes = Duration.between(booking.getEntryTime(), exitTime).toMinutes();
        if (minutes < 0) {
            throw new IllegalArgumentException("Exit time cannot be before entry time.");
        }
        long hours = calculateBillableHours(minutes);
        BigDecimal rate = BigDecimal.valueOf(getRate(booking.getVehicle().getType()));
        BigDecimal total = rate.multiply(BigDecimal.valueOf(hours));
        String billId = String.format("BILL-%03d", repo.nextCounter("bill"));
        return new Bill(billId, booking.getBookingId(), minutes, hours, rate, total);
    }
}
