package parking.model;

public class Car extends Vehicle {
    public Car(String regNo) { super(regNo, 4); }
    @Override protected double ratePerHour() { return 30; }
}
