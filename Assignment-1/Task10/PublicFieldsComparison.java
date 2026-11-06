/*
 * Programming in Java  -  Assignment 1 (CO1)
 * Task 10  -  Alternative Concept and Complexity Analysis
 *
 * Name            : Vignesh S
 * Register Number : 192325002
 *
 * Question: the fields are private with validating setters.  If the fields were
 * made PUBLIC and validation were done by the CALLING code instead, what would
 * the solution be and what could go wrong?  Analyse the time cost of validation
 * per update and the number of places where the same check would be repeated.
 *
 * This file is in the default package deliberately: it must be able to touch
 * design B's public fields directly, the way careless caller code would.
 */
public class PublicFieldsComparison {

    /* ========== DESIGN A: private fields, validation inside the class ===== */
    static class TicketA {
        private int entryMinute;
        private int exitMinute = -1;
        private String vehicleNo;

        TicketA(String vehicleNo, int entryMinute) {
            setVehicleNo(vehicleNo);
            setEntryMinute(entryMinute);
        }
        void setVehicleNo(String v) {
            if (v == null || v.isBlank()) throw new IllegalArgumentException("plate is mandatory");
            this.vehicleNo = v.trim().toUpperCase();
        }
        void setEntryMinute(int m) {
            if (m < 0 || m > 1439) throw new IllegalArgumentException("entry must be 0..1439");
            this.entryMinute = m;
        }
        void setExitMinute(int m) {
            if (m < 0 || m > 1439) throw new IllegalArgumentException("exit must be 0..1439");
            if (m < entryMinute)   throw new IllegalArgumentException("exit cannot precede entry");
            this.exitMinute = m;
        }
        int duration() { return exitMinute < 0 ? -1 : exitMinute - entryMinute; }
        String getVehicleNo() { return vehicleNo; }
        int getEntryMinute() { return entryMinute; }
    }

    /* ========== DESIGN B: public fields, validation left to the caller ==== */
    static class TicketB {
        public int entryMinute;
        public int exitMinute = -1;
        public String vehicleNo;
        int duration() { return exitMinute < 0 ? -1 : exitMinute - entryMinute; }
    }

    /** The validation the caller now has to write - in EVERY caller. */
    static void validateB(TicketB t) {
        if (t.vehicleNo == null || t.vehicleNo.isBlank())
            throw new IllegalArgumentException("plate is mandatory");
        if (t.entryMinute < 0 || t.entryMinute > 1439)
            throw new IllegalArgumentException("entry must be 0..1439");
        if (t.exitMinute >= 0) {
            if (t.exitMinute > 1439) throw new IllegalArgumentException("exit must be 0..1439");
            if (t.exitMinute < t.entryMinute)
                throw new IllegalArgumentException("exit cannot precede entry");
        }
    }

    private static final int N = 20_000_000;

