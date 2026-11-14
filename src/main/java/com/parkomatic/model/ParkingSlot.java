package com.parkomatic.model;

/** One parking space that supports a single vehicle type. IDs look like C01, B02, V01, E01. */
public class ParkingSlot {
    private final String slotId;
    private final VehicleType supportedType;
    private boolean occupied;

    public ParkingSlot(String slotId, VehicleType supportedType) {
        this.slotId = slotId;
        this.supportedType = supportedType;
        this.occupied = false;
    }

    /** Builds the ID for the n-th slot of a type, e.g. (CAR, 3) -> "C03". */
    public static String idFor(VehicleType type, int number) {
        return type.getSlotPrefix() + String.format("%02d", number);
    }

    public String getSlotId() { return slotId; }
    public VehicleType getSupportedType() { return supportedType; }
    public boolean isOccupied() { return occupied; }
    public void setOccupied(boolean occupied) { this.occupied = occupied; }

    /** The numeric part of the ID (C03 -> 3), used for sorting. */
    public int getNumber() {
        try {
            return Integer.parseInt(slotId.substring(1));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /** A slot can be allocated if it fits the vehicle type and is free. */
    public boolean canPark(VehicleType type) {
        return !occupied && supportedType == type;
    }

    @Override
    public String toString() {
        return slotId + " [" + supportedType + "] - " + (occupied ? "OCCUPIED" : "FREE");
    }
}
