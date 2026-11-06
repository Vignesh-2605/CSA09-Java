package parking.model;

/** What a vehicle is, for slot-fitting purposes. Price is NOT here any more. */
public enum VehicleCategory {
    TWO_WHEELER(1), E_BIKE(1), CAR(2), ELECTRIC_CAR(2), DELIVERY_VAN(3);

    private final int slotUnits;        // how much room it needs
    VehicleCategory(int slotUnits) { this.slotUnits = slotUnits; }
    public int slotUnits() { return slotUnits; }
}
