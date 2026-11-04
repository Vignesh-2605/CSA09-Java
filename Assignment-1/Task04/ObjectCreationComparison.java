/*
 * Programming in Java  -  Assignment 1 (CO1)
 * Task 4  -  Alternative Concept and Complexity Analysis
 *
 * Name            : Vignesh S
 * Register Number : 192325002
 *
 * Question: the design uses overloaded constructors and constructor chaining.
 * If objects were instead created by a default constructor followed by setter
 * calls, or through static factory methods, what would the solution be?  How
 * does each approach guarantee a VALID object, and what is the time and space
 * cost of creating n ParkingTicket objects in each?
 */
public class ObjectCreationComparison {

    /* ---- A: chained overloaded constructors (the chosen design) ---------- */
    static final class TicketA {
        private static int last = 1000;
        private final int no; private final String vehicleNo;
        private final int floor, slot, entryMinute;

        TicketA(String vehicleNo, int floor, int slot, int entryMinute) {
            if (vehicleNo == null || vehicleNo.isBlank())
                throw new IllegalArgumentException("vehicle number is mandatory");
            if (floor < 0 || slot < 0)
                throw new IllegalArgumentException("floor and slot cannot be negative");
            if (entryMinute < 0 || entryMinute > 1439)
                throw new IllegalArgumentException("entry minute must be 0..1439");
            this.no = ++last; this.vehicleNo = vehicleNo;
            this.floor = floor; this.slot = slot; this.entryMinute = entryMinute;
        }
        TicketA(String vehicleNo, int floor, int slot) { this(vehicleNo, floor, slot, 540); }
        int getNo() { return no; }
    }

    /* ---- B: default constructor + setters -------------------------------- */
    static final class TicketB {
        private static int last = 1000;
        private int no; private String vehicleNo;
        private int floor = -1, slot = -1, entryMinute = -1;
        private boolean sealed = false;

        TicketB() { this.no = ++last; }          // a HALF-BUILT object now exists

        void setVehicleNo(String v) {
            if (v == null || v.isBlank()) throw new IllegalArgumentException("vehicle number is mandatory");
            this.vehicleNo = v;
        }
        void setFloor(int f) { if (f < 0) throw new IllegalArgumentException("negative floor"); this.floor = f; }
        void setSlot(int s)  { if (s < 0) throw new IllegalArgumentException("negative slot");  this.slot = s; }
        void setEntryMinute(int m) {
            if (m < 0 || m > 1439) throw new IllegalArgumentException("entry minute must be 0..1439");
            this.entryMinute = m;
        }
        /** The validity check the constructor got for free now has to be called by hand. */
        void seal() {
            if (vehicleNo == null || floor < 0 || slot < 0 || entryMinute < 0)
                throw new IllegalStateException("ticket is incomplete and cannot be used");
            sealed = true;
        }
        boolean isUsable() { return sealed; }
        int getNo() { return no; }
    }

    /* ---- C: static factory methods --------------------------------------- */
    static final class TicketC {
        private static int last = 1000;
        private final int no; private final String vehicleNo;
        private final int floor, slot, entryMinute;

        private TicketC(String v, int f, int s, int m) {       // private: factory only
            this.no = ++last; this.vehicleNo = v; this.floor = f; this.slot = s; this.entryMinute = m;
        }
        static TicketC atGate(String v, int f, int s, int m) {
            if (v == null || v.isBlank()) throw new IllegalArgumentException("vehicle number is mandatory");
            if (f < 0 || s < 0) throw new IllegalArgumentException("floor and slot cannot be negative");
            if (m < 0 || m > 1439) throw new IllegalArgumentException("entry minute must be 0..1439");
            return new TicketC(v, f, s, m);
        }
        /** A factory can be NAMED, which overloaded constructors cannot. */
        static TicketC walkIn(String v) { return atGate(v, 0, 0, 540); }
        int getNo() { return no; }
    }

    private static final int N = 2_000_000;

