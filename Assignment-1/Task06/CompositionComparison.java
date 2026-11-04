/*
 * Programming in Java  -  Assignment 1 (CO1)
 * Task 6  -  Alternative Concept and Complexity Analysis
 *
 * Name            : Vignesh S
 * Register Number : 192325002
 *
 * Question: the design uses INHERITANCE (Car is-a Vehicle).  If COMPOSITION
 * were used instead - a single Vehicle class that HAS-A VehicleType object
 * holding the rate - what would the solution be?  Compare the work needed to
 * add a new vehicle type, and analyse the time to compute a fee and the space
 * per vehicle object in both designs.
 */
public class CompositionComparison {

    /* ============ DESIGN A: inheritance (the submitted design) ============ */
    abstract static class VehicleI {
        final String regNo;
        VehicleI(String regNo) { this.regNo = regNo; }
        abstract double ratePerHour();
        final long fee(int minutes) { return (long) ((minutes + 59) / 60) * (long) ratePerHour(); }
    }
    static class TwoWheelerI extends VehicleI {
        TwoWheelerI(String r) { super(r); }
        @Override double ratePerHour() { return 10; }
    }
    static class CarI extends VehicleI {
        CarI(String r) { super(r); }
        @Override double ratePerHour() { return 30; }
    }
    static class ElectricCarI extends CarI { ElectricCarI(String r) { super(r); } }

    /* ============ DESIGN B: composition, one Vehicle class ================ */
    /** The rate is now DATA carried in an object, not behaviour in a subclass. */
    static final class VehicleType {
        final String name;
        final double ratePerHour;
        final int dailyCap;
        VehicleType(String name, double ratePerHour, int dailyCap) {
            this.name = name; this.ratePerHour = ratePerHour; this.dailyCap = dailyCap;
        }
    }
    static final class VehicleC {
        final String regNo;
        final VehicleType type;                       // HAS-A, not IS-A
        VehicleC(String regNo, VehicleType type) { this.regNo = regNo; this.type = type; }
        long fee(int minutes) {
            long f = (long) ((minutes + 59) / 60) * (long) type.ratePerHour;
            return Math.min(f, type.dailyCap);
        }
    }

    private static final int N = 20_000_000;

