/*
 * Programming in Java  -  Assignment 1 (CO1)
 * Task 8  -  Apply Polymorphism  (compile-time and runtime)
 *
 * Name            : Vignesh S
 * Register Number : 192325002
 *
 * The classes are nested inside this file so that it compiles alongside
 * FeeDemo.java in the same folder without clashing with its type names.
 *
 * Shows: an OVERLOADED calculateFee - calculateFee(int minutes) and
 * calculateFee(LocalTime in, LocalTime out) - and how the COMPILER chooses
 * between them; a single Vehicle[] holding three different types by UPCASTING;
 * and a traced dynamic method dispatch showing the declared type, the actual
 * object type and the method body that actually ran.
 */
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;

public class PolymorphismDemo {

    interface Chargeable { double chargingFee(int kWh); }

    abstract static class Vehicle {
        private final String regNo;
        protected Vehicle(String regNo) { this.regNo = regNo; }
        public String getRegNo() { return regNo; }

        protected abstract double ratePerHour();

        /* ---- COMPILE-TIME POLYMORPHISM: two methods, one name ------------ */

        /** Overload 1: the duration is already known in minutes. */
        public long calculateFee(int minutes) {
            if (minutes < 0) throw new IllegalArgumentException("negative duration");
            return (long) ((minutes + 59) / 60) * (long) ratePerHour();
        }

        /** Overload 2: the clock times are known, the duration is not. */
        public long calculateFee(LocalTime in, LocalTime out) {
            int minutes = (int) ChronoUnit.MINUTES.between(in, out);
            if (minutes < 0) minutes += 24 * 60;            // the stay crossed midnight
            return calculateFee(minutes);                    // delegates to overload 1
        }

        /** Overload 3: a flat long, to show widening in overload resolution. */
        public long calculateFee(long minutes) {
            return calculateFee((int) Math.min(minutes, Integer.MAX_VALUE));
        }

        @Override public String toString() { return getClass().getSimpleName() + " " + regNo; }
    }

    static class TwoWheeler extends Vehicle {
        TwoWheeler(String r) { super(r); }
        @Override protected double ratePerHour() {
            System.out.println("            >> body that ran: TwoWheeler.ratePerHour() -> 10");
            return 10;
        }
    }
    static class Car extends Vehicle {
        Car(String r) { super(r); }
        @Override protected double ratePerHour() {
            System.out.println("            >> body that ran: Car.ratePerHour() -> 30");
            return 30;
        }
    }
    static class ElectricCar extends Car implements Chargeable {
        ElectricCar(String r) { super(r); }
        @Override public double chargingFee(int kWh) { return 12.0 * kWh; }
    }

    /* ---- the alternative this design replaces, kept for contrast --------- */
    static long feeWithIfElseChain(Vehicle v, int minutes) {
        int hours = (minutes + 59) / 60;
        if (v instanceof ElectricCar) return hours * 30L;
        else if (v instanceof Car)    return hours * 30L;
        else if (v instanceof TwoWheeler) return hours * 10L;
        else throw new IllegalArgumentException("unknown vehicle type");
    }

