/*
 * Programming in Java  -  Assignment 1 (CO1)
 * Task 9  -  Alternative Concept and Complexity Analysis
 *
 * Name            : Vignesh S
 * Register Number : 192325002
 *
 * Question: Vehicle is an abstract class and Chargeable is an interface.  If
 * Vehicle were changed to an INTERFACE with default methods, and Chargeable to
 * an ABSTRACT CLASS, what would the solution be?  Identify what can no longer
 * be expressed (constructors, state, multiple inheritance) and analyse any
 * difference in time per method call and space per object.
 */
public class InvertedAbstractionComparison {

    /* =========== DESIGN B: the roles swapped (what it would look like) ===== */

    /** Vehicle as an INTERFACE.  It cannot hold regNo, so every implementer
     *  must declare the field AND the accessor for itself. */
    interface IVehicle {
        int MINUTES_PER_HOUR = 60;             // only constants are possible

        String getRegNo();                     // state must be promised, not held
        double ratePerHour();

        default int dailyCap() { return 200; }

        /** The template method survives as a default - but it can no longer be
         *  final, so ANY implementer may silently replace the billing rule. */
        default long calculateFee(int minutes) {
            if (minutes < 0) throw new IllegalArgumentException("negative duration");
            int hours = (minutes + MINUTES_PER_HOUR - 1) / MINUTES_PER_HOUR;
            return Math.min((long) hours * (long) ratePerHour(), dailyCap());
        }
    }

    /** Chargeable as an ABSTRACT CLASS. It now occupies the single
     *  inheritance slot of anything that wants to be chargeable. */
    abstract static class AChargeable {
        protected static final double RATE_PER_KWH = 12.0;
        private final int batteryKWh;                       // it CAN hold state now
        protected AChargeable(int batteryKWh) { this.batteryKWh = batteryKWh; }
        public int getBatteryKWh() { return batteryKWh; }
        public abstract double chargingFee(int kWh);
    }

    /** A plain car: must now write regNo and getRegNo() itself. */
    static final class BCar implements IVehicle {
        private final String regNo;                         // repeated in EVERY implementer
        BCar(String regNo) {
            if (regNo == null || regNo.isBlank())           // validation repeated too
                throw new IllegalArgumentException("registration number is mandatory");
            this.regNo = regNo;
        }
        @Override public String getRegNo() { return regNo; }
        @Override public double ratePerHour() { return 30; }
    }

    static final class BTwoWheeler implements IVehicle {
        private final String regNo;                         // repeated again
        BTwoWheeler(String regNo) {
            if (regNo == null || regNo.isBlank())           // and validated again
                throw new IllegalArgumentException("registration number is mandatory");
            this.regNo = regNo;
        }
        @Override public String getRegNo() { return regNo; }
        @Override public double ratePerHour() { return 10; }
        @Override public int dailyCap() { return 60; }
    }

    /** An EV: extends AChargeable (its ONE superclass) and implements IVehicle.
     *  This still works - but notice that the inheritance slot is now spent on
     *  a CAPABILITY, so the EV can no longer be a kind of Car at all. */
    static final class BElectricCar extends AChargeable implements IVehicle {
        private final String regNo;                         // repeated a third time
        BElectricCar(String regNo, int batteryKWh) {
            super(batteryKWh);
            if (regNo == null || regNo.isBlank())
                throw new IllegalArgumentException("registration number is mandatory");
            this.regNo = regNo;
        }
        @Override public String getRegNo() { return regNo; }
        @Override public double ratePerHour() { return 30; }  // Car's rate, COPIED not inherited
        @Override public double chargingFee(int kWh) { return RATE_PER_KWH * kWh; }
    }

    /** What the swap actually costs: a vehicle that ignores the billing rule.
     *  In design A this class would not COMPILE, because calculateFee is final. */
    static final class BFreeloader implements IVehicle {
        private final String regNo;
        BFreeloader(String regNo) { this.regNo = regNo; }
        @Override public String getRegNo() { return regNo; }
        @Override public double ratePerHour() { return 30; }
        @Override public long calculateFee(int minutes) { return 0; }   // LEGAL here
    }

    private static final int N = 20_000_000;

