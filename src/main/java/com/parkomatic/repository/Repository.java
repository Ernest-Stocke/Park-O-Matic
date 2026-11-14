package com.parkomatic.repository;

import java.util.List;

import com.parkomatic.model.Booking;
import com.parkomatic.model.ParkingSlot;
import com.parkomatic.model.User;
import com.parkomatic.model.Vehicle;
import com.parkomatic.model.VehicleType;

/**
 * Storage contract used by the services. JdbcRepository implements it with SQL;
 * the tests use an in-memory implementation.
 */
public interface Repository {
    // ---- users ----
    User findUser(String username);
    List<User> listUsers();
    void addUser(User user);
    void updateUserPassword(String username, String passwordHash);

    // ---- vehicles ----
    Vehicle findVehicle(String registrationNumber);
    List<Vehicle> listVehicles();
    void addVehicle(Vehicle vehicle);

    // ---- slots ----
    List<ParkingSlot> listSlots();
    /** Adds and removes slots in one transaction. */
    void applySlotChanges(List<ParkingSlot> toAdd, List<String> idsToRemove);

    // ---- bookings and bills ----
    List<Booking> listBookings();
    /** Inserts the booking and marks its slot occupied, atomically. */
    void startBooking(Booking booking);
    /** Saves exit time, inserts the bill and frees the slot, atomically. The booking must already be completed. */
    void completeBooking(Booking booking);
    void markBillPaid(String billId);

    // ---- settings ----
    /** Hourly rate in whole rupees, or null if none has been saved for the type. */
    Integer getRate(VehicleType type);
    void setRate(VehicleType type, int rate);

    /** Increments and returns the named counter (used for booking and bill IDs). First call returns 1. */
    int nextCounter(String name);
}
