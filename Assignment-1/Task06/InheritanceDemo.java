/*
 * Programming in Java  -  Assignment 1 (CO1)
 * Task 6  -  Design the Inheritance Hierarchy
 *
 * Name            : Vignesh S
 * Register Number : 192325002
 *
 * Two hierarchies are designed:
 *   VEHICLES  Vehicle -> TwoWheeler | Car -> ElectricCar      (multilevel)
 *   USERS     CampusUser -> Student | Staff | Visitor          (hierarchical)
 *
 * The program traces the exact order in which constructors execute when an
 * ElectricCar is created, and shows super used for a field, for a method and
 * for a constructor.
 */

/* ======================= VEHICLE HIERARCHY ======================= */

abstract class Vehicle {
    protected final String regNo;            // protected: subclasses read it directly
    protected final int wheels;
    protected static int created = 0;

    protected Vehicle(String regNo, int wheels) {
        this.regNo = regNo;
        this.wheels = wheels;
        created++;
        System.out.println("        3. Vehicle(String,int)      regNo=" + regNo
                           + ", wheels=" + wheels);
    }

    protected abstract double ratePerHour();

    /** Subclasses extend this with super.describe(). */
    public String describe() { return wheels + "-wheeler " + regNo; }

    public String getRegNo() { return regNo; }
}

class TwoWheeler extends Vehicle {
    TwoWheeler(String regNo) {
        super(regNo, 2);                     // SUPER as a CONSTRUCTOR call
        System.out.println("        4. TwoWheeler(String)");
    }
    @Override protected double ratePerHour() { return 10; }
}

class Car extends Vehicle {
    protected final int seats;

    Car(String regNo) { this(regNo, 5); }    // chaining within the subclass

    Car(String regNo, int seats) {
        super(regNo, 4);                     // SUPER: must be the first statement
        this.seats = seats;
        System.out.println("        4. Car(String,int)          seats=" + seats);
    }

    @Override protected double ratePerHour() { return 30; }

    /** Extends, rather than replaces, the inherited description. */
    @Override public String describe() {
        return super.describe() + ", " + seats + " seats";   // SUPER on a METHOD
    }
}

class ElectricCar extends Car {
    private final int batteryKWh;

    ElectricCar(String regNo, int batteryKWh) {
        super(regNo, 5);                     // SUPER: reaches Car, which reaches Vehicle
        this.batteryKWh = batteryKWh;
        System.out.println("        5. ElectricCar(String,int)  battery=" + batteryKWh + " kWh");
    }

    /** Inherits the car rate on purpose, so an EV parks at the car tariff. */
    @Override public String describe() {
        return super.describe() + ", electric " + batteryKWh + " kWh"
               + " (wheels field read through super: " + super.wheels + ")";  // SUPER on a FIELD
    }
}

/* ========================= USER HIERARCHY ========================= */

abstract class CampusUser {
    protected final String name;
    protected CampusUser(String name) { this.name = name; }
    /** Students and staff with a pass park free; visitors always pay. */
    public abstract boolean paysFee();
    public String getName() { return name; }
    public String role() { return getClass().getSimpleName(); }
}
class Student extends CampusUser {
    private final boolean hasPass;
    Student(String name, boolean hasPass) { super(name); this.hasPass = hasPass; }
    @Override public boolean paysFee() { return !hasPass; }
}
class Staff extends CampusUser {
    private final boolean hasPass;
    Staff(String name, boolean hasPass) { super(name); this.hasPass = hasPass; }
    @Override public boolean paysFee() { return !hasPass; }
}
class Visitor extends CampusUser {
    Visitor(String name) { super(name); }
    @Override public boolean paysFee() { return true; }      // a pass does not apply
}

/* ============================== DEMO ============================== */

public class InheritanceDemo {

