/*
 * Programming in Java  -  Assignment 1 (CO1)
 * Task 9  -  Introduce Abstraction
 *
 * Name            : Vignesh S
 * Register Number : 192325002
 *
 * Separates WHAT the system does from HOW it is done:
 *   abstract class Vehicle   - shared STATE and a partly-written algorithm
 *   interface Chargeable     - an optional CAPABILITY (EVs only)
 *   interface Payable        - an optional CAPABILITY (fee-paying users)
 *   interface Identifiable   - shows a default method and a static method
 * and shows one class gaining several behaviours by implementing more than one
 * interface, which single class inheritance cannot provide.
 */
import java.util.ArrayList;
import java.util.List;

/* =================== INTERFACES: capabilities =================== */

/** Anything that can draw power at a charging slot. */
interface Chargeable {
    double RATE_PER_KWH = 12.0;                    // implicitly public static final

    double chargingFee(int kWh);

    /** DEFAULT METHOD: a sensible shared implementation that most
     *  implementers will not need to override. */
    default boolean needsChargingSlot() { return true; }

    /** STATIC METHOD on an interface: a helper that belongs to the capability
     *  itself, not to any one implementer, and cannot be inherited or
     *  overridden - so it can never be reinterpreted. */
    static double estimateUnits(int batteryKWh, int percentToAdd) {
        if (percentToAdd < 0 || percentToAdd > 100)
            throw new IllegalArgumentException("percent must be 0..100");
        return batteryKWh * percentToAdd / 100.0;
    }
}

/** Anything that can be billed. Separate from Chargeable on purpose. */
interface Payable {
    long amountDue();
    /** DEFAULT METHOD: formatting is presentation, identical for every payer. */
    default String receiptLine() { return String.format("AMOUNT DUE  Rs %d", amountDue()); }
}

/** A capability that the facility uses for logging. */
interface Identifiable {
    String id();
    default String shortId() { return id().length() <= 6 ? id() : id().substring(0, 6) + ".."; }
    static String mask(String id) { return id.replaceAll(".(?=.{2})", "*"); }
}

/* =================== ABSTRACT CLASS: shared state =================== */

/**
 * Vehicle is ABSTRACT because "a vehicle" is never parked - a two-wheeler, a
 * car or an EV is.  It holds the STATE every vehicle has and writes the fee
 * ALGORITHM once, leaving only the varying step - the rate - abstract.
 */
abstract class Vehicle implements Identifiable {

    private final String regNo;                    // state: an interface cannot hold this
    private final int wheels;

    protected Vehicle(String regNo, int wheels) {  // constructor: an interface cannot have one
        if (regNo == null || regNo.isBlank())
            throw new IllegalArgumentException("registration number is mandatory");
        this.regNo = regNo; this.wheels = wheels;
    }

    /* the ONE step that genuinely varies by type */
    protected abstract double ratePerHour();

    /* the ONE step that has a sensible default but may be specialised */
    protected int dailyCap() { return 200; }

    /** TEMPLATE METHOD: written once, in terms of the abstract step above.
     *  final, so no subclass can reinterpret the billing rule. */
    public final long calculateFee(int minutes) {
        if (minutes < 0) throw new IllegalArgumentException("negative duration");
        int hours = (minutes + 59) / 60;
        long fee = (long) hours * (long) ratePerHour();
        return Math.min(fee, dailyCap());
    }

    public String getRegNo() { return regNo; }
    public int getWheels()   { return wheels; }

    @Override public String id() { return regNo; }  // Identifiable satisfied here, once
    @Override public String toString() { return getClass().getSimpleName() + "(" + regNo + ")"; }
}

class TwoWheeler extends Vehicle {
    TwoWheeler(String regNo) { super(regNo, 2); }
    @Override protected double ratePerHour() { return 10; }
    @Override protected int dailyCap() { return 60; }
}

class Car extends Vehicle {
    Car(String regNo) { super(regNo, 4); }
    @Override protected double ratePerHour() { return 30; }
}

/** MULTIPLE CAPABILITIES: one class, one superclass, two interfaces. */
final class ElectricCar extends Car implements Chargeable {
    private final int batteryKWh;
    ElectricCar(String regNo, int batteryKWh) { super(regNo); this.batteryKWh = batteryKWh; }
    @Override public double chargingFee(int kWh) { return RATE_PER_KWH * kWh; }
    int getBatteryKWh() { return batteryKWh; }
}

/** An EV that the owner never charges on campus: it OVERRIDES the default. */
final class HybridCar extends Car implements Chargeable {
    HybridCar(String regNo) { super(regNo); }
    @Override public double chargingFee(int kWh) { return RATE_PER_KWH * kWh; }
    @Override public boolean needsChargingSlot() { return false; }   // runs on petrol too
}

/* =================== users: the other side of the abstraction =================== */

abstract class CampusUser implements Identifiable {
    protected final String name;
    protected CampusUser(String name) { this.name = name; }
    @Override public String id() { return name; }
    public String role() { return getClass().getSimpleName(); }
}

