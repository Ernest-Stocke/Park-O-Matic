package com.parkomatic.model;

import java.time.LocalDateTime;

/** A single parking session linking a vehicle to a slot. */
public class Booking {
    private final String bookingId;
    private final Vehicle vehicle;
    private final ParkingSlot slot;
    private final LocalDateTime entryTime;
    private LocalDateTime exitTime;
    private Bill bill;

    public Booking(String bookingId, Vehicle vehicle, ParkingSlot slot, LocalDateTime entryTime) {
        this.bookingId = bookingId;
        this.vehicle = vehicle;
        this.slot = slot;
        this.entryTime = entryTime;
    }

    public String getBookingId() { return bookingId; }
    public Vehicle getVehicle() { return vehicle; }
    public ParkingSlot getSlot() { return slot; }
    public LocalDateTime getEntryTime() { return entryTime; }
    public LocalDateTime getExitTime() { return exitTime; }
    public Bill getBill() { return bill; }

    /** A booking is active until an exit time has been recorded. */
    public boolean isActive() {
        return exitTime == null;
    }

    /** Records exit time and bill. A completed booking cannot be completed again. */
    public void complete(LocalDateTime exitTime, Bill bill) {
        if (!isActive()) {
            throw new IllegalStateException("Booking " + bookingId + " is already completed.");
        }
        this.exitTime = exitTime;
        this.bill = bill;
    }

    @Override
    public String toString() {
        return bookingId + " | " + vehicle.getRegistrationNumber() + " | Slot " + slot.getSlotId()
                + " | In: " + entryTime.toString().replace('T', ' ')
                + " | " + (isActive() ? "ACTIVE" : "Out: " + exitTime.toString().replace('T', ' '));
    }
}
