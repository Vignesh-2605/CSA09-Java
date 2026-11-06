package parking.model;

import parking.pricing.Tariffs;

/** UNCHANGED in behaviour: same plate, same price. Only the constructor differs. */
public class TwoWheeler extends Vehicle {
    public TwoWheeler(String regNo) {
        super(regNo, VehicleCategory.TWO_WHEELER, Tariffs.twoWheeler());
    }
}