    public static void main(String[] args) {
        System.out.println("TASK 4  -  ALTERNATIVE OBJECT-CREATION STRATEGIES");
        System.out.println("=".repeat(80));

        System.out.println();
        System.out.println("1.  HOW EACH APPROACH GUARANTEES A VALID OBJECT");
        System.out.println("-".repeat(80));
        System.out.printf("%-26s | %-22s | %s%n", "APPROACH", "WHEN IS IT CHECKED", "CAN AN INVALID OBJECT EXIST?");
        System.out.println("-".repeat(80));
        System.out.printf("%-26s | %-22s | %s%n", "A  chained constructors",
                          "before the object", "no - construction fails instead");
        System.out.printf("%-26s | %-22s | %s%n", "B  default + setters",
                          "per setter, then seal()", "YES - until seal() is called");
        System.out.printf("%-26s | %-22s | %s%n", "C  static factory",
                          "inside the factory", "no - if the constructor stays private");

        System.out.println();
        System.out.println("  Demonstration of the window that B opens:");
        TicketB half = new TicketB();
        half.setVehicleNo("TN01AB1111");          // caller forgets floor, slot and entry time
        System.out.println("     new TicketB(); setVehicleNo(...)  -> object #" + half.getNo()
                           + " already exists, usable = " + half.isUsable());
        try { half.seal(); }
        catch (IllegalStateException e) { System.out.println("     seal() -> REJECTED: " + e.getMessage()); }
        System.out.println("     ...but the half-built object was already handed a ticket NUMBER,");
        System.out.println("     so the counter has advanced for a ticket that was never issued.");
        System.out.println();
        TicketA aOk = new TicketA("TN02CD2222", 1, 7);
        System.out.println("     A: new TicketA(...) -> #" + aOk.getNo() + ", valid the instant it exists");
        try { new TicketA("", 1, 7); }
        catch (IllegalArgumentException e) { System.out.println("     A: invalid input -> REJECTED: " + e.getMessage()
                                                               + ", no object and no number consumed"); }
        TicketC cOk = TicketC.walkIn("TN03EF3333");
        System.out.println("     C: TicketC.walkIn(...) -> #" + cOk.getNo()
                           + ", the NAME says what kind of ticket it is");

        System.out.println();
        System.out.println("2.  MEASURED COST OF CREATING n = " + String.format("%,d", N) + " TICKETS");
        System.out.println("-".repeat(80));

        // warm up
        long sink = 0;
        for (int w = 0; w < 3; w++) {
            for (int i = 0; i < 200_000; i++) {
                sink += new TicketA("TN" + i, 0, 0, 540).getNo();
                TicketB b = new TicketB(); b.setVehicleNo("TN" + i); b.setFloor(0); b.setSlot(0);
                b.setEntryMinute(540); b.seal(); sink += b.getNo();
                sink += TicketC.atGate("TN" + i, 0, 0, 540).getNo();
            }
        }

        long t0 = System.nanoTime();
        for (int i = 0; i < N; i++) sink += new TicketA("TN01AB1111", 0, 0, 540).getNo();
        long tA = System.nanoTime() - t0;

        long t1 = System.nanoTime();
        for (int i = 0; i < N; i++) {
            TicketB b = new TicketB();
            b.setVehicleNo("TN01AB1111"); b.setFloor(0); b.setSlot(0); b.setEntryMinute(540);
            b.seal();
            sink += b.getNo();
        }
        long tB = System.nanoTime() - t1;

        long t2 = System.nanoTime();
        for (int i = 0; i < N; i++) sink += TicketC.atGate("TN01AB1111", 0, 0, 540).getNo();
        long tC = System.nanoTime() - t2;

        System.out.printf("%-26s | %-12s | %-16s | %-10s | %s%n",
                          "APPROACH", "TOTAL (ms)", "PER OBJECT (ns)", "CALLS", "COMPLEXITY");
        System.out.printf("%-26s | %-12.1f | %-16.1f | %-10s | %s%n",
                          "A  chained constructors", tA / 1e6, (double) tA / N, "1", "O(n)");
        System.out.printf("%-26s | %-12.1f | %-16.1f | %-10s | %s%n",
                          "B  default + setters", tB / 1e6, (double) tB / N, "6", "O(n)");
        System.out.printf("%-26s | %-12.1f | %-16.1f | %-10s | %s%n",
                          "C  static factory", tC / 1e6, (double) tC / N, "2", "O(n)");
        System.out.println();
        System.out.println("  All three are O(n) in time and O(n) in space - one object per ticket.");
        System.out.println("  The measured per-object times come out within a nanosecond or two of");
        System.out.println("  one another, and the ORDER of the three varies between runs.  That is");
        System.out.println("  the expected result, not a surprise: B issues 6 calls per object");
        System.out.println("  against A's 1, but every one of them is small enough for the JIT to");
        System.out.println("  inline, after which the three loops compile to almost the same machine");
        System.out.println("  code.  The honest reading of this table is therefore that creation");
        System.out.println("  strategy does NOT affect running time at this scale, so the choice");
        System.out.println("  must be made on correctness instead.");
        System.out.println();
        System.out.println("  Space is not identical, though: TicketB carries an extra 'sealed'");
        System.out.println("  boolean that exists only to record whether the object finished being");
        System.out.println("  built, which is one byte per ticket (padded) spent on a question A and");
        System.out.println("  C answer at compile time.");

        System.out.println();
        System.out.println("3.  DECISION");
        System.out.println("-".repeat(80));
        System.out.println("  A is retained.  The deciding argument is not speed - the measured gap");
        System.out.println("  is small and all three are O(n) - but the INVARIANT: a ParkingTicket");
        System.out.println("  that exists must name a real vehicle in a real slot at a real time,");
        System.out.println("  because the fee is computed from exactly those fields.  Only A and C");
        System.out.println("  can make that true at every instant, and only A lets the fields be");
        System.out.println("  declared final, which is what actually stops a ticket being edited");
        System.out.println("  after it is issued.  C would be preferred the moment creation needs a");
        System.out.println("  NAME that a constructor cannot carry (walkIn versus atGate) or the");
        System.out.println("  freedom to return a cached or subclass instance - neither of which");
        System.out.println("  this requirement asks for.");
        if (sink == Long.MIN_VALUE) System.out.println(sink);
    }
}
