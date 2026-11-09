/*
 * Programming in Java  -  Assignment 2 (CO2)
 * Q6  -  Priority, Order and Fairness in a Queue
 *
 * Name            : Vignesh S
 * Register Number : 192325002
 *
 * Designed from the problem statement, not from supplied code.
 */
import bench.Bench;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Queue;

public class TriageDesign {

    /* ---- the entity ---- */
    record Case(String name, int severity, long arrival) {
        @Override public String toString() { return name + "(S" + severity + ")"; }
    }

    /** severity ascending (1 = critical), then arrival ascending. */
    static final Comparator<Case> TRIAGE_ORDER =
            Comparator.comparingInt(Case::severity).thenComparingLong(Case::arrival);

    /* ---- the design ---- */
    static final class TriageQueue {
        private final PriorityQueue<Case> heap = new PriorityQueue<>(TRIAGE_ORDER);
        private long seq = 0;
        void admit(String name, int severity) {
            if (severity < 1 || severity > 5)
                throw new IllegalArgumentException("severity must be 1..5, was " + severity);
            heap.add(new Case(name, severity, seq++));
        }
        Case next() { return heap.poll(); }
        Case peek() { return heap.peek(); }
        int size() { return heap.size(); }
        /** The waiting list IN TREATMENT ORDER - which iteration does not give. */
        List<Case> waitingList() {
            List<Case> l = new ArrayList<>(heap);
            l.sort(TRIAGE_ORDER);
            return l;
        }
        List<Case> rawIterationOrder() { return new ArrayList<>(heap); }
    }

    static int pass = 0, fail = 0;
    static void check(String id, String what, Object expected, Object actual) {
        boolean ok = String.valueOf(expected).equals(String.valueOf(actual));
        if (ok) pass++; else fail++;
        System.out.printf("%-5s | %-44s | %-34s | %-34s | %s%n", id, what, expected, actual,
                ok ? "PASS" : "**FAIL**");
    }
    static void head() {
        System.out.printf("%-5s | %-44s | %-34s | %-34s | %s%n",
                "ID", "WHAT IS TESTED", "EXPECTED (worked out by hand)", "ACTUAL", "RESULT");
        System.out.println("-".repeat(132));
    }

