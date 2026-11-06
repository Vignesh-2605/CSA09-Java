package parking.model;

import parking.pricing.Tariffs;

public class Car extends Vehicle {
    public Car(String regNo) { super(regNo, VehicleCategory.CAR, Tariffs.car()); }
}