    public static void main(String[] args) {
        System.out.println("TASK 8  -  POLYMORPHISM");
        System.out.println("=".repeat(80));

        System.out.println();
        System.out.println("1.  COMPILE-TIME POLYMORPHISM: OVERLOADED calculateFee");
        System.out.println("-".repeat(80));
        Vehicle car = new Car("TN02CD2222");
        System.out.println("  three overloads exist:");
        System.out.println("     calculateFee(int)                  minutes already known");
        System.out.println("     calculateFee(long)                 a wider integral duration");
        System.out.println("     calculateFee(LocalTime, LocalTime) clock times at the barriers");
        System.out.println();
        System.out.println("  a) calculateFee(130)              - argument is an int literal");
        long f1 = car.calculateFee(130);
        System.out.println("     -> Rs " + f1 + "   compiler chose calculateFee(int): exact match");
        System.out.println();
        System.out.println("  b) calculateFee(130L)             - argument is a long literal");
        long f2 = car.calculateFee(130L);
        System.out.println("     -> Rs " + f2 + "   compiler chose calculateFee(long): exact match,");
        System.out.println("        no narrowing is ever applied implicitly");
        System.out.println();
        System.out.println("  c) calculateFee(LocalTime.of(9,0), LocalTime.of(11,10))");
        long f3 = car.calculateFee(LocalTime.of(9, 0), LocalTime.of(11, 10));
        System.out.println("     -> Rs " + f3 + "   two arguments, so only one overload is applicable");
        System.out.println();
        short s = 130;
        System.out.println("  d) calculateFee(shortValue)       - argument is a short");
        long f4 = car.calculateFee(s);
        System.out.println("     -> Rs " + f4 + "   short WIDENS to int; the compiler prefers the");
        System.out.println("        narrowest applicable widening, so calculateFee(int) wins over");
        System.out.println("        calculateFee(long)");
        System.out.println();
        System.out.println("  HOW THE COMPILER SELECTS: it fixes the method at COMPILE time from the");
        System.out.println("  static types of the arguments, in three phases - exact match first,");
        System.out.println("  then widening, then boxing/varargs - and if two are equally specific it");
        System.out.println("  is an ambiguity ERROR, not a runtime choice.  The object's real class");
        System.out.println("  plays no part in this decision at all.");

        System.out.println();
        System.out.println("2.  UPCASTING: ONE ARRAY OF Vehicle HOLDING THREE DIFFERENT TYPES");
        System.out.println("-".repeat(80));
        Vehicle[] parked = { new TwoWheeler("TN01AB1111"),
                             new Car("TN02CD2222"),
                             new ElectricCar("TN03EF3333") };
        System.out.println("  Vehicle[] parked = { new TwoWheeler(..), new Car(..), new ElectricCar(..) };");
        System.out.println();
        System.out.printf("  %-10s | %-14s | %-14s | %s%n",
                          "INDEX", "DECLARED TYPE", "ACTUAL CLASS", "ASSIGNMENT NEEDED");
        System.out.println("  " + "-".repeat(76));
        for (int i = 0; i < parked.length; i++)
            System.out.printf("  %-10d | %-14s | %-14s | %s%n", i, "Vehicle",
                              parked[i].getClass().getSimpleName(), "none - upcast is implicit");
        System.out.println();
        System.out.println("  The upcast is implicit and always safe, because every element really");
        System.out.println("  IS-A Vehicle.  The array's element type is the CONTRACT (anything that");
        System.out.println("  can be parked and billed), not the implementation.");

        System.out.println();
        System.out.println("3.  RUNTIME POLYMORPHISM: DYNAMIC METHOD DISPATCH, TRACED");
        System.out.println("-".repeat(80));
        System.out.println("  one line of source is executed three times:");
        System.out.println("      long fee = v.calculateFee(130);");
        System.out.println();
        for (Vehicle v : parked) {
            System.out.println("    iteration with element " + v);
            System.out.println("        declared type of v   : Vehicle          (fixed at compile time)");
            System.out.println("        actual object type   : " + String.format("%-16s",
                               v.getClass().getSimpleName()) + " (discovered at run time)");
            System.out.println("        method invoked       : Vehicle.calculateFee(int)  [final-style,");
            System.out.println("                               not overridden - one billing rule]");
            System.out.println("        which calls          : ratePerHour()  -> DISPATCHED");
            long fee = v.calculateFee(130);
            String note = "";
            if (v instanceof Chargeable c) { fee += c.chargingFee(10); note = " + charging 10 kWh"; }
            System.out.println("        fee                  : Rs " + fee + note);
            if (v instanceof ElectricCar) {
                System.out.println("                               (ElectricCar inherits Car's rate: an");
                System.out.println("                               EV parks at the car tariff by design)");
            }
            System.out.println();
        }
        System.out.println("  THE MECHANISM: calculateFee() is resolved at compile time to");
        System.out.println("  Vehicle.calculateFee(int), but the ratePerHour() call inside it is");
        System.out.println("  compiled to invokevirtual.  At run time the JVM reads the object's");
        System.out.println("  header, finds its class, looks ratePerHour() up in that class's method");
        System.out.println("  table and jumps there.  The DECLARED type decides what may be called;");
        System.out.println("  the ACTUAL type decides which body runs.");

        System.out.println();
        System.out.println("4.  WHY THIS AVOIDS A LONG if-else CHAIN ON THE VEHICLE TYPE");
        System.out.println("-".repeat(80));
        System.out.println("  the chain that dispatch replaces:");
        System.out.println("      if (v instanceof ElectricCar)  return hours * 30;");
        System.out.println("      else if (v instanceof Car)     return hours * 30;");
        System.out.println("      else if (v instanceof TwoWheeler) return hours * 10;");
        System.out.println("      else throw new IllegalArgumentException(...);");
        System.out.println();
        System.out.printf("  %-18s | %-14s | %-14s | %s%n", "VEHICLE", "dispatch", "if-else chain", "AGREE?");
        System.out.println("  " + "-".repeat(70));
        for (Vehicle v : parked) {
            System.out.println("  (dispatch call for " + v.getClass().getSimpleName() + ")");
            long a = v.calculateFee(130);
            long b = feeWithIfElseChain(v, 130);
            System.out.printf("  %-18s | Rs %-11d | Rs %-11d | %s%n",
                              v.getRegNo(), a, b, a == b ? "yes" : "NO");
        }
        System.out.println();
        System.out.println("  Both give the same answer today.  The chain is still the worse design:");
        System.out.println("   - ORDER MATTERS.  ElectricCar must be tested BEFORE Car, because an");
        System.out.println("     ElectricCar IS-A Car and the Car branch would otherwise swallow it.");
        System.out.println("     Reordering two lines silently changes the bill.");
        System.out.println("   - It lives OUTSIDE the classes, so adding a type means editing a");
        System.out.println("     method that already works and was already tested.");
        System.out.println("   - The final else is reachable, so 'unknown vehicle type' is a runtime");
        System.out.println("     error the compiler cannot warn about.  With dispatch that branch");
        System.out.println("     does not exist, because an unimplemented abstract method is a");
        System.out.println("     compile error instead.");
        System.out.println();
        System.out.println("  Quantified in InstanceofComparison.java in this folder: the chain costs");
        System.out.println("  up to k tests for k vehicle types, dispatch costs one indirect jump.");
    }
}