    public static void main(String[] args) {
        System.out.println("TASK 10  -  ALTERNATIVE: PUBLIC FIELDS WITH CALLER-SIDE VALIDATION");
        System.out.println("=".repeat(80));

        System.out.println();
        System.out.println("1.  WHAT THE SOLUTION LOOKS LIKE");
        System.out.println("-".repeat(80));
        System.out.println("  A  private fields     caller writes:  t.setExitMinute(675);");
        System.out.println("     + validating setter                 -> checked, or it throws");
        System.out.println();
        System.out.println("  B  public fields      caller writes:  t.exitMinute = 675;");
        System.out.println("     + caller validation                 validateB(t);   // if remembered");

        System.out.println();
        System.out.println("2.  WHAT GOES WRONG: THE SAME FOUR MISTAKES IN BOTH DESIGNS");
        System.out.println("-".repeat(80));
        System.out.printf("%-34s | %-26s | %s%n", "MISTAKE", "DESIGN A", "DESIGN B");
        System.out.println("-".repeat(80));

        // 1. exit before entry
        TicketA a1 = new TicketA("TN01AB1111", 540);
        String aResult;
        try { a1.setExitMinute(400); aResult = "ACCEPTED, duration " + a1.duration(); }
        catch (IllegalArgumentException e) { aResult = "rejected: " + e.getMessage(); }
        TicketB b1 = new TicketB();
        b1.vehicleNo = "TN01AB1111"; b1.entryMinute = 540;
        b1.exitMinute = 400;                                  // nobody called validateB
        System.out.printf("%-34s | %-26s | %s%n", "exit (400) before entry (540)",
                          aResult, "ACCEPTED, duration " + b1.duration());

        // 2. impossible clock time
        TicketA a2 = new TicketA("TN02CD2222", 540);
        try { a2.setExitMinute(5000); aResult = "ACCEPTED"; }
        catch (IllegalArgumentException e) { aResult = "rejected: " + e.getMessage(); }
        TicketB b2 = new TicketB();
        b2.vehicleNo = "TN02CD2222"; b2.entryMinute = 540; b2.exitMinute = 5000;
        System.out.printf("%-34s | %-26s | %s%n", "exit minute 5000 (not a time)",
                          aResult, "ACCEPTED, duration " + b2.duration());

        // 3. blank plate
        try { new TicketA("  ", 540); aResult = "ACCEPTED"; }
        catch (IllegalArgumentException e) { aResult = "rejected: " + e.getMessage(); }
        TicketB b3 = new TicketB();
        b3.vehicleNo = "  "; b3.entryMinute = 540;
        System.out.printf("%-34s | %-26s | %s%n", "blank number plate",
                          aResult, "ACCEPTED, plate = \"" + b3.vehicleNo + "\"");

        // 4. untidy input normalisation
        TicketA a4 = new TicketA(" tn04gh4444 ", 540);
        TicketB b4 = new TicketB();
        b4.vehicleNo = " tn04gh4444 "; b4.entryMinute = 540;
        System.out.printf("%-34s | %-26s | %s%n", "\" tn04gh4444 \" normalised?",
                          "yes -> " + a4.getVehicleNo(), "no  -> \"" + b4.vehicleNo + "\"");

        System.out.println();
        System.out.println("  Design B accepted all four.  None of them threw, none logged anything,");
        System.out.println("  and the object is now in a state the business rules say is impossible.");
        System.out.println("  The negative duration is the dangerous one: a fee computed from it is");
        System.out.println("  Rs 0 or negative, so the facility pays the driver to park.");
        System.out.println();
        System.out.println("  And the untidy plate is worse than it looks: \" tn04gh4444 \" and");
        System.out.println("  \"TN04GH4444\" are different keys in any HashMap, so the vehicle that");
        System.out.println("  entered cannot be found at the exit barrier.");
        System.out.println();
        System.out.println("  Note that validateB() EXISTS in this file and is correct.  The failure");
        System.out.println("  is not that the check is missing - it is that nothing forces a caller");
        System.out.println("  to run it.  Design A has no such gap: the only route to the field is");
        System.out.println("  the setter, so the check cannot be skipped.");

        System.out.println();
        System.out.println("3.  WHERE THE SAME CHECK HAS TO BE REPEATED");
        System.out.println("-".repeat(80));
        System.out.printf("%-38s | %-16s | %s%n", "CALL SITE THAT WRITES A FIELD", "DESIGN A", "DESIGN B");
        System.out.println("-".repeat(80));
        String[][] sites = {
            {"Facility.admit()",                "0 checks", "4 checks"},
            {"Facility.release()",              "0 checks", "4 checks"},
            {"the Scanner gate console",        "0 checks", "4 checks"},
            {"the batch file importer",         "0 checks", "4 checks"},
            {"the duplicate-ticket re-issue",   "0 checks", "4 checks"},
            {"the test harness",                "0 checks", "4 checks"},
        };
        for (String[] r : sites) System.out.printf("%-38s | %-16s | %s%n", r[0], r[1], r[2]);
        System.out.println("-".repeat(80));
        System.out.printf("%-38s | %-16s | %s%n", "TOTAL for 6 call sites, 4 rules",
                          "4 (once, in the class)", "24 (4 x 6)");
        System.out.println();
        System.out.println("  The count is the argument.  With s call sites and r rules, design A");
        System.out.println("  writes the validation r times - O(r).  Design B writes it r x s");
        System.out.println("  times - O(r x s) - and every one of those copies is a place where a");
        System.out.println("  future rule change can be forgotten.  Adding a seventh call site to");
        System.out.println("  design A costs nothing; adding one to design B costs four more checks");
        System.out.println("  that a reviewer has to notice are missing.");

        System.out.println();
        System.out.println("4.  MEASURED TIME COST OF VALIDATION PER UPDATE");
        System.out.println("-".repeat(80));

        TicketA ta = new TicketA("TN01AB1111", 540);
        TicketB tb = new TicketB();
        tb.vehicleNo = "TN01AB1111"; tb.entryMinute = 540;

        long sink = 0;
        for (int w = 0; w < 3; w++)
            for (int i = 0; i < 2_000_000; i++) {
                ta.exitMinute = 600; ta.setExitMinute(600 + (i % 100)); sink += ta.duration();
                tb.exitMinute = 600 + (i % 100); sink += tb.duration();
            }

        long t0 = System.nanoTime();
        for (int i = 0; i < N; i++) { ta.setExitMinute(600 + (i % 100)); sink += ta.duration(); }
        long tA = System.nanoTime() - t0;

        long t1 = System.nanoTime();
        for (int i = 0; i < N; i++) { tb.exitMinute = 600 + (i % 100); sink += tb.duration(); }
        long tB = System.nanoTime() - t1;

        long t2 = System.nanoTime();
        for (int i = 0; i < N; i++) {
            tb.exitMinute = 600 + (i % 100); validateB(tb); sink += tb.duration();
        }
        long tBv = System.nanoTime() - t2;

        System.out.printf("%-38s | %-14s | %-18s | %s%n",
                          "UPDATE PATH", "TOTAL (ms)", "PER UPDATE (ns)", "CHECKS RUN");
        System.out.println("-".repeat(80));
        System.out.printf("%-38s | %-14.1f | %-18.3f | %s%n", "A  setter with validation",
                          tA / 1e6, (double) tA / N, "3");
        System.out.printf("%-38s | %-14.1f | %-18.3f | %s%n", "B  bare public field write",
                          tB / 1e6, (double) tB / N, "0 - unsafe");
        System.out.printf("%-38s | %-14.1f | %-18.3f | %s%n", "B  field write + validateB()",
                          tBv / 1e6, (double) tBv / N, "4");
        System.out.println();
        System.out.printf("  cost of safety in design A  : %.3f ns per update%n",
                          Math.max(0, (double) (tA - tB) / N));
        System.out.println("  Validation is three integer comparisons, and the measured difference");
        System.out.println("  above is a few nanoseconds per update - real, but small, and the same");
        System.out.println("  order as the validateB() row, which confirms that the cost is the");
        System.out.println("  CHECKS themselves and not the fact that they sit behind a method.");
        System.out.println();
        System.out.println("  Put that figure against the actual workload.  A facility of 320 slots");
        System.out.println("  turning over four times a day performs about 1,280 entries and 1,280");
        System.out.println("  exits, so roughly 5,000 field updates daily.  At the measured rate");
        System.out.printf("  that is %.1f microseconds of validation per day, or about %.0f%n",
                          5000 * Math.max(0, (double) (tA - tB) / N) / 1000.0,
                          365 * 5000 * Math.max(0, (double) (tA - tB) / N) / 1_000_000.0);
        System.out.println("  milliseconds per YEAR for the whole system.  There is no performance");
        System.out.println("  case for removing it.");

        System.out.println();
        System.out.println("5.  CONCLUSION");
        System.out.println("-".repeat(80));
        System.out.println("  Design A is retained.  The run-time difference is measurable but");
        System.out.println("  irrelevant at this workload; what the designs really differ in is");
        System.out.println("  where a mistake surfaces.  In A an invalid update throws at");
        System.out.println("  the point of the mistake, with a message naming the rule.  In B it");
        System.out.println("  succeeds, and the consequence appears later as a wrong fee, a vehicle");
        System.out.println("  that cannot be found at the barrier, or a negative duration in a");
        System.out.println("  monthly report - far from the line that caused it.");
        System.out.println();
        System.out.println("  Public fields also freeze the representation: once callers write");
        System.out.println("  t.exitMinute directly, exitMinute can never become an Integer, a");
        System.out.println("  LocalTime or a computed value without breaking every one of them.");
        System.out.println("  The private field plus setter is what makes the Task 12 redesign");
        System.out.println("  possible at all.");
        if (sink == Long.MIN_VALUE) System.out.println(sink);
    }
}
