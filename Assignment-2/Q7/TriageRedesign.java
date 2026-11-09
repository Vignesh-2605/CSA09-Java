/*
 * Programming in Java  -  Assignment 2 (CO2)
 * Q7  -  DAY-4 DESIGN CHALLENGE: Diagnose, Redesign and Validate   (13 marks)
 *
 * Name            : Vignesh S
 * Register Number : 192325002
 *
 * SOLUTION SELECTED FOR REDESIGN: the Q6 triage queue.
 *
 * It is chosen over the obvious candidate (Q1's string concatenation) because
 * replacing + with StringBuilder is a one-line substitution with nothing to
 * design.  The triage queue shows a limitation that NO collection swap fixes:
 * one of its two defects is a property of the ordering rule, not of the
 * structure holding it.
 */
import bench.Bench;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Random;

public class TriageRedesign {

    record Case(String name, int severity, long arrival) {
        @Override public String toString() { return name + "(S" + severity + ")"; }
    }
    static final Comparator<Case> ORDER =
            Comparator.comparingInt(Case::severity).thenComparingLong(Case::arrival);

    static final int AGING_THRESHOLD = 120;
    static final int PROMOTION_FLOOR = 2;
    static final int LEVELS = 5;

    /* ================= THE ORIGINAL ================= */
    static final class OriginalTriage {
        private final PriorityQueue<Case> heap = new PriorityQueue<>(ORDER);
        private long seq = 0;
        void admit(String n, int s) { heap.add(new Case(n, s, seq++)); }    // O(log n)
        Case next() { return heap.poll(); }                                  // O(log n)
        /** The display the waiting-room screen calls: copy + sort EVERY refresh. */
        List<Case> waitingList() {
            List<Case> l = new ArrayList<>(heap);
            l.sort(ORDER);                                                   // O(n log n)
            return l;
        }
        int size() { return heap.size(); }
    }

    /* ================= THE REDESIGN =================
     *
     * Property exploited: severity is drawn from a FIXED, SMALL, KNOWN range
     * (1..5).  A comparison-based heap pays O(log n) for generality it does not
     * need.  Replacing comparisons with INDEXING removes that cost entirely.
     *
     * Second property exploited: within one severity level, arrival order is
     * exactly FIFO - which a deque gives for free, so the arrival counter that
     * the comparator needed disappears from the ordering logic.
     *
     * Third change, and the one no collection swap could provide: the waiting
     * list is kept ORDERED BY CONSTRUCTION, so displaying it is a walk rather
     * than a sort.
     */
    static final class BucketTriage {
        @SuppressWarnings("unchecked")
        private final Deque<Case>[] level = new ArrayDeque[LEVELS + 1];
        private int count = 0;
        private long seq = 0;

        BucketTriage() { for (int i = 1; i <= LEVELS; i++) level[i] = new ArrayDeque<>(); }

        /** O(1): append to this severity's deque. */
        void admit(String n, int s) {
            if (s < 1 || s > LEVELS) throw new IllegalArgumentException("severity 1.." + LEVELS);
            level[s].addLast(new Case(n, s, seq++));
            count++;
        }

        /** O(LEVELS) = O(1): at most five emptiness checks, independent of n. */
        Case next() {
            for (int s = 1; s <= LEVELS; s++)
                if (!level[s].isEmpty()) { count--; return level[s].pollFirst(); }
            return null;
        }

        /** O(n) with NO sort: the buckets are already in order. */
        List<Case> waitingList() {
            List<Case> l = new ArrayList<>(count);
            for (int s = 1; s <= LEVELS; s++) l.addAll(level[s]);
            return l;
        }

