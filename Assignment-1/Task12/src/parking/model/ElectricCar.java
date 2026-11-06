package parking.model;

import parking.pricing.Tariffs;

public final class ElectricCar extends Vehicle implements Chargeable {

    private final int batteryKWh;

    public ElectricCar(String regNo, int batteryKWh) {
        super(regNo, VehicleCategory.ELECTRIC_CAR, Tariffs.electricCar());
        if (batteryKWh <= 0) throw new IllegalArgumentException("battery capacity must be positive");
        this.batteryKWh = batteryKWh;
    }

    @Override public double chargingFee(int kWh) {
        if (kWh < 0) throw new IllegalArgumentException("units drawn cannot be negative");
        if (kWh > batteryKWh)
            throw new IllegalArgumentException("cannot draw " + kWh + " kWh into a "
                                              + batteryKWh + " kWh battery");
        return RATE_PER_KWH * kWh;
    }
    public int getBatteryKWh() { return batteryKWh; }
}
