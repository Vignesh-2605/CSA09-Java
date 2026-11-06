package parking.model;

/** An optional capability: the vehicle can draw power at a charging slot. */
public interface Chargeable {
    double RATE_PER_KWH = 12.0;
    double chargingFee(int kWh);
    default boolean needsChargingSlot() { return true; }
}