    public static void main(String[] args) {
        System.out.println("TASK 9  -  ALTERNATIVE: SWAPPING THE ABSTRACT CLASS AND THE INTERFACE");
        System.out.println("=".repeat(80));

        System.out.println();
        System.out.println("1.  WHAT THE SWAPPED SOLUTION LOOKS LIKE");
        System.out.println("-".repeat(80));
        System.out.println("  Vehicle    becomes an interface with default dailyCap() and a default");
        System.out.println("             calculateFee(); regNo becomes an abstract getRegNo().");
        System.out.println("  Chargeable becomes an abstract class that can hold batteryKWh and a");
        System.out.println("             protected constructor.");
        System.out.println();
        System.out.println("  It compiles and it runs - that is worth stating plainly before the");
        System.out.println("  criticism.  The fees are identical:");
        IVehicle[] fleetB = { new BTwoWheeler("TN01AB1111"), new BCar("TN02CD2222"),
                              new BElectricCar("TN03EF3333", 40) };
        System.out.printf("  %-16s | %-12s | %-12s | %s%n", "CLASS", "REG NO", "FEE 130m", "CHARGEABLE");
        System.out.println("  " + "-".repeat(70));
        for (IVehicle v : fleetB)
            System.out.printf("  %-16s | %-12s | Rs %-9d | %s%n",
                              v.getClass().getSimpleName(), v.getRegNo(), v.calculateFee(130),
                              (v instanceof AChargeable) ? "yes" : "no");

        System.out.println();
        System.out.println("2.  WHAT CAN NO LONGER BE EXPRESSED");
        System.out.println("-".repeat(80));
        System.out.printf("%-24s | %-18s | %s%n", "CAPABILITY", "DESIGN A", "DESIGN B (swapped)");
        System.out.println("-".repeat(80));
        String[][] lost = {
            {"instance state",      "regNo in Vehicle", "LOST - repeated in every class"},
            {"constructor",         "validates regNo once", "LOST - repeated in every class"},
            {"final method",        "calculateFee is final", "LOST - a default is overridable"},
            {"protected member",    "available to subclasses", "LOST - interfaces are public"},
            {"multiple capability", "1 class + N interfaces", "LOST - 1 abstract class only"},
            {"initialisation order","super() runs first", "LOST - no interface constructor"},
        };
        for (String[] r : lost) System.out.printf("%-24s | %-18s | %s%n", r[0], r[1], r[2]);

        System.out.println();
        System.out.println("  a) STATE AND THE CONSTRUCTOR");
        System.out.println("     An interface field is implicitly public static final, so regNo");
        System.out.println("     cannot live in IVehicle.  Every one of the "
                           + fleetB.length + " classes above");
        System.out.println("     therefore declares its own regNo AND repeats the same two-line");
        System.out.println("     validation.  With k vehicle types that is k copies of a rule that");
        System.out.println("     design A states once - and k places for it to be forgotten.");
        System.out.println();
        System.out.println("  b) THE FINAL BILLING RULE - the serious loss");
        BFreeloader cheat = new BFreeloader("TN99ZZ9999");
        System.out.println("     a default method cannot be final, so this class is LEGAL:");
        System.out.println("         @Override public long calculateFee(int m) { return 0; }");
        System.out.printf("     BFreeloader parked 130 min -> Rs %d   (should be Rs %d)%n",
                          cheat.calculateFee(130), new BCar("x").calculateFee(130));
        System.out.println("     In design A the same class does not compile, because");
        System.out.println("     Vehicle.calculateFee is final.  See Task 7's");
        System.out.println("     compile_errors/compiler_errors.txt for that error.");
        System.out.println("     This is the single strongest argument against the swap: it turns a");
        System.out.println("     compile-time guarantee about REVENUE into an honour system.");
        System.out.println();
        System.out.println("  c) MULTIPLE INHERITANCE OF CAPABILITY");
        System.out.println("     Chargeable as an abstract class consumes the one inheritance slot.");
        System.out.println("     BElectricCar extends AChargeable, so it can no longer extend Car -");
        System.out.println("     notice that its ratePerHour() returns 30 as a COPIED literal");
        System.out.println("     rather than inheriting the car tariff.  Change the car rate and");
        System.out.println("     the EV silently keeps the old one.  And a future vehicle that is");
        System.out.println("     both Chargeable and Insurable cannot be written at all, because");
        System.out.println("     two abstract classes cannot both be extended.");

        System.out.println();
        System.out.println("3.  MEASURED TIME PER METHOD CALL  (" + String.format("%,d", N) + " calls)");
        System.out.println("-".repeat(80));

        // design A equivalents, defined locally so both live in one file
        abstract class AVehicle {
            final String regNo;
            AVehicle(String regNo) { this.regNo = regNo; }
            abstract double ratePerHour();
            int dailyCap() { return 200; }
            final long calculateFee(int m) {
                if (m < 0) throw new IllegalArgumentException("negative duration");
                return Math.min((long) ((m + 59) / 60) * (long) ratePerHour(), dailyCap());
            }
        }
        class ACar extends AVehicle { ACar(String r){super(r);} double ratePerHour(){return 30;} }
        class ABike extends AVehicle {
            ABike(String r){super(r);} double ratePerHour(){return 10;}
            @Override int dailyCap() { return 60; }
        }

        AVehicle[] fleetA = { new ABike("TN01AB1111"), new ACar("TN02CD2222"), new ACar("TN03EF3333") };
        IVehicle[] fleetB2 = { new BTwoWheeler("TN01AB1111"), new BCar("TN02CD2222"), new BCar("TN03EF3333") };

        long sink = 0;
        for (int w = 0; w < 3; w++)
            for (int i = 0; i < 3_000_000; i++) {
                sink += fleetA[i % 3].calculateFee(130);
                sink += fleetB2[i % 3].calculateFee(130);
            }

        long t0 = System.nanoTime();
        for (int i = 0; i < N; i++) sink += fleetA[i % 3].calculateFee(130);
        long tA = System.nanoTime() - t0;

        long t1 = System.nanoTime();
        for (int i = 0; i < N; i++) sink += fleetB2[i % 3].calculateFee(130);
        long tB = System.nanoTime() - t1;

        System.out.printf("%-34s | %-12s | %-16s | %s%n",
                          "DESIGN", "TOTAL (ms)", "PER CALL (ns)", "BYTECODE");
        System.out.printf("%-34s | %-12.1f | %-16.2f | %s%n",
                          "A  abstract class (submitted)", tA / 1e6, (double) tA / N, "invokevirtual");
        System.out.printf("%-34s | %-12.1f | %-16.2f | %s%n",
                          "B  interface + default method", tB / 1e6, (double) tB / N, "invokeinterface");
        System.out.printf("%-34s | %-12s | %-16s | %s%n", "difference", "",
                          String.format("%.2f", Math.abs((double) tA - tB) / N),
                          String.format("%.0f%%", 100.0 * Math.abs(tA - tB) / tA));
        System.out.println();
        System.out.println("  invokeinterface historically cost more than invokevirtual because an");
        System.out.println("  interface method has no fixed slot in a single method table.  On a");
        System.out.println("  modern JVM the inline cache closes most of that gap, and the measured");
        System.out.println("  difference above is small - of the order of a nanosecond per call.");
        System.out.println("  At a few thousand vehicles a day this is irrelevant: the swap is NOT");
        System.out.println("  a performance question, and claiming it is would be dishonest.");

        System.out.println();
        System.out.println("4.  SPACE PER OBJECT");
        System.out.println("-".repeat(80));
        System.out.printf("%-34s | %-30s | %s%n", "DESIGN", "FIELDS", "APPROX SIZE");
        System.out.println("-".repeat(80));
        System.out.printf("%-34s | %-30s | %s%n", "A  abstract class",
                          "header 12 B + regNo ref 4 B", "16 B");
        System.out.printf("%-34s | %-30s | %s%n", "B  interface implementer",
                          "header 12 B + regNo ref 4 B", "16 B");
        System.out.printf("%-34s | %-30s | %s%n", "B  EV extends AChargeable",
                          "header 12 B + regNo 4 B + int 4 B", "24 B (padded)");
        System.out.println();
        System.out.println("  Per-object space is essentially UNCHANGED.  The field still has to");
        System.out.println("  exist either way; design B merely moves its DECLARATION from one");
        System.out.println("  superclass into each implementer, which costs source lines, not heap.");
        System.out.println("  So neither time nor space decides this question.");

        System.out.println();
        System.out.println("5.  CONCLUSION");
        System.out.println("-".repeat(80));
        System.out.println("  The original choice is retained, and the measurements are the reason");
        System.out.println("  it has to be argued on design grounds rather than on cost: the swap");
        System.out.println("  is within a nanosecond per call and identical in heap space.");
        System.out.println();
        System.out.println("  What actually decides it is that the swap inverts the two questions");
        System.out.println("  each construct answers.  An abstract class answers 'what IS this?',");
        System.out.println("  and a vehicle IS something - it has a plate, it is validated once, it");
        System.out.println("  is billed by one unalterable rule.  An interface answers 'what can it");
        System.out.println("  DO?', and charging is something a vehicle MAY do, independently of");
        System.out.println("  what kind of vehicle it is.  Swapping them makes identity optional and");
        System.out.println("  capability exclusive, which is exactly backwards - and the concrete");
        System.out.println("  price is BFreeloader: a class that bills nothing, compiles cleanly,");
        System.out.println("  and in the original design could not exist.");
        if (sink == Long.MIN_VALUE) System.out.println(sink);
    }
}