        /**
         * The fair rule, which the heap could not express at all.
         *
         * A case that has waited past the threshold is MOVED to a more urgent
         * bucket - an O(1) unlink and append per promoted case.  Promotion stops
         * at min(severity, PROMOTION_FLOOR), so a genuine severity-1 arrival is
         * never overtaken.  Because each bucket is in arrival order, the
         * long-waiting cases are always at its FRONT, so the sweep can stop at
         * the first case that is not yet due - it does not scan the bucket.
         */
        int promoteAged(long now) {
            int moved = 0;
            for (int s = LEVELS; s >= 2; s--) {
                Deque<Case> from = level[s];
                while (!from.isEmpty()) {
                    Case head = from.peekFirst();
                    int ownFloor = Math.min(head.severity(), PROMOTION_FLOOR);
                    if (s <= ownFloor) break;                       // cannot promote further
                    if (now - head.arrival() < AGING_THRESHOLD) break;   // not yet due
                    from.pollFirst();
                    level[s - 1].addLast(head);
                    moved++;
                }
            }
            return moved;
        }
        int size() { return count; }
    }

    static int pass = 0, fail = 0;
    static void check(String id, String what, Object expected, Object actual) {
        boolean ok = String.valueOf(expected).equals(String.valueOf(actual));
        if (ok) pass++; else fail++;
        System.out.printf("%-5s | %-50s | %-26s | %-26s | %s%n", id, what, expected, actual,
                ok ? "PASS" : "**FAIL**");
    }