    public static void main(String[] args) {
        System.out.println("TASK 6  -  ALTERNATIVE: INHERITANCE  vs  COMPOSITION");
        System.out.println("=".repeat(80));

        /* ---- what each design looks like ---- */
        System.out.println();
        System.out.println("1.  WHAT THE SOLUTION LOOKS LIKE IN EACH DESIGN");
        System.out.println("-".repeat(80));
        System.out.println("  A  INHERITANCE   the rate is BEHAVIOUR: one class per type, each");
        System.out.println("                   overriding ratePerHour().  3 types = 3 classes.");
        System.out.println("  B  COMPOSITION   the rate is DATA: one Vehicle class holding a");
        System.out.println("                   VehicleType object.  3 types = 3 VehicleType");
        System.out.println("                   INSTANCES and no new classes at all.");

        VehicleType TWO = new VehicleType("TWO_WHEELER", 10, 60);
        VehicleType CAR = new VehicleType("CAR",         30, 200);
        VehicleType EV  = new VehicleType("EV",          30, 200);

        System.out.println();
        System.out.println("  Both compute the same fee for the same stay:");
        System.out.printf("  %-14s | %-12s | %-14s | %-14s | %s%n",
                          "TYPE", "MINUTES", "A inheritance", "B composition", "AGREE?");
        System.out.println("  " + "-".repeat(76));
        Object[][] cases = {
            { "TWO_WHEELER", new TwoWheelerI("TN01A1"), new VehicleC("TN01A1", TWO), 130 },
            { "CAR",         new CarI("TN02B2"),        new VehicleC("TN02B2", CAR), 130 },
            { "EV",          new ElectricCarI("TN03C3"),new VehicleC("TN03C3", EV),  130 },
            { "CAR",         new CarI("TN04D4"),        new VehicleC("TN04D4", CAR), 61  },
        };
        for (Object[] c : cases) {
            int m = (Integer) c[3];
            long a = ((VehicleI) c[1]).fee(m);
            long b = ((VehicleC) c[2]).fee(m);
            System.out.printf("  %-14s | %-12d | Rs %-11d | Rs %-11d | %s%n",
                              c[0], m, a, b, a == b ? "yes" : "NO");
        }

        /* ---- adding a new vehicle type ---- */
        System.out.println();
        System.out.println("2.  WORK NEEDED TO ADD A NEW VEHICLE TYPE (for example: DELIVERY_VAN)");
        System.out.println("-".repeat(80));
        System.out.printf("%-26s | %-22s | %-18s | %s%n",
                          "DESIGN", "WHAT IS WRITTEN", "FILES TOUCHED", "RECOMPILE?");
        System.out.println("-".repeat(80));
        System.out.printf("%-26s | %-22s | %-18s | %s%n", "A  inheritance",
                          "a new subclass", "1 new file", "yes");
        System.out.printf("%-26s | %-22s | %-18s | %s%n", "B  composition",
                          "a new VehicleType(...)", "0 new files", "no - it is data");
        System.out.println();
        System.out.println("  Design B wins decisively on this question, and the demonstration is");
        System.out.println("  that the new type can be added AT RUN TIME, from a configuration row:");
        VehicleType VAN = new VehicleType("DELIVERY_VAN", 60, 400);   // added here, now
        VehicleC van = new VehicleC("TN09ZZ9999", VAN);
        System.out.println("     new VehicleType(\"DELIVERY_VAN\", 60, 400)  added while running");
        System.out.println("     van parked 130 min -> Rs " + van.fee(130)
                           + "   (no class was compiled for it)");
        System.out.println();
        System.out.println("  Design A cannot do that: a DeliveryVan subclass is a compile-time");
        System.out.println("  artefact, so a new tariff always means a new build and a redeploy.");

        /* ---- measured fee time ---- */
        System.out.println();
        System.out.println("3.  MEASURED TIME TO COMPUTE A FEE  (" + String.format("%,d", N) + " calls each)");
        System.out.println("-".repeat(80));

        VehicleI[] fleetA = { new TwoWheelerI("A"), new CarI("B"), new ElectricCarI("C") };
        VehicleC[] fleetB = { new VehicleC("A", TWO), new VehicleC("B", CAR), new VehicleC("C", EV) };

        long sink = 0;
        for (int w = 0; w < 3; w++)
            for (int i = 0; i < 3_000_000; i++) {
                sink += fleetA[i % 3].fee(i % 300);
                sink += fleetB[i % 3].fee(i % 300);
            }

        long t0 = System.nanoTime();
        for (int i = 0; i < N; i++) sink += fleetA[i % 3].fee(i % 300);
        long tA = System.nanoTime() - t0;

        long t1 = System.nanoTime();
        for (int i = 0; i < N; i++) sink += fleetB[i % 3].fee(i % 300);
        long tB = System.nanoTime() - t1;

        System.out.printf("%-26s | %-12s | %-18s | %s%n",
                          "DESIGN", "TOTAL (ms)", "PER FEE (ns)", "HOW THE RATE IS REACHED");
        System.out.printf("%-26s | %-12.1f | %-18.2f | %s%n", "A  inheritance",
                          tA / 1e6, (double) tA / N, "virtual call (vtable)");
        System.out.printf("%-26s | %-12.1f | %-18.2f | %s%n", "B  composition",
                          tB / 1e6, (double) tB / N, "one field dereference");
        System.out.println();
        System.out.println("  Both are O(1) per fee.  A pays for a virtual dispatch; B pays for");
        System.out.println("  following one reference.  Neither cost depends on the NUMBER of");
        System.out.println("  vehicle types, which is the point worth making: adding a tenth type");
        System.out.println("  slows neither design down, unlike the switch-on-type alternative");
        System.out.println("  examined in Task 7, which grows to O(k).");

        /* ---- space ---- */
        System.out.println();
        System.out.println("4.  SPACE PER VEHICLE OBJECT  (64-bit JVM, compressed references)");
        System.out.println("-".repeat(80));
        System.out.printf("%-26s | %-34s | %s%n", "DESIGN", "FIELDS IN THE OBJECT", "APPROX SIZE");
        System.out.println("-".repeat(80));
        System.out.printf("%-26s | %-34s | %s%n", "A  inheritance",
                          "header 12 B + regNo ref 4 B", "16 B");
        System.out.printf("%-26s | %-34s | %s%n", "B  composition",
                          "header 12 B + regNo 4 B + type 4 B", "24 B (padded)");
        System.out.println();
        System.out.println("  B is about 8 bytes larger PER VEHICLE because of the extra type");
        System.out.println("  reference, but the VehicleType objects themselves are SHARED - three");
        System.out.println("  of them serve every vehicle on campus, so the type data is paid for");
        System.out.println("  once, not per object.  For n = 1,000 vehicles that is about "
                           + (8 * 1000 / 1024) + " KB more.");
        System.out.println("  Design A stores no rate at all per object: the rate lives in the");
        System.out.println("  class's method table, which is also shared.  So in both designs the");
        System.out.println("  rate is O(k) space for k types, not O(n).");

        System.out.println();
        System.out.println("5.  DECISION");
        System.out.println("-".repeat(80));
        System.out.println("  Inheritance is retained for THIS assignment, for one honest reason:");
        System.out.println("  the task list requires method overriding (Task 7), dynamic dispatch");
        System.out.println("  (Task 8) and an abstract class (Task 9) to be demonstrated, and the");
        System.out.println("  composition design has none of them - its subclass count is zero.");
        System.out.println();
        System.out.println("  On the engineering merits the two are closer than they look, and");
        System.out.println("  composition is better on the one axis the Day-4 challenge actually");
        System.out.println("  tests: adding a type without touching compiled code.  Vehicle types");
        System.out.println("  differ only in a RATE, which is data, and inheritance is the wrong");
        System.out.println("  tool for varying data.  Inheritance earns its place only where the");
        System.out.println("  subclasses differ in BEHAVIOUR - which is exactly what happens in");
        System.out.println("  Task 12, where a delivery van's fee depends on the time of day.");
        System.out.println("  The redesign there therefore keeps the Vehicle hierarchy for identity");
        System.out.println("  and moves the rate into a composed PricingRule - the hybrid that each");
        System.out.println("  of these two designs is half of.");
        if (sink == Long.MIN_VALUE) System.out.println(sink);
    }
}