    public static void main(String[] args) {
        System.out.println("TASK 6  -  INHERITANCE HIERARCHY");
        System.out.println("=".repeat(80));

        System.out.println();
        System.out.println("1.  THE HIERARCHY AND THE KIND OF INHERITANCE USED");
        System.out.println("-".repeat(80));
        System.out.println("     Vehicle  (abstract superclass)");
        System.out.println("        |");
        System.out.println("        +-- TwoWheeler                  <- single inheritance");
        System.out.println("        |");
        System.out.println("        +-- Car                         <- single inheritance");
        System.out.println("              |");
        System.out.println("              +-- ElectricCar           <- MULTILEVEL (3 levels)");
        System.out.println();
        System.out.println("     Vehicle has TWO direct subclasses, which makes the vehicle side");
        System.out.println("     HIERARCHICAL as well; ElectricCar extends Car rather than Vehicle,");
        System.out.println("     which makes that branch MULTILEVEL.  Java has no multiple class");
        System.out.println("     inheritance, so a capability such as Chargeable is an INTERFACE");
        System.out.println("     (Task 9), not a second superclass.");
        System.out.println();
        System.out.println("     CampusUser  (abstract superclass)");
        System.out.println("        +-- Student   +-- Staff   +-- Visitor    <- HIERARCHICAL");

        System.out.println();
        System.out.println("2.  WHAT IS INHERITED AND WHAT EACH SUBCLASS ADDS");
        System.out.println("-".repeat(80));
        System.out.printf("%-14s | %-26s | %-18s | %s%n",
                          "CLASS", "INHERITS", "ADDS", "OVERRIDES");
        System.out.println("-".repeat(80));
        System.out.printf("%-14s | %-26s | %-18s | %s%n", "Vehicle", "-",
                          "regNo, wheels", "-");
        System.out.printf("%-14s | %-26s | %-18s | %s%n", "TwoWheeler", "regNo, wheels, describe()",
                          "-", "ratePerHour()");
        System.out.printf("%-14s | %-26s | %-18s | %s%n", "Car", "regNo, wheels",
                          "seats", "ratePerHour(), describe()");
        System.out.printf("%-14s | %-26s | %-18s | %s%n", "ElectricCar", "regNo, wheels, seats, rate",
                          "batteryKWh", "describe()");
        System.out.println();
        System.out.println("  ElectricCar deliberately does NOT override ratePerHour(): an EV is");
        System.out.println("  parked at the car tariff, and the charging charge is separate (Task 9).");
        System.out.println("  Inheriting the rate is the design statement that an EV IS-A car for");
        System.out.println("  billing purposes.");

        System.out.println();
        System.out.println("3.  CONSTRUCTOR EXECUTION ORDER WHEN AN ElectricCar IS CREATED");
        System.out.println("-".repeat(80));
        System.out.println("  source:  new ElectricCar(\"TN03EF3333\", 40)");
        System.out.println();
        System.out.println("  TRACE (printed by the constructors themselves, in the order they ran):");
        System.out.println("        1. ElectricCar(String,int) body entered, super(regNo,5) called first");
        System.out.println("        2. Car(String,int) body entered, super(regNo,4) called first");
        ElectricCar ev = new ElectricCar("TN03EF3333", 40);
        System.out.println();
        System.out.println("  Reading the printed lines 3, 4, 5 top to bottom gives the rule: the");
        System.out.println("  MOST GENERAL constructor finishes FIRST.  Each subclass constructor");
        System.out.println("  suspends at its super(...) call, lets the parent complete, and only");
        System.out.println("  then runs its own body - so a subclass can never read a field the");
        System.out.println("  superclass has not yet initialised.");
        System.out.println();
        System.out.println("  Formally:  Vehicle(...)  ->  Car(...)  ->  ElectricCar(...)");
        System.out.println("  and if a constructor omits super(...), the compiler inserts an");
        System.out.println("  implicit no-argument super() as its first statement.");

        System.out.println();
        System.out.println("4.  super USED THREE WAYS");
        System.out.println("-".repeat(80));
        System.out.println("  on a CONSTRUCTOR : super(regNo, 4) in Car - passes work upward");
        System.out.println("  on a METHOD      : super.describe() in Car - extends, does not replace");
        System.out.println("  on a FIELD       : super.wheels in ElectricCar - reads the inherited field");
        System.out.println();
        System.out.println("  result: " + ev.describe());

        System.out.println();
        System.out.println("5.  THE WHOLE HIERARCHY EXERCISED THROUGH THE SUPERCLASS TYPE");
        System.out.println("-".repeat(80));
        Vehicle[] fleet = { new TwoWheeler("TN01AB1111"), new Car("TN02CD2222"),
                            new ElectricCar("TN04GH4444", 30) };
        System.out.printf("%-14s | %-12s | %-9s | %s%n", "ACTUAL CLASS", "REG NO", "RATE/HR", "describe()");
        System.out.println("-".repeat(80));
        for (Vehicle v : fleet)
            System.out.printf("%-14s | %-12s | Rs %-6.0f | %s%n",
                              v.getClass().getSimpleName(), v.getRegNo(), v.ratePerHour(), v.describe());
        System.out.println();
        System.out.println("  Vehicles constructed so far (static, inherited by all): " + Vehicle.created);

        System.out.println();
        System.out.println("6.  THE USER HIERARCHY");
        System.out.println("-".repeat(80));
        CampusUser[] users = { new Student("Vignesh S", true), new Student("Arun", false),
                               new Staff("Dr Ramesh", true), new Visitor("Guest") };
        System.out.printf("%-14s | %-10s | %s%n", "NAME", "ROLE", "FEE PAYABLE");
        for (CampusUser u : users)
            System.out.printf("%-14s | %-10s | %s%n", u.getName(), u.role(),
                              u.paysFee() ? "yes, full fee" : "no, valid pass");
        System.out.println();
        System.out.println("  Visitor overrides paysFee() to return true unconditionally, which is");
        System.out.println("  why 'has a pass' is a field of Student and Staff but not of Visitor -");
        System.out.println("  a subclass adds only the state its own rule actually needs.");
    }
}