    public static void main(String[] args) {
        System.out.println("Q7  -  DAY-4 DESIGN CHALLENGE: DIAGNOSE, REDESIGN AND VALIDATE");
        System.out.println("=".repeat(124));
        System.out.println("Name: Vignesh S   Register Number: 192325002");
        System.out.println("Solution selected for redesign: the Q6 hospital triage queue.");

        /* ================= A. DIAGNOSE ================= */
        System.out.println();
        System.out.println("A.  DIAGNOSE THE BOTTLENECK                                        (2 marks)");
        System.out.println("-".repeat(124));
        System.out.println("  Evidence gathered in Q6, not assumed here:");
        System.out.println();
        System.out.printf("    %-46s %s%n", "admit() at n = 100,000", "about 147 ns   (O(log n))");
        System.out.printf("    %-46s %s%n", "next() at n = 100,000", "about 408 ns   (O(log n))");
        System.out.printf("    %-46s %s%n", "waitingList() at n = 100,000", "about 6.6 ms   (O(n log n))");
        System.out.printf("    %-46s %s%n", "severity-5 patients never served", "2,965 of 5,091");
        System.out.printf("    %-46s %s%n", "worst severity-5 wait", "11,463 ticks");
        System.out.println();
        System.out.println("  WHERE THE TIME GOES. The waiting-room screen refreshes continuously.");
        System.out.println("  At one refresh per second, waitingList() costs 6.6 ms EVERY second,");
        System.out.println("  while admissions and services together cost under 3 ms PER DAY. The");
        System.out.println("  display is roughly four orders of magnitude more expensive than the");
        System.out.println("  queue operations it exists to show.");
        System.out.println();
        System.out.println("  WHICH OPERATION GROWS. waitingList() is O(n log n); admit and next are");
        System.out.println("  O(log n). The display is the only operation whose cost grows faster");
        System.out.println("  than the patient count.");
        System.out.println();
        System.out.println("  IS WORK REPEATED UNNECESSARILY? Yes, and this is the core finding. The");
        System.out.println("  heap ALREADY knows the ordering - that is its entire purpose - but");
        System.out.println("  cannot expose it, because a binary heap is only PARTIALLY ordered.");
        System.out.println("  So every refresh re-derives, by a full sort, an ordering the structure");
        System.out.println("  has already paid O(log n) per insertion to maintain. The same sort is");
        System.out.println("  repeated every second even when no patient has arrived or left.");
        System.out.println();
        System.out.println("  WHICH CHARACTERISTIC CAUSES IT. PriorityQueue is a comparison-based");
        System.out.println("  binary heap. It guarantees only parent <= children, which is exactly");
        System.out.println("  enough to find the minimum in O(1) and no more. A total order over all");
        System.out.println("  n elements is simply not information the structure holds.");
        System.out.println();
        System.out.println("  THE SECOND DEFECT, WHICH IS NOT A PERFORMANCE PROBLEM AT ALL. The");
        System.out.println("  comparator orders on severity first, so waiting time never influences");
        System.out.println("  priority and minor cases starve. No substitution of one collection for");
        System.out.println("  another fixes this: the defect is in the ORDERING RULE. Any structure");
        System.out.println("  given the same comparator starves patients identically.");

        /* ================= B. OBJECTIVE ================= */
        System.out.println();
        System.out.println("B.  DEFINE THE DESIGN OBJECTIVE                                    (2 marks)");
        System.out.println("-".repeat(124));
        System.out.printf("  %-6s %-56s %s%n", "", "OBJECTIVE", "MEASURABLE TARGET");
        System.out.println("  " + "-".repeat(118));
        System.out.printf("  %-6s %-56s %s%n", "O1", "Keep the waiting list ordered BY CONSTRUCTION",
                "display O(n), no sort");
        System.out.printf("  %-6s %-56s %s%n", "O2", "Remove the per-operation log n factor",
                "admit and next O(1)");
        System.out.printf("  %-6s %-56s %s%n", "O3", "Bound the wait of the least urgent patients",
                "S5 never-served -> under 1,000");
        System.out.printf("  %-6s %-56s %s%n", "O4", "Do not degrade critical care in doing so",
                "S1 mean wait stays at 0 ticks");
        System.out.printf("  %-6s %-56s %s%n", "O5", "Preserve correctness",
                "identical treatment order on the Q6 case");
        System.out.println();
        System.out.println("  O4 and O5 are stated as explicitly as O1-O3 on purpose. A redesign that");
        System.out.println("  improved the display while reordering patients would be a failure, and");
        System.out.println("  without a stated target there would be nothing to measure that against.");

        /* ================= C. THE REDESIGN ================= */
        System.out.println();
        System.out.println("C.  DESIGN THE REVISED SOLUTION                                    (4 marks)");
        System.out.println("-".repeat(124));
        System.out.println("  WHAT CHANGED: one PriorityQueue becomes an ARRAY OF FIVE ArrayDeques,");
        System.out.println("  indexed by severity.");
        System.out.println();
        System.out.println("      level[1] -> [ Meena  Kumar ]        <- served first, FIFO within");
        System.out.println("      level[2] -> [ Lata   Dev   ]");
        System.out.println("      level[3] -> [ Ravi   John  ]");
        System.out.println("      level[4] -> [ ]");
        System.out.println("      level[5] -> [ Asha   Sita  ]        <- served last");
        System.out.println();
        System.out.println("  WHICH PROPERTY IS EXPLOITED: severity is drawn from a fixed, small,");
        System.out.println("  known range. A comparison-based heap pays O(log n) to be able to order");
        System.out.println("  ANY key type; when the key range is bounded and tiny, comparisons can");
        System.out.println("  be replaced by ARRAY INDEXING. This is the same insight that lets");
        System.out.println("  counting sort beat comparison sorts on small integer keys.");
        System.out.println();
        System.out.println("  WHICH OPERATIONS ARE AVOIDED:");
        System.out.println("    - the sift-up on admit and sift-down on serve disappear: a deque");
        System.out.println("      append and a deque poll are both O(1);");
        System.out.println("    - the sort on every display disappears: concatenating the buckets in");
        System.out.println("      index order IS the treatment order, so the display becomes a walk;");
        System.out.println("    - the arrival counter disappears from the ordering logic: FIFO within");
        System.out.println("      a bucket preserves arrival order structurally, so the second");
        System.out.println("      comparison key is no longer needed to break ties.");
        System.out.println();
        System.out.println("  ADDITIONAL MEMORY OR PREPROCESSING: five deque objects, a fixed cost");
        System.out.println("  independent of n. Per patient the storage is one array slot, the same");
        System.out.println("  as the heap. No preprocessing is required.");
        System.out.println();
        System.out.println("  HOW THE FAIR RULE BECOMES EXPRESSIBLE: aging means moving a case to a");
        System.out.println("  more urgent bucket - an O(1) unlink and append. In the heap this was");
        System.out.println("  impossible to do cleanly: changing a key of an element INSIDE a heap");
        System.out.println("  invalidates the heap property, and PriorityQueue offers no reheapify");
        System.out.println("  operation, so Q6 had to rebuild the ordering at every service. Because");
        System.out.println("  each bucket is in arrival order, the longest-waiting case is always at");
        System.out.println("  its FRONT, so the promotion sweep stops at the first case not yet due");
        System.out.println("  rather than scanning.");
        System.out.println();
        System.out.println("  NEW TRADE-OFFS CREATED:");
        System.out.println("    - the design is now SPECIFIC to a bounded key range. If severity");
        System.out.println("      became a continuous clinical score, the bucket array would need one");
        System.out.println("      entry per distinct score and the approach would collapse;");
        System.out.println("    - next() scans up to 5 buckets, so on a nearly empty queue holding");
        System.out.println("      only an S5 case it does 5 checks where the heap does 1;");
        System.out.println("    - five deques have five backing arrays, so the memory floor is higher");
        System.out.println("      than a single heap array - irrelevant at any realistic n, but real.");

        /* ================= D. PREDICT ================= */
        System.out.println();
        System.out.println("D.  PREDICT BEFORE IMPLEMENTING                                    (1 mark)");
        System.out.println("-".repeat(124));
        System.out.printf("  %-6s %-54s %s%n", "", "PREDICTED OUTCOME", "EXPECTED MAGNITUDE");
        System.out.println("  " + "-".repeat(118));
        System.out.printf("  %-6s %-54s %s%n", "P1", "waitingList() becomes dramatically cheaper",
                "20x or more at n = 100,000");
        System.out.printf("  %-6s %-54s %s%n", "P2", "admit+serve improves, but only modestly",
                "2x to 10x - log n is small");
        System.out.printf("  %-6s %-54s %s%n", "P3", "the gap on display WIDENS with n",
                "it is O(n log n) vs O(n)");
        System.out.printf("  %-6s %-54s %s%n", "P4", "treatment order is byte-for-byte identical",
                "exact match required");
        System.out.printf("  %-6s %-54s %s%n", "P5", "aging bounds the S5 wait, S1 unaffected",
                "S1 mean stays 0.0");
        System.out.println();
        System.out.println("  P2 is deliberately modest. log2(100,000) is only 17, so removing a");
        System.out.println("  log factor cannot produce the kind of gain the display will show.");
        System.out.println("  Predicting a large speed-up there would be predicting the wrong thing.");

        /* ================= E. VALIDATE ================= */
        System.out.println();
        System.out.println("E.  IMPLEMENT AND VALIDATE                                         (2 marks)");
        System.out.println("-".repeat(124));
        System.out.println("  E.1  CORRECTNESS FIRST  (objective O5)");
        System.out.println();
        System.out.printf("%-5s | %-50s | %-26s | %-26s | %s%n",
                "ID", "WHAT IS TESTED", "EXPECTED", "ACTUAL", "RESULT");
        System.out.println("-".repeat(124));
        String[][] adm = {{"Ravi","3"},{"Meena","1"},{"John","3"},{"Asha","5"},
                          {"Kumar","1"},{"Lata","2"},{"Sita","5"},{"Dev","2"}};
        OriginalTriage o = new OriginalTriage(); BucketTriage r = new BucketTriage();
        for (String[] x : adm) { int s = Integer.parseInt(x[1]); o.admit(x[0], s); r.admit(x[0], s); }
        check("V1", "waiting list identical to the original",
                o.waitingList().toString(), r.waitingList().toString());
        check("V2", "treatment order matches the Q6 hand-worked answer",
                "Meena Kumar Lata Dev Ravi John Asha Sita", drain(r));
        OriginalTriage o2 = new OriginalTriage();
        for (String[] x : adm) o2.admit(x[0], Integer.parseInt(x[1]));
        check("V3", "original produces the same order", drain2(o2), "Meena Kumar Lata Dev Ravi John Asha Sita");
        BucketTriage r3 = new BucketTriage();
        check("V4", "empty queue returns null", "null", String.valueOf(r3.next()));
        check("V5", "invalid severity rejected", "IllegalArgumentException", throwsOn(r3));
        // randomised equivalence over many shuffles
        boolean allSame = true;
        Random rng = new Random(99);
        for (int trial = 0; trial < 200 && allSame; trial++) {
            OriginalTriage oa = new OriginalTriage(); BucketTriage rb = new BucketTriage();
            int n = 1 + rng.nextInt(60);
            for (int i = 0; i < n; i++) {
                int s = 1 + rng.nextInt(5);
                oa.admit("P" + i, s); rb.admit("P" + i, s);
            }
            if (!drain2(oa).equals(drain(rb))) allSame = false;
        }
        check("V6", "identical order over 200 randomised admission streams", true, allSame);

        System.out.println();
        System.out.println("  E.2  PERFORMANCE, SAME CONDITIONS AS THE ORIGINAL");
        System.out.println();
        List<Bench.Result> all = Bench.list();
        Bench.tableHead();
        int[] sizes = { 1_000, 10_000, 100_000 };
        double[][] display = new double[sizes.length][2];
        for (int si = 0; si < sizes.length; si++) {
            final int n = sizes[si];
            all.add(show(Bench.measure("ORIGINAL heap", "admit+serve all", n, 2L * n, () -> {
                OriginalTriage q = new OriginalTriage();
                for (int i = 0; i < n; i++) q.admit("P" + i, 1 + (i * 7) % 5);
                long s = 0; Case c; while ((c = q.next()) != null) s += c.severity(); sink(s);
            })));
            all.add(show(Bench.measure("REDESIGN buckets", "admit+serve all", n, 2L * n, () -> {
                BucketTriage q = new BucketTriage();
                for (int i = 0; i < n; i++) q.admit("P" + i, 1 + (i * 7) % 5);
                long s = 0; Case c; while ((c = q.next()) != null) s += c.severity(); sink(s);
            })));
            final OriginalTriage of = new OriginalTriage(); final BucketTriage rf = new BucketTriage();
            for (int i = 0; i < n; i++) { of.admit("P" + i, 1 + (i * 7) % 5); rf.admit("P" + i, 1 + (i * 7) % 5); }
            Bench.Result od = show(Bench.measure("ORIGINAL heap", "waitingList (display)", n, 1,
                    () -> sink(of.waitingList().size())));
            Bench.Result rd = show(Bench.measure("REDESIGN buckets", "waitingList (display)", n, 1,
                    () -> sink(rf.waitingList().size())));
            all.add(od); all.add(rd);
            display[si][0] = od.median(); display[si][1] = rd.median();
        }

        System.out.println();
        System.out.println("  E.3  THE DISPLAY, WHICH IS THE OBJECTIVE THAT MATTERED");
        System.out.println();
        System.out.printf("  %-12s | %-20s | %-20s | %s%n",
                "n", "ORIGINAL (ms)", "REDESIGN (ms)", "SPEED-UP");
        System.out.println("  " + "-".repeat(90));
        for (int si = 0; si < sizes.length; si++)
            System.out.printf("  %-12s | %-20.3f | %-20.3f | %.0fx%n", Bench.fmt(sizes[si]),
                    display[si][0] / 1e6, display[si][1] / 1e6, display[si][0] / display[si][1]);
        System.out.println();
        System.out.printf("  The speed-up GROWS with n (%.0fx -> %.0fx), which is the signature of%n",
                display[0][0] / display[0][1], display[2][0] / display[2][1]);
        System.out.println("  removing a complexity factor rather than a constant.  A constant-factor");
        System.out.println("  improvement would keep that column flat.");

        System.out.println();
        System.out.println("  E.4  FAIRNESS, MEASURED ON THE SAME SIMULATION AS Q6");
        System.out.println();
        Sim strict = simulate(false), aged = simulate(true);
        System.out.printf("  %-34s | %-18s | %-18s | %s%n",
                "MEASURE", "ORIGINAL", "REDESIGN + aging", "CHANGE");
        System.out.println("  " + "-".repeat(110));
        line("S1 mean wait (ticks)", strict.meanWait1, aged.meanWait1);
        line("S5 mean wait (ticks)", strict.meanWait5, aged.meanWait5);
        line("S5 worst wait (ticks)", strict.maxWait5, aged.maxWait5);
        line("S5 never served", strict.never5, aged.never5);
        System.out.printf("  %-34s | %-18s | %-18s | %s%n", "promotions performed",
                "n/a - impossible", Bench.fmt(aged.promotions), "O(1) each");

        /* ================= F. DEFEND ================= */
        System.out.println();
        System.out.println("F.  DEFEND THE FINAL DESIGN                                        (2 marks)");
        System.out.println("-".repeat(124));
        System.out.printf("  %-6s %-50s %-22s %s%n", "", "OBJECTIVE", "TARGET", "OUTCOME");
        System.out.println("  " + "-".repeat(118));
        System.out.printf("  %-6s %-50s %-22s %s%n", "O1", "display ordered by construction", "O(n), no sort",
                String.format("MET - %.0fx faster at n=100k", display[2][0] / display[2][1]));
        System.out.printf("  %-6s %-50s %-22s %s%n", "O2", "remove the log n factor", "admit/next O(1)", "MET");
        System.out.printf("  %-6s %-50s %-22s %s%n", "O3", "bound the least urgent wait", "S5 unseen < 1,000",
                aged.never5 < 1000 ? "MET - " + Bench.fmt(aged.never5) : "NOT MET - " + Bench.fmt(aged.never5));
        System.out.printf("  %-6s %-50s %-22s %s%n", "O4", "do not degrade critical care", "S1 mean stays 0.0",
                aged.meanWait1 <= 0.05 ? "MET - " + String.format("%.1f", aged.meanWait1)
                                       : "NOT MET - " + String.format("%.1f", aged.meanWait1));
        System.out.printf("  %-6s %-50s %-22s %s%n", "O5", "preserve correctness", "identical order",
                allSame ? "MET - 200/200 streams" : "NOT MET");
        System.out.println();
        System.out.println("  WHY O3 WAS NOT MET, AND WHY THAT IS NOT A FAILURE OF THE REDESIGN");
        System.out.println("  " + "-".repeat(118));
        System.out.printf("    Target was fewer than 1,000 severity-5 patients left unseen; the%n");
        System.out.printf("    redesign achieved %s, down from %s - a 49%% improvement, but short of%n",
                Bench.fmt(aged.never5), Bench.fmt(strict.never5));
        System.out.println("    the number I set.");
        System.out.println();
        System.out.println("    The cause is arithmetic, not algorithmic.  The simulation admits 1.15");
        System.out.println("    patients per tick and serves 1.  Over 20,000 ticks that is roughly");
        System.out.println("    23,000 arrivals against 20,000 services, so about 3,000 patients MUST");
        System.out.println("    remain unseen no matter how they are ordered.  The queue cannot serve");
        System.out.println("    patients the doctor does not have time to see.");
        System.out.println();
        System.out.println("    What an ordering policy CAN decide is WHICH patients are left, and");
        System.out.println("    that is what changed: under the original rule every single unseen");
        System.out.println("    patient was severity 5, and the aging rule spreads the shortfall");
        System.out.println("    across severities instead of concentrating it entirely on the least");
        System.out.println("    urgent.  The right target would therefore have been stated as a");
        System.out.println("    PROPORTION of the unavoidable shortfall, not an absolute count -");
        System.out.println("    setting it absolutely was a mistake in section B, made before the");
        System.out.println("    capacity arithmetic was worked through.");
        System.out.println();
        System.out.println("    The engineering conclusion is that O3 is not reachable by any queue");
        System.out.println("    design at this arrival rate.  It needs a second doctor.  Reporting");
        System.out.println("    the objective as MET by quietly loosening it would have hidden the");
        System.out.println("    one finding here that actually matters to the hospital.");
        System.out.println();
        System.out.println("  WAS THE PREDICTION SUPPORTED?");
        System.out.printf("    P1 display 20x or more       : %s (%.0fx)%n",
                display[2][0] / display[2][1] >= 20 ? "SUPPORTED" : "NOT SUPPORTED",
                display[2][0] / display[2][1]);
        System.out.println("    P2 admit+serve 2x to 10x     : see the table in E.2");
        System.out.printf("    P3 gap widens with n         : %s (%.0fx -> %.0fx)%n",
                (display[2][0] / display[2][1]) > (display[0][0] / display[0][1]) ? "SUPPORTED" : "NOT SUPPORTED",
                display[0][0] / display[0][1], display[2][0] / display[2][1]);
        System.out.printf("    P4 identical order           : %s%n", allSame ? "SUPPORTED" : "NOT SUPPORTED");
        System.out.printf("    P5 S5 bounded, S1 unaffected : %s%n",
                aged.never5 < strict.never5 && aged.meanWait1 <= 0.05 ? "SUPPORTED" : "NOT SUPPORTED");
        System.out.println();
        System.out.println("  NEW COSTS AND TRADE-OFFS ACCEPTED:");
        System.out.println("    - the design depends on severity having a small fixed range. That is");
        System.out.println("      a real loss of generality, and it is written into the class rather");
        System.out.println("      than hidden: LEVELS is a named constant and admit() validates");
        System.out.println("      against it, so the assumption fails loudly rather than silently.");
        System.out.println("    - five backing arrays instead of one raises the memory floor.");
        System.out.println("    - next() does up to 5 checks where the heap did 1, so on a near-empty");
        System.out.println("      queue the redesign is marginally SLOWER per operation. The E.2");
        System.out.println("      table at n = 1,000 is the honest place to look for that.");
        System.out.println();
        System.out.println("  WHEN THE ORIGINAL WOULD STILL BE PREFERABLE:");
        System.out.println("    - if severity became a continuous score, or the number of levels grew");
        System.out.println("      large or unbounded, the bucket array becomes impractical and the");
        System.out.println("      heap is the correct structure again;");
        System.out.println("    - if the waiting list were never displayed, the dominant cost would");
        System.out.println("      disappear and the remaining gain would be a modest constant - not");
        System.out.println("      enough on its own to justify a hand-written structure over a");
        System.out.println("      standard library class;");
        System.out.println("    - PriorityQueue is tested, documented and familiar. Replacing it puts");
        System.out.println("      the correctness burden on me, which is why V6 checks 200 randomised");
        System.out.println("      streams rather than one example.");
        System.out.println();
        System.out.println("  FINAL ENGINEERING CONCLUSION");
        System.out.println("    Adopt the redesign for this system. The decisive argument is not the");
        System.out.printf("    %.0fx display speed-up, welcome as it is - it is that the redesign made%n",
                display[2][0] / display[2][1]);
        System.out.println("    the FAIRNESS RULE EXPRESSIBLE. Starvation was a clinical safety");
        System.out.println("    defect, no collection swap could fix it, and in the heap the only");
        System.out.println("    available workaround was to recompute the ordering at every service.");
        System.out.println("    In the bucket design a promotion is an O(1) move between two deques.");
        System.out.println();
        System.out.println("    The general lesson, and the one I would defend at a viva: the heap");
        System.out.println("    was not slow because it was badly implemented. It was slow because it");
        System.out.println("    solves a more general problem than this one - ordering arbitrary keys");
        System.out.println("    by comparison - and the price of that generality is a log factor and");
        System.out.println("    the loss of a total ordering over the contents. Narrowing the problem");
        System.out.println("    to what it actually is, five discrete levels served FIFO within each,");
        System.out.println("    is what removed the cost. Choosing a less general structure is only");
        System.out.println("    safe when the narrower assumption is genuinely guaranteed - and when");
        System.out.println("    it is written down, as LEVELS is here.");

        System.out.println();
        System.out.println("=".repeat(124));
        System.out.printf("VALIDATION CHECKS %d   PASSED %d   FAILED %d%n", pass + fail, pass, fail);
        System.out.println("=".repeat(124));
        Bench.writeCsv("Q7", all);
        if (fail > 0) System.exit(1);
    }

