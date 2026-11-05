/*
 * Programming in Java  -  Assignment 1 (CO1)
 * Task 7  -  Override and Specialise Behaviour
 *
 * Name            : Vignesh S
 * Register Number : 192325002
 *
 * Shows which methods must be overridden and why, the @Override annotation and
 * the rules it enforces, where final is appropriate for a variable, a method and
 * a class, and toString()/equals()/hashCode() overridden so that two Vehicle
 * objects with the same registration number are treated as EQUAL - including the
 * HashSet and HashMap behaviour that depends on it.
 */
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

abstract class Vehicle {

    /** final VARIABLE: a registration number is the identity of the object and
     *  must never change after construction, or equals()/hashCode() would lie. */
    private final String regNo;

    /** final CONSTANT: shared by every vehicle, cannot be reassigned. */
    protected static final int MINUTES_PER_HOUR = 60;

    protected Vehicle(String regNo) {
        this.regNo = Objects.requireNonNull(regNo, "registration number is mandatory");
    }

    protected abstract double ratePerHour();

    /** final METHOD: the billing RULE ("every started hour in full") is the same
     *  for every vehicle type and a subclass must not be able to reinterpret it.
     *  Only the RATE varies, and that is what ratePerHour() is for. */
    public final long calculateFee(int minutes) {
        if (minutes < 0) throw new IllegalArgumentException("negative duration");
        int hours = (minutes + MINUTES_PER_HOUR - 1) / MINUTES_PER_HOUR;
        return (long) (hours * ratePerHour());
    }

    public String getRegNo() { return regNo; }

    /* ---- the three Object methods, overridden together ------------------- */

    /** Two vehicles are the same vehicle when they carry the same plate -
     *  regardless of which subclass models them. */
    @Override public boolean equals(Object o) {
        if (this == o) return true;                       // fast path, reflexive
        if (!(o instanceof Vehicle)) return false;        // Vehicle, NOT getClass()
        return regNo.equals(((Vehicle) o).regNo);
    }

    /** Must agree with equals(): equal objects MUST give equal hash codes. */
    @Override public int hashCode() { return regNo.hashCode(); }

    @Override public String toString() {
        return getClass().getSimpleName() + "[" + regNo + ", Rs " + (long) ratePerHour() + "/hr]";
    }
}

class TwoWheeler extends Vehicle {
    TwoWheeler(String regNo) { super(regNo); }
    @Override protected double ratePerHour() { return 10; }
}

class Car extends Vehicle {
    Car(String regNo) { super(regNo); }
    @Override protected double ratePerHour() { return 30; }
}

/** final CLASS: an EV is the end of this branch.  Nothing may extend it, so the
 *  combination "car tariff + charging fee" cannot be silently re-specialised. */
final class ElectricCar extends Car {
    private final int batteryKWh;
    ElectricCar(String regNo, int batteryKWh) { super(regNo); this.batteryKWh = batteryKWh; }
    double chargingFee(int kWh) { return 12.0 * kWh; }
    @Override public String toString() {
        return super.toString().replace("]", ", " + batteryKWh + " kWh battery]");
    }
}

public class OverrideDemo {

