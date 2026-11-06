package parking.model;

public class TwoWheeler extends Vehicle {
    public TwoWheeler(String regNo) { super(regNo, 2); }
    @Override protected double ratePerHour() { return 10; }
    @Override protected int dailyCap() { return 60; }
}