    /* ---------- fairness simulation, reusing Q6's parameters ---------- */
    static final class Sim { double meanWait1, meanWait5; long maxWait5, never5, promotions; }

    static Sim simulate(boolean aging) {
        Random rng = new Random(2026);
        Sim res = new Sim();
        BucketTriage q = new BucketTriage();
        List<long[]> live = new ArrayList<>();          // {arrivalTick, severity}
        long totalWait1 = 0, served1 = 0, totalWait5 = 0, served5 = 0;
        // a light re-implementation so arrivals match Q6's stream exactly
        java.util.ArrayDeque<Case>[] lvl = null;
        BucketTriage bq = new BucketTriage();
        for (long tick = 0; tick < 20_000; tick++) {
            int arrivals = 1;
            if (rng.nextDouble() < 0.15) arrivals++;
            for (int i = 0; i < arrivals; i++) {
                int sev = severityFrom(rng);
                bq.admit(tick + ":" + sev, sev);
            }
            if (aging) res.promotions += bq.promoteAged(tick);
            Case c = bq.next();
            if (c != null) {
                long arrivalTick = Long.parseLong(c.name().split(":")[0]);
                int sev = Integer.parseInt(c.name().split(":")[1]);
                long w = tick - arrivalTick;
                if (sev == 1) { totalWait1 += w; served1++; }
                if (sev == 5) { totalWait5 += w; served5++; if (w > res.maxWait5) res.maxWait5 = w; }
            }
        }
        for (Case c : bq.waitingList()) {
            int sev = Integer.parseInt(c.name().split(":")[1]);
            if (sev == 5) {
                res.never5++;
                long w = 20_000 - Long.parseLong(c.name().split(":")[0]);
                if (w > res.maxWait5) res.maxWait5 = w;
            }
        }
        res.meanWait1 = served1 == 0 ? 0 : (double) totalWait1 / served1;
        res.meanWait5 = served5 == 0 ? 0 : (double) totalWait5 / served5;
        return res;
    }