    public static void main(String[] args) {
        System.out.println("Q6  -  HOSPITAL EMERGENCY TRIAGE");
        System.out.println("=".repeat(132));
        System.out.println("Name: Vignesh S   Register Number: 192325002");

        /* ================= the design argument ================= */
        System.out.println();
        System.out.println("1.  WHY A PLAIN Queue IS NOT SUFFICIENT");
        System.out.println("-".repeat(132));
        System.out.println("  A Queue - LinkedList or ArrayDeque - is strictly FIFO: it serves in the");
        System.out.println("  order patients arrived.  The rule here is the opposite: the most severe");
        System.out.println("  patient must be seen next, whenever they arrived.  Demonstrated:");
        System.out.println();
        Queue<Case> fifo = new ArrayDeque<>();
        fifo.add(new Case("Ravi", 3, 0)); fifo.add(new Case("Meena", 1, 1));
        fifo.add(new Case("Asha", 5, 2)); fifo.add(new Case("Kumar", 1, 3));
        StringBuilder fo = new StringBuilder();
        Case x; while ((x = fifo.poll()) != null) fo.append(x).append(' ');
        System.out.println("    ArrayDeque order : " + fo.toString().trim());
        System.out.println("    required order   : Meena(S1) Kumar(S1) Ravi(S3) Asha(S5)");
        System.out.println("    A critical patient is served FOURTH behind two less severe ones.");
        System.out.println();
        System.out.println("  Sorting the queue on every admission would fix the order but costs");
        System.out.println("  O(n log n) per arrival.  A PriorityQueue maintains a binary heap so");
        System.out.println("  that the minimum is always at the root: O(log n) to admit, O(log n) to");
        System.out.println("  serve, and O(1) to see who is next.");

        System.out.println();
        System.out.println("2.  HOW ARRIVAL ORDER IS PRESERVED AMONG EQUAL SEVERITIES");
        System.out.println("-".repeat(132));
        System.out.println("  A PriorityQueue is NOT STABLE.  Two patients of equal severity emerge");
        System.out.println("  in whatever order the heap's sift operations happened to leave them,");
        System.out.println("  which depends on the insertion sequence and is not predictable.");
        System.out.println();
        System.out.println("  The fix is to make the ordering TOTAL rather than partial: a strictly");
        System.out.println("  increasing arrival counter is appended as a second key, so no two");
        System.out.println("  cases ever compare equal and the tie cannot be broken arbitrarily.");
        System.out.println("  A wall-clock timestamp would NOT be safe here - two admissions in the");
        System.out.println("  same millisecond would tie again - so the counter is used instead.");
        System.out.println();
        System.out.println("  Demonstration with the counter REMOVED (severity only):");
        PriorityQueue<Case> unstable = new PriorityQueue<>(Comparator.comparingInt(Case::severity));
        String[] names = { "A", "B", "C", "D", "E", "F", "G", "H" };
        for (int i = 0; i < names.length; i++) unstable.add(new Case(names[i], 3, i));
        StringBuilder us = new StringBuilder();
        Case u; while ((u = unstable.poll()) != null) us.append(u.name());
        System.out.println("    admitted A B C D E F G H, all severity 3");
        System.out.println("    served   " + us + "   <- arrival order " +
                (us.toString().equals("ABCDEFGH") ? "happened to survive" : "NOT preserved"));
        System.out.println("    (the heap is free to return any of them first; relying on it is a bug");
        System.out.println("     even on a run where it looks correct)");

        /* ================= hand-verified correctness ================= */
        System.out.println();
        System.out.println("3.  CORRECTNESS CHECK ON 8 PATIENTS WORKED OUT BY HAND");
        System.out.println("-".repeat(132));
        System.out.println("  Admissions, in order:");
        System.out.println("    #0 Ravi S3   #1 Meena S1   #2 John S3   #3 Asha S5");
        System.out.println("    #4 Kumar S1  #5 Lata S2    #6 Sita S5   #7 Dev S2");
        System.out.println();
        System.out.println("  By hand: group by severity, then by arrival within each group.");
        System.out.println("    S1 : Meena(#1), Kumar(#4)");
        System.out.println("    S2 : Lata(#5),  Dev(#7)");
        System.out.println("    S3 : Ravi(#0),  John(#2)");
        System.out.println("    S5 : Asha(#3),  Sita(#6)");
        System.out.println("  => Meena Kumar Lata Dev Ravi John Asha Sita");
        System.out.println();
        TriageQueue er = new TriageQueue();
        er.admit("Ravi", 3); er.admit("Meena", 1); er.admit("John", 3); er.admit("Asha", 5);
        er.admit("Kumar", 1); er.admit("Lata", 2); er.admit("Sita", 5); er.admit("Dev", 2);

        head();
        check("C1", "waiting list, in treatment order",
                "[Meena(S1), Kumar(S1), Lata(S2), Dev(S2), Ravi(S3), John(S3), Asha(S5), Sita(S5)]",
                er.waitingList().toString());
        check("C2", "peek() shows the next patient without serving",
                "Meena(S1)", er.peek().toString());
        check("C3", "size unchanged after peek", 8, er.size());

        StringBuilder served = new StringBuilder();
        TriageQueue er2 = new TriageQueue();
        er2.admit("Ravi", 3); er2.admit("Meena", 1); er2.admit("John", 3); er2.admit("Asha", 5);
        er2.admit("Kumar", 1); er2.admit("Lata", 2); er2.admit("Sita", 5); er2.admit("Dev", 2);
        Case p; while ((p = er2.next()) != null) served.append(p.name()).append(' ');
        check("C4", "actual treatment order", "Meena Kumar Lata Dev Ravi John Asha Sita",
                served.toString().trim());
        check("C5", "queue empty after all served", 0, er2.size());
        check("C6", "next() on an empty queue returns null", "null", String.valueOf(er2.next()));
        check("C7", "severity 0 is rejected", "IllegalArgumentException", throwsOn(er2, 0));
        check("C8", "severity 6 is rejected", "IllegalArgumentException", throwsOn(er2, 6));

        System.out.println();
        System.out.println("  WHY PRINTING THE PriorityQueue DIRECTLY DOES NOT SHOW TREATMENT ORDER:");
        System.out.println("    toString() / iterator()  : " + er.rawIterationOrder());
        System.out.println("    waitingList()            : " + er.waitingList());
        System.out.println();
        System.out.println("  A PriorityQueue is a BINARY HEAP stored in an array.  The heap property");
        System.out.println("  guarantees only that each node is <= its two children - it says nothing");
        System.out.println("  about siblings or about the array order beyond that.  So the array is");
        System.out.println("  PARTIALLY ordered: index 0 is genuinely the next patient, and the rest");
        System.out.println("  is not a ranking at all.  Iteration walks that array, so it shows the");
        System.out.println("  heap's internal layout rather than the order of treatment.  To display");
        System.out.println("  a waiting list the contents must be copied and sorted - which is O(n");
        System.out.println("  log n) and is the cost examined in section 4.");

        /* ================= growth ================= */
        System.out.println();
        System.out.println("4.  GROWTH: PREDICTED, THEN MEASURED");
        System.out.println("-".repeat(132));
        System.out.println("  PREDICTION, before measuring:");
        System.out.println("    admit()        O(log n) - sift the new case up at most log2(n) levels");
        System.out.println("    next()         O(log n) - move the last element to the root, sift down");
        System.out.println("    peek()         O(1)     - read index 0");
        System.out.println("    waitingList()  O(n log n) - copy and sort");
        System.out.println();
        System.out.println("    Because log2(n) grows so slowly, going from 1,000 to 100,000 patients");
        System.out.println("    (100x) should raise the per-operation cost by only about 10/17 = 1.7x,");
        System.out.println("    NOT by 100x.  That is the prediction worth testing.");
        System.out.println();
        int[] sizes = { 1_000, 10_000, 100_000, 1_000_000 };
        List<Bench.Result> all = Bench.list();
        Bench.tableHead();
        for (int n : sizes) {
            final int size = n;
            all.add(show(Bench.measure("PriorityQueue", "admit", n, n, () -> {
                TriageQueue q = new TriageQueue();
                for (int i = 0; i < size; i++) q.admit("P" + i, 1 + (i * 7) % 5);
                sink(q.size());
            })));
            final TriageQueue filled = new TriageQueue();
            for (int i = 0; i < size; i++) filled.admit("P" + i, 1 + (i * 7) % 5);
            all.add(show(Bench.measure("PriorityQueue", "peek", n, 1000, () -> {
                long s = 0; for (int i = 0; i < 1000; i++) s += filled.peek().severity(); sink(s);
            })));
            all.add(show(Bench.measure("PriorityQueue", "serve all (next)", n, n, () -> {
                TriageQueue q = new TriageQueue();
                for (int i = 0; i < size; i++) q.admit("P" + i, 1 + (i * 7) % 5);
                long s = 0; Case c; while ((c = q.next()) != null) s += c.severity(); sink(s);
            })));
            if (n <= 100_000)
                all.add(show(Bench.measure("PriorityQueue", "waitingList (sorted)", n, 1,
                        () -> sink(filled.waitingList().size()))));
        }
        System.out.println();
        System.out.println("  The admit and serve columns should rise only slightly across a 1000x");
        System.out.println("  range of n, while waitingList rises roughly in proportion to n log n.");

        System.out.println();
        System.out.println("=".repeat(132));
        System.out.printf("HAND-VERIFIED CHECKS %d   PASSED %d   FAILED %d%n", pass + fail, pass, fail);
        System.out.println("=".repeat(132));
        System.out.println();
        System.out.println("Run Q6/Starvation.java for the fairness inquiry and the deployment decision,");
        System.out.println("and Q6/QueueAlternatives.java for the sorted-list and bucketed designs.");
        Bench.writeCsv("Q6", all);
        if (fail > 0) System.exit(1);
    }

    static String throwsOn(TriageQueue q, int severity) {
        try { q.admit("X", severity); return "no exception"; }
        catch (IllegalArgumentException e) { return e.getClass().getSimpleName(); }
    }
    static Bench.Result show(Bench.Result r) { Bench.row(r); return r; }
    static long SINK;
    static void sink(long v) { SINK += v; }
}
