package parking.model;

public final class ElectricCar extends Car implements Chargeable {
    private final int batteryKWh;
    public ElectricCar(String regNo, int batteryKWh) {
        super(regNo);
        if (batteryKWh <= 0) throw new IllegalArgumentException("battery capacity must be positive");
        this.batteryKWh = batteryKWh;
    }
    @Override public double chargingFee(int kWh) {
        if (kWh < 0) throw new IllegalArgumentException("units drawn cannot be negative");
        return RATE_PER_KWH * kWh;
    }
    public int getBatteryKWh() { return batteryKWh; }
}
