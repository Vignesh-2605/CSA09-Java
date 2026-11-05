/*
 * Programming in Java  -  Assignment 1 (CO1)
 * Task 8  -  Apply Polymorphism  ("Complete the Code")
 *
 * Name            : Vignesh S
 * Register Number : 192325002
 *
 * This single file applies inheritance (Task 6), method overriding (Task 7),
 * polymorphism (Task 8) and an interface (Task 9).
 * The given skeleton and its main() method are unchanged; only TODO 1, TODO 2
 * and TODO 3 have been completed.
 */
interface Chargeable {
    double chargingFee(int kWh);
}

abstract class Vehicle {
    private final String regNo;

    protected Vehicle(String regNo) { this.regNo = regNo; }

    public String getRegNo() { return regNo; }

    protected abstract double ratePerHour();

    /** Every started hour is charged in full. */
    public double calculateFee(int minutes) {
        // TODO 1: Compute billable hours as the ceiling of minutes / 60 using integer
        //   arithmetic only, and return hours x ratePerHour()
        int billableHours = (minutes + 59) / 60;   // integer ceiling of minutes / 60
        return billableHours * ratePerHour();
    }

    @Override public boolean equals(Object o) {
        return o instanceof Vehicle && regNo.equals(((Vehicle) o).regNo);
    }

    @Override public int hashCode() { return regNo.hashCode(); }
}

class TwoWheeler extends Vehicle {
    TwoWheeler(String regNo) { super(regNo); }
    @Override protected double ratePerHour() { return 10; }
}

class Car extends Vehicle {
    // TODO 2: Write the constructor using super, and override ratePerHour() to return
    //   30
    Car(String regNo) { super(regNo); }
    @Override protected double ratePerHour() { return 30; }
}

class ElectricCar extends Car implements Chargeable {
    ElectricCar(String regNo) { super(regNo); }
    // TODO 3: Implement chargingFee() at Rs 12 per kWh
    @Override public double chargingFee(int kWh) { return 12.0 * kWh; }
}

public class FeeDemo {
    public static void main(String[] args) {
        Vehicle[] parked = { new TwoWheeler("TN01A1"), new Car("TN02B2"),
                             new ElectricCar("TN03C3") };
        for (Vehicle v : parked) {
            double fee = v.calculateFee(130);          // dynamic method dispatch
            if (v instanceof Chargeable c) fee += c.chargingFee(10);
            System.out.println(v.getClass().getSimpleName() + " " + v.getRegNo()
                    + " -> Rs " + fee);
        }
        System.out.println(new Car("TN02B2").equals(new ElectricCar("TN02B2")));
    }
}