/** A visitor pays, so it implements Payable. */
final class VisitorUser extends CampusUser implements Payable {
    private final long due;
    VisitorUser(String name, long due) { super(name); this.due = due; }
    @Override public long amountDue() { return due; }
}

/** A pass holder does NOT pay, so it simply does not implement Payable.
 *  The absence of the interface is the business rule, enforced by the type. */
final class PassHolder extends CampusUser {
    PassHolder(String name) { super(name); }
}

/* =================== demonstration =================== */

public class AbstractionDemo {

    public static void main(String[] args) {
        System.out.println("TASK 9  -  ABSTRACTION: ABSTRACT CLASSES AND INTERFACES");
        System.out.println("=".repeat(80));

        System.out.println();
        System.out.println("1.  WHAT IS ABSTRACT, AND WHAT IS AN INTERFACE");
        System.out.println("-".repeat(80));
        System.out.printf("%-18s | %-16s | %s%n", "TYPE", "KIND", "WHY THIS KIND");
        System.out.println("-".repeat(80));
        String[][] kinds = {
            {"Vehicle",      "abstract class", "holds STATE (regNo, wheels) and a constructor"},
            {"ratePerHour()","abstract method", "every type has one; none can be inherited"},
            {"dailyCap()",   "concrete method", "a default exists; may be specialised"},
            {"calculateFee()","final method",   "the billing rule must not vary"},
            {"Chargeable",   "interface",      "an OPTIONAL capability, not a kind of thing"},
            {"Payable",      "interface",      "cuts across vehicles AND users"},
            {"Identifiable", "interface",      "anything loggable, no shared state needed"},
        };
        for (String[] r : kinds) System.out.printf("%-18s | %-16s | %s%n", r[0], r[1], r[2]);

        System.out.println();
        System.out.println("2.  WHY Vehicle IS AN ABSTRACT CLASS AND Chargeable IS AN INTERFACE");
        System.out.println("-".repeat(80));
        System.out.println("  Vehicle must be an ABSTRACT CLASS because it needs three things an");
        System.out.println("  interface cannot provide:");
        System.out.println("     - instance STATE: regNo and wheels are fields, not constants;");
        System.out.println("     - a CONSTRUCTOR that validates regNo before any object exists;");
        System.out.println("     - a partly-written algorithm (calculateFee) that calls down into");
        System.out.println("       an abstract step - the template-method pattern.");
        System.out.println();
        System.out.println("  Chargeable must be an INTERFACE because charging is a CAPABILITY, not");
        System.out.println("  a kind of vehicle:");
        System.out.println("     - it has no state of its own, only the RATE_PER_KWH constant;");
        System.out.println("     - it cuts ACROSS the hierarchy - an electric car and a future");
        System.out.println("       e-bike are chargeable but are not each other's relatives;");
        System.out.println("     - Java allows only ONE superclass, so making it a class would");
        System.out.println("       permanently consume ElectricCar's single inheritance slot.");
        System.out.println();
        System.out.println("  The test used throughout: if the answer to 'what IS it?' - abstract");
        System.out.println("  class.  If the answer to 'what can it DO?' - interface.");

        System.out.println();
        System.out.println("3.  Vehicle CANNOT BE INSTANTIATED");
        System.out.println("-".repeat(80));
        System.out.println("     new Vehicle(\"TN01AB1111\", 4);");
        System.out.println("  -> error: Vehicle is abstract; cannot be instantiated");
        System.out.println("  (see compile_errors/compiler_errors.txt in this folder for the real");
        System.out.println("   javac output)");
        System.out.println();
        System.out.println("  This is the abstraction doing its job: the facility can declare a");
        System.out.println("  Vehicle variable and bill through it, but nobody can create a vehicle");
        System.out.println("  with no type and therefore no rate.");

        System.out.println();
        System.out.println("4.  ONE CLASS, SEVERAL BEHAVIOURS THROUGH INTERFACES");
        System.out.println("-".repeat(80));
        ElectricCar ev = new ElectricCar("TN03EF3333", 40);
        System.out.println("  class ElectricCar extends Car implements Chargeable");
        System.out.printf("  %-34s | %s%n", "is-a Vehicle (via Car)", ev instanceof Vehicle);
        System.out.printf("  %-34s | %s%n", "is-a Car", ev instanceof Car);
        System.out.printf("  %-34s | %s%n", "can-do Chargeable", ev instanceof Chargeable);
        System.out.printf("  %-34s | %s%n", "can-do Identifiable (inherited)", ev instanceof Identifiable);
        System.out.println();
        System.out.println("  It has ONE superclass but THREE usable types, so the same object can");
        System.out.println("  be handed to a method that wants a Vehicle, a method that wants a");
        System.out.println("  Chargeable, and a logger that wants an Identifiable - each seeing only");
        System.out.println("  the part of it that concerns them.");

        System.out.println();
        System.out.println("5.  DEFAULT AND STATIC INTERFACE METHODS");
        System.out.println("-".repeat(80));
        HybridCar hy = new HybridCar("TN08PQ8888");
        System.out.printf("  %-40s | %s%n", "ev.needsChargingSlot()   [default used]",
                          ev.needsChargingSlot());
        System.out.printf("  %-40s | %s%n", "hy.needsChargingSlot()   [default OVERRIDDEN]",
                          hy.needsChargingSlot());
        System.out.println();
        System.out.println("  WHERE A DEFAULT METHOD IS USEFUL: needsChargingSlot() is true for");
        System.out.println("  almost every chargeable vehicle, so writing it in every implementer");
        System.out.println("  would be repetition - yet a hybrid genuinely differs.  A default gives");
        System.out.println("  the common answer for free AND lets the exception override it.  It is");
        System.out.println("  also how a capability can gain a new method later without breaking");
        System.out.println("  every class that already implements it.");
        System.out.println();
        System.out.printf("  %-40s | %.1f kWh%n",
                          "Chargeable.estimateUnits(40, 80)  [static]",
                          Chargeable.estimateUnits(40, 80));
        System.out.printf("  %-40s | %s%n", "Identifiable.mask(\"TN03EF3333\")  [static]",
                          Identifiable.mask("TN03EF3333"));
        System.out.printf("  %-40s | %s%n", "ev.shortId()  [default]", ev.shortId());
        System.out.println();
        System.out.println("  WHERE A STATIC INTERFACE METHOD IS USEFUL: estimateUnits() is a helper");
        System.out.println("  about CHARGING in general - it needs no vehicle at all.  Before Java 8");
        System.out.println("  it would have gone into a separate ChargingUtils class, leaving the");
        System.out.println("  helper far from the contract it serves.  As a static interface method");
        System.out.println("  it sits with the capability, and because it cannot be inherited or");
        System.out.println("  overridden, its answer is the same everywhere.");

        System.out.println();
        System.out.println("6.  THE ABSTRACTION IN USE: THE FACILITY SEES ONLY CONTRACTS");
        System.out.println("-".repeat(80));
        List<Vehicle> parked = new ArrayList<>();
        parked.add(new TwoWheeler("TN01AB1111"));
        parked.add(new Car("TN02CD2222"));
        parked.add(ev);
        parked.add(hy);

        System.out.printf("%-14s | %-12s | %-11s | %-12s | %-11s | %s%n",
                          "CLASS", "REG NO", "PARK 130m", "CHARGEABLE", "CHARGE 10kWh", "TOTAL");
        System.out.println("-".repeat(80));
        long collected = 0;
        for (Vehicle v : parked) {
            long park = v.calculateFee(130);
            double charge = 0;
            boolean chargeable = v instanceof Chargeable;
            if (chargeable) charge = ((Chargeable) v).chargingFee(10);
            long total = park + (long) charge;
            collected += total;
            System.out.printf("%-14s | %-12s | Rs %-8d | %-12s | Rs %-9.0f | Rs %d%n",
                              v.getClass().getSimpleName(), v.getRegNo(), park,
                              chargeable ? "yes" : "no", charge, total);
        }
        System.out.println("-".repeat(80));
        System.out.println("  total collected = Rs " + collected);
        System.out.println();
        System.out.println("  The loop body names no concrete vehicle class.  It knows only that");
        System.out.println("  every element can be billed (Vehicle) and that SOME can be charged");
        System.out.println("  (Chargeable).  A new vehicle type therefore changes nothing here -");
        System.out.println("  which is the Day-4 requirement, tested in Task 12.");

        System.out.println();
        System.out.println("7.  Payable: AN INTERFACE THAT CUTS ACROSS TWO HIERARCHIES");
        System.out.println("-".repeat(80));
        CampusUser[] users = { new PassHolder("Vignesh S"), new VisitorUser("Guest", 90),
                               new PassHolder("Dr Ramesh"), new VisitorUser("Vendor", 180) };
        System.out.printf("%-14s | %-12s | %-12s | %s%n", "NAME", "ROLE", "PAYS?", "RECEIPT LINE");
        System.out.println("-".repeat(80));
        long billed = 0;
        for (CampusUser u : users) {
            if (u instanceof Payable p) {
                billed += p.amountDue();
                System.out.printf("%-14s | %-12s | %-12s | %s%n",
                                  u.id(), u.role(), "yes", p.receiptLine());
            } else {
                System.out.printf("%-14s | %-12s | %-12s | %s%n",
                                  u.id(), u.role(), "no", "valid pass - no receipt issued");
            }
        }
        System.out.println("-".repeat(80));
        System.out.println("  billed to visitors = Rs " + billed);
        System.out.println();
        System.out.println("  PassHolder does not implement Payable, so amountDue() cannot even be");
        System.out.println("  CALLED on one - the free-parking rule is enforced by the type system");
        System.out.println("  rather than by an if-statement that somebody could forget to write.");
    }
}