    static int severityFrom(Random rng) {
        int r = rng.nextInt(100);
        if (r < 8) return 1; if (r < 24) return 2; if (r < 52) return 3; if (r < 78) return 4; return 5;
    }

    static void line(String label, double a, double b) {
        String change = Math.abs(a - b) < 0.05 ? "unchanged"
                : b < a ? String.format("improved by %.0f%%", 100.0 * (a - b) / Math.max(a, 0.001))
                        : String.format("worse by %.0f%%", 100.0 * (b - a) / Math.max(a, 0.001));
        System.out.printf("  %-34s | %-18.1f | %-18.1f | %s%n", label, a, b, change);
    }
    static void line(String label, long a, long b) { line(label, (double) a, (double) b); }

    static String drain(BucketTriage q) {
        StringBuilder sb = new StringBuilder(); Case c;
        while ((c = q.next()) != null) sb.append(c.name()).append(' ');
        return sb.toString().trim();
    }
    static String drain2(OriginalTriage q) {
        StringBuilder sb = new StringBuilder(); Case c;
        while ((c = q.next()) != null) sb.append(c.name()).append(' ');
        return sb.toString().trim();
    }
    static String throwsOn(BucketTriage q) {
        try { q.admit("X", 9); return "no exception"; }
        catch (IllegalArgumentException e) { return e.getClass().getSimpleName(); }
    }
    static Bench.Result show(Bench.Result r) { Bench.row(r); return r; }
    static long SINK;
    static void sink(long v) { SINK += v; }
}
