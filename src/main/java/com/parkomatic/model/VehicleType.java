package com.parkomatic.model;

/** Vehicle categories. Each has its own slot pool, slot-ID prefix and default hourly rate. */
public enum VehicleType {
    CAR("C", "Car", 30),
    BIKE("B", "Bike", 30),
    VIP("V", "VIP", 30),
    EMERGENCY("E", "Emergency", 0); // ambulances, fire engines, police: free by default (admin can change)

    private final String slotPrefix;
    private final String label;
    private final int defaultRate;

    VehicleType(String slotPrefix, String label, int defaultRate) {
        this.slotPrefix = slotPrefix;
        this.label = label;
        this.defaultRate = defaultRate;
    }

    public String getSlotPrefix() { return slotPrefix; }
    public String getLabel() { return label; }
    public int getDefaultRate() { return defaultRate; }

    /** VIP and Emergency registrations are restricted to admins. */
    public boolean needsAdminToRegister() {
        return this == VIP || this == EMERGENCY;
    }

    @Override
    public String toString() { return label; }
}
