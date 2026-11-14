package com.parkomatic.model;

import com.parkomatic.util.PlateValidator;

/** A registered vehicle. Immutable: all fields are private and final. */
public class Vehicle {
    private final String registrationNumber;
    private final String ownerName;
    private final VehicleType type;

    public Vehicle(String registrationNumber, String ownerName, VehicleType type) {
        if (ownerName == null || ownerName.trim().isEmpty()) {
            throw new IllegalArgumentException("Owner name cannot be empty.");
        }
        if (type == null) {
            throw new IllegalArgumentException("Vehicle type is required.");
        }
        // Validates the Indian plate format and normalises it (upper-case, no spaces/hyphens)
        this.registrationNumber = PlateValidator.normalizeAndValidate(registrationNumber);
        this.ownerName = ownerName.trim();
        this.type = type;
    }

    public String getRegistrationNumber() { return registrationNumber; }
    public String getOwnerName() { return ownerName; }
    public VehicleType getType() { return type; }

    @Override
    public String toString() {
        return registrationNumber + " (" + type + ", owner: " + ownerName + ")";
    }
}
