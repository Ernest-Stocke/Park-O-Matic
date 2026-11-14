import java.math.BigDecimal;

/** Invoice generated when a vehicle exits. */
public class Bill {
    private final String billId;
    private final String bookingId;
    private final long durationMinutes;
    private final long billableHours;
    private final BigDecimal hourlyRate;
    private final BigDecimal totalAmount;
    private boolean paid;

    public Bill(String billId, String bookingId, long durationMinutes,
                long billableHours, BigDecimal hourlyRate, BigDecimal totalAmount) {
        this.billId = billId;
        this.bookingId = bookingId;
        this.durationMinutes = durationMinutes;
        this.billableHours = billableHours;
        this.hourlyRate = hourlyRate;
        this.totalAmount = totalAmount;
        this.paid = false;
    }

    public String getBillId() { return billId; }
    public String getBookingId() { return bookingId; }
    public long getDurationMinutes() { return durationMinutes; }
    public long getBillableHours() { return billableHours; }
    public BigDecimal getHourlyRate() { return hourlyRate; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public boolean isPaid() { return paid; }

    /** Demonstration only: flips a status flag, no real payment is processed. */
    public void markPaid() {
        this.paid = true;
    }

    @Override
    public String toString() {
        return billId + " | Booking " + bookingId + " | " + durationMinutes + " min | "
                + billableHours + " hr x Rs." + hourlyRate + " = Rs." + totalAmount
                + " | " + (paid ? "PAID" : "UNPAID");
    }
}