    public static void main(String[] args) {
        System.out.println("TASK 7  -  OVERRIDING AND SPECIALISING BEHAVIOUR");
        System.out.println("=".repeat(80));

        System.out.println();
        System.out.println("1.  WHICH METHODS ARE OVERRIDDEN, AND WHY");
        System.out.println("-".repeat(80));
        System.out.printf("%-18s | %-16s | %s%n", "METHOD", "OVERRIDDEN IN", "WHY THE SUBCLASS VERSION IS NEEDED");
        System.out.println("-".repeat(80));
        System.out.printf("%-18s | %-16s | %s%n", "ratePerHour()", "every subclass",
                          "the tariff genuinely differs per type");
        System.out.printf("%-18s | %-16s | %s%n", "toString()", "Vehicle, ElectricCar",
                          "a ticket must print readably");
        System.out.printf("%-18s | %-16s | %s%n", "equals()", "Vehicle",
                          "identity is the plate, not the address");
        System.out.printf("%-18s | %-16s | %s%n", "hashCode()", "Vehicle",
                          "must stay consistent with equals()");
        System.out.printf("%-18s | %-16s | %s%n", "calculateFee()", "NOBODY - it is final",
                          "the billing rule must not vary");

        System.out.println();
        System.out.println("2.  THE RULES OF OVERRIDING THAT @Override ENFORCES");
        System.out.println("-".repeat(80));
        System.out.println("  signature    : name and parameter list must match EXACTLY.");
        System.out.println("                 double ratePerHour(int) would be an OVERLOAD, not an");
        System.out.println("                 override - and the abstract method would stay unimplemented.");
        System.out.println("  return type  : identical, or a subtype (covariant return).");
        System.out.println("  access       : may widen (protected -> public) but never narrow.");
        System.out.println("                 protected -> private does not compile.");
        System.out.println("  exceptions   : no NEW or BROADER checked exception may be declared.");
        System.out.println("  static/final : a static method is hidden, not overridden; a final");
        System.out.println("                 method cannot be overridden at all.");
        System.out.println();
        System.out.println("  @Override itself changes no behaviour - it asks the COMPILER to check");
        System.out.println("  that a supertype method really is being replaced.  Without it, a typo");
        System.out.println("  such as 'ratePerHor()' would compile as a brand-new method and the");
        System.out.println("  object would silently keep the inherited rate.  See the commented");
        System.out.println("  experiment at the bottom of this file.");

        System.out.println();
        System.out.println("3.  WHERE final IS APPROPRIATE, AND WHY");
        System.out.println("-".repeat(80));
        System.out.printf("%-26s | %-12s | %s%n", "DECLARATION", "KIND", "REASON");
        System.out.println("-".repeat(80));
        System.out.printf("%-26s | %-12s | %s%n", "private final String regNo", "variable",
                          "identity must not change after equals()");
        System.out.printf("%-26s | %-12s | %s%n", "static final MINUTES_PER_HOUR", "constant",
                          "a unit conversion is not configurable");
        System.out.printf("%-26s | %-12s | %s%n", "public final calculateFee()", "method",
                          "the billing RULE is fixed; only the rate varies");
        System.out.printf("%-26s | %-12s | %s%n", "final class ElectricCar", "class",
                          "closes the branch; no hidden re-specialisation");
        System.out.println();
        System.out.println("  The final on calculateFee() is the important one.  It is what makes the");
        System.out.println("  design safe: a subclass can change WHAT IT COSTS per hour, but it can");
        System.out.println("  never change the rule that a part-hour is charged as a whole hour, so");
        System.out.println("  no future vehicle type can quietly under-bill the campus.");

        System.out.println();
        System.out.println("4.  OVERRIDDEN ratePerHour() SEEN THROUGH A SUPERCLASS REFERENCE");
        System.out.println("-".repeat(80));
        Vehicle[] fleet = { new TwoWheeler("TN01AB1111"), new Car("TN02CD2222"),
                            new ElectricCar("TN03EF3333", 40) };
        System.out.printf("%-14s | %-9s | %-11s | %s%n", "ACTUAL CLASS", "RATE/HR", "FEE 130 min", "toString()");
        System.out.println("-".repeat(80));
        for (Vehicle v : fleet)
            System.out.printf("%-14s | Rs %-6.0f | Rs %-8d | %s%n",
                              v.getClass().getSimpleName(), v.ratePerHour(),
                              v.calculateFee(130), v);

        System.out.println();
        System.out.println("5.  equals(), hashCode() AND toString() WORKING TOGETHER");
        System.out.println("-".repeat(80));
        Vehicle carA = new Car("TN02CD2222");
        Vehicle carB = new Car("TN02CD2222");             // same plate, different object
        Vehicle evSame = new ElectricCar("TN02CD2222", 40); // same plate, different CLASS
        Vehicle other = new Car("TN07XY7777");

        System.out.printf("%-46s | %s%n", "carA == carB  (reference identity)", carA == carB);
        System.out.printf("%-46s | %s%n", "carA.equals(carB)  (same plate, same class)", carA.equals(carB));
        System.out.printf("%-46s | %s%n", "carA.equals(evSame)  (same plate, SUBCLASS)", carA.equals(evSame));
        System.out.printf("%-46s | %s%n", "carA.equals(other)  (different plate)", carA.equals(other));
        System.out.printf("%-46s | %s%n", "carA.hashCode() == carB.hashCode()",
                          carA.hashCode() == carB.hashCode());
        System.out.printf("%-46s | %s%n", "carA.hashCode() == evSame.hashCode()",
                          carA.hashCode() == evSame.hashCode());

        System.out.println();
        System.out.println("  Why instanceof Vehicle and not getClass() == o.getClass():");
        System.out.println("  one physical car has ONE number plate.  If the same plate were");
        System.out.println("  registered once as a Car and once as an ElectricCar, they are still");
        System.out.println("  the same vehicle and the facility must refuse the duplicate entry.");
        System.out.println("  A getClass() comparison would call them different and let the vehicle");
        System.out.println("  occupy two slots at once.  The cost of this choice is that equals()");
        System.out.println("  is no longer symmetric with a subclass that adds its own equality");
        System.out.println("  fields - which is precisely why ElectricCar is declared final.");

        System.out.println();
        System.out.println("6.  THE CONSEQUENCE FOR HashSet AND HashMap");
        System.out.println("-".repeat(80));
        Set<Vehicle> insideNow = new HashSet<>();
        System.out.println("  add(carA)   -> " + insideNow.add(carA)   + "   (new vehicle admitted)");
        System.out.println("  add(carB)   -> " + insideNow.add(carB)   + "   (same plate: REFUSED as a duplicate)");
        System.out.println("  add(evSame) -> " + insideNow.add(evSame) + "   (same plate: REFUSED as a duplicate)");
        System.out.println("  add(other)  -> " + insideNow.add(other)  + "   (different plate admitted)");
        System.out.println("  vehicles recorded inside = " + insideNow.size() + "  (expected 2)");
        System.out.println();
        Map<Vehicle, Integer> entryMinute = new HashMap<>();
        entryMinute.put(carA, 540);
        System.out.println("  entryMinute.put(carA, 540)");
        System.out.println("  entryMinute.get(carB)   -> " + entryMinute.get(carB)
                           + "   (found with a DIFFERENT object, same plate)");
        System.out.println("  entryMinute.get(evSame) -> " + entryMinute.get(evSame)
                           + "   (found across the subclass too)");
        System.out.println();
        System.out.println("  This is the whole point of overriding hashCode() alongside equals():");
        System.out.println("  the exit barrier can look a vehicle up from a freshly scanned plate");
        System.out.println("  without holding the original object.  Had hashCode() been left as");
        System.out.println("  Object's identity hash, get(carB) would return null and every vehicle");
        System.out.println("  would be unable to leave.");
    }
}

/* ---------------------------------------------------------------------------
 * EXPERIMENT: what @Override prevents.
 *
 * Uncommenting the class below gives a COMPILE ERROR, which is the desired
 * outcome - the annotation catches the mistake at build time:
 *
 *     class BrokenBike extends TwoWheeler {
 *         BrokenBike(String r) { super(r); }
 *         @Override protected double ratePerHour(int unused) { return 5; }
 *     }
 *
 *   error: method does not override or implement a method from a supertype
 *
 * Without @Override the same code COMPILES CLEANLY and creates a second,
 * unrelated method; every BrokenBike would then be billed at the inherited
 * Rs 10/hour while the author believed the rate was Rs 5.  Equally, a final
 * method cannot be overridden at all:
 *
 *     class SneakyCar extends Car {
 *         SneakyCar(String r) { super(r); }
 *         @Override public long calculateFee(int m) { return 0; }
 *     }
 *
 *   error: calculateFee(int) in SneakyCar cannot override calculateFee(int)
 *          in Vehicle; overridden method is final
 *
 * That error is the design working as intended: no subclass can make parking
 * free by reinterpreting the billing rule.
 * ------------------------------------------------------------------------- */
