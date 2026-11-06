package parking.model;

import parking.pricing.Tariffs;

/**
 * NEW VEHICLE CATEGORY 1 of 2.
 *
 * Note what this file does NOT contain: no rate, no cap, no fee method, and no
 * change to any existing class.  It is 8 lines, and it is chargeable as well as
 * parkable - which the original design could also have expressed, since
 * Chargeable was already an interface.
 */
public final class EBike extends Vehicle implements Chargeable {

    public EBike(String regNo) { super(regNo, VehicleCategory.E_BIKE, Tariffs.eBike()); }

    @Override public double chargingFee(int kWh) {
        if (kWh < 0) throw new IllegalArgumentException("units drawn cannot be negative");
        return RATE_PER_KWH * kWh;
    }
}
