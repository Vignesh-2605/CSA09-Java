package parking.model;

/** Unchanged from the original design. */
public interface Chargeable {
    double RATE_PER_KWH = 12.0;
    double chargingFee(int kWh);
    default boolean needsChargingSlot() { return true; }
}
