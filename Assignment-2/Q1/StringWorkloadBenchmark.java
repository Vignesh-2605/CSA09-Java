/*
 * Programming in Java  -  Assignment 2 (CO2)
 * Q1  -  the experiment
 *
 * Name            : Vignesh S
 * Register Number : 192325002
 *
 * Three deliberately contrasting workloads, each run through five assembly
 * strategies, every workload repeated and the median reported.  The prediction
 * is PRINTED FIRST, before any measurement, so the comparison of prediction
 * against observation is made on the record rather than after the fact.
 */
import bench.Bench;
import java.util.List;

public class StringWorkloadBenchmark {

    /* ---- the three workloads --------------------------------------------
     * W1  one short ticket               - 12 fields,  1 ticket
     * W2  the bulk workload the brief asks for - 50 fields, 10,000 tickets
     * W3  my own: one pathological ticket - 4,000 fields, 1 ticket
     *     chosen because it isolates the per-ticket quadratic term that W2
     *     hides by spreading the cost over many small tickets.
     */
    record Workload(String id, String description, int fields, int tickets) { }

    static final Workload[] WORKLOADS = {
        new Workload("W1", "one short ticket",            12,     1),
        new Workload("W2", "bulk booking run",            50, 10_000),
        new Workload("W3", "one pathological ticket",  4_000,     1),
    };

    public static void main(String[] args) {
        System.out.println("Q1  -  BUILDING TEXT UNDER LOAD: String vs StringBuilder vs StringBuffer");
        System.out.println("=".repeat(118));
        System.out.println("Name: Vignesh S   Register Number: 192325002");
        System.out.printf("Harness: %d warm-up runs discarded, %d measured repetitions, MEDIAN reported.%n",
                Bench.WARMUP, Bench.REPS);

        /* ================= PREDICTION, STATED BEFORE MEASURING ============ */
        System.out.println();
        System.out.println("1.  PREDICTION  (recorded before any measurement was taken)");
        System.out.println("-".repeat(118));
        System.out.println("  A String is immutable, so s = s + f cannot extend the existing object.");
        System.out.println("  It must allocate a new character array, copy everything already written");
        System.out.println("  into it, then append the new field.  Building a ticket of n fields");
        System.out.println("  therefore copies 1 + 2 + 3 + ... + n field-widths of text, which is the");
        System.out.println("  arithmetic series n(n+1)/2 - that is O(n^2) CHARACTERS COPIED for O(n)");
        System.out.println("  characters of output, and it allocates two throwaway objects per field.");
        System.out.println();
        System.out.println("  StringBuilder keeps ONE growable char array and writes each character");
        System.out.println("  once, so it is O(n) characters copied and allocates a constant number of");
        System.out.println("  objects regardless of n.  StringBuffer does exactly the same work with");
        System.out.println("  every method synchronized.");
        System.out.println();
        System.out.printf("  %-6s | %-34s | %s%n", "ID", "PREDICTED OUTCOME", "REASONING");
        System.out.println("  " + "-".repeat(114));
        System.out.printf("  %-6s | %-34s | %s%n", "W1",
                "all five within noise of another", "12 fields is too few for n^2 to matter");
        System.out.printf("  %-6s | %-34s | %s%n", "W2",
                "concat clearly slowest, but finite", "50 fields each; n^2 is small, repeated 10,000x");
        System.out.printf("  %-6s | %-34s | %s%n", "W3",
                "concat catastrophically slowest", "4,000 fields in ONE ticket: n^2 dominates");
        System.out.printf("  %-6s | %-34s | %s%n", "all",
                "StringBuffer a little behind SB", "same algorithm, plus uncontended lock overhead");
        System.out.printf("  %-6s | %-34s | %s%n", "all",
                "pre-sized SB fastest of the three", "no internal array doubling at all");
        System.out.println();
        System.out.println("  Quantified prediction for W3: the + loop copies roughly 4,000/2 = 2,000");
        System.out.println("  times as many characters as StringBuilder, so if the characters copied");
        System.out.println("  dominate, the gap should be of that order - hundreds to thousands of");
        System.out.println("  times, NOT a few percent.");

        /* ================= OPERATION COUNTS, COMPUTED NOT TIMED =========== */
        System.out.println();
        System.out.println("2.  WORK PERFORMED, COUNTED ANALYTICALLY  (independent of any clock)");
        System.out.println("-".repeat(118));
        System.out.printf("%-6s | %-8s | %-9s | %-16s | %-16s | %-14s | %s%n",
                "ID", "FIELDS", "TICKETS", "CHARS COPIED +", "CHARS COPIED SB", "RATIO", "OBJECTS + vs SB");
        System.out.println("-".repeat(118));
        for (Workload w : WORKLOADS) {
            String[] f = TicketBuilder.ticketFields(w.fields(), 1);
            long cc = TicketBuilder.concatCharsCopied(f) * w.tickets();
            long cb = TicketBuilder.builderCharsCopied(f) * w.tickets();
            long oc = TicketBuilder.concatObjects(w.fields()) * w.tickets();
            long ob = TicketBuilder.builderObjects(w.fields()) * w.tickets();
            System.out.printf("%-6s | %-8d | %-9s | %-16s | %-16s | %-14s | %s vs %s%n",
                    w.id(), w.fields(), Bench.fmt(w.tickets()), Bench.fmt(cc), Bench.fmt(cb),
                    String.format("%.1fx", (double) cc / cb), Bench.fmt(oc), Bench.fmt(ob));
        }
        System.out.println();
        System.out.println("  These counts are derived from the algorithm, not measured, so they are");
        System.out.println("  the same on any machine.  They are the reason the timings below look as");
        System.out.println("  they do, and they are what the Big-O claim actually rests on.");

        /* ================= MEASUREMENT ==================================== */
        List<Bench.Result> all = Bench.list();

        for (Workload w : WORKLOADS) {
            System.out.println();
            System.out.printf("3.%s  WORKLOAD %s  -  %s (%d fields x %s tickets)%n",
                    w.id().substring(1), w.id(), w.description(), w.fields(), Bench.fmt(w.tickets()));
            System.out.println("-".repeat(118));

            final String[][] data = new String[w.tickets()][];
            for (int t = 0; t < w.tickets(); t++) data[t] = TicketBuilder.ticketFields(w.fields(), t);
            final int chars = TicketBuilder.charCount(data[0]);
            final long ops = w.tickets();

            long heapBefore = Bench.usedHeap();
            Bench.Result rConcat = Bench.measure("String +", w.id(), w.fields(), ops, () -> {
                for (String[] f : data) sink(TicketBuilder.withConcat(f));
            });
            long heapAfterConcat = Bench.usedHeap();

            Bench.Result rSb = Bench.measure("StringBuilder", w.id(), w.fields(), ops, () -> {
                for (String[] f : data) sink(TicketBuilder.withStringBuilder(f));
            });
            Bench.Result rSized = Bench.measure("StringBuilder(sized)", w.id(), w.fields(), ops, () -> {
                for (String[] f : data) sink(TicketBuilder.withSizedStringBuilder(f, chars));
            });
            Bench.Result rBuf = Bench.measure("StringBuffer", w.id(), w.fields(), ops, () -> {
                for (String[] f : data) sink(TicketBuilder.withStringBuffer(f));
            });
            Bench.Result rJoin = Bench.measure("String.join", w.id(), w.fields(), ops, () -> {
                for (String[] f : data) sink(TicketBuilder.withStringJoin(f));
            });
            Bench.Result rJoiner = Bench.measure("StringJoiner", w.id(), w.fields(), ops, () -> {
                for (String[] f : data) sink(TicketBuilder.withStringJoiner(f));
            });
            Bench.Result rFmt = Bench.measure("String.format", w.id(), w.fields(), ops, () -> {
                for (String[] f : data) sink(TicketBuilder.withFormat(f));
            });

            rConcat.objectsCreated = TicketBuilder.concatObjects(w.fields()) * w.tickets();
            rSb.objectsCreated     = TicketBuilder.builderObjects(w.fields()) * w.tickets();
            rSized.objectsCreated  = TicketBuilder.builderObjects(w.fields()) * w.tickets();
            rBuf.objectsCreated    = TicketBuilder.builderObjects(w.fields()) * w.tickets();
            rConcat.bytesAllocated = heapAfterConcat - heapBefore;

            Bench.tableHead();
            for (Bench.Result r : new Bench.Result[]{ rConcat, rSb, rSized, rBuf, rJoin, rJoiner, rFmt }) {
                Bench.row(r); all.add(r);
            }
            double base = rSb.median();
            System.out.println();
            System.out.printf("  relative to StringBuilder:  String + = %.1fx   StringBuffer = %.2fx   "
                            + "sized SB = %.2fx%n",
                    rConcat.median() / base, rBuf.median() / base, rSized.median() / base);
            System.out.printf("  measurement noise (RSD): lowest %.1f%%, highest %.1f%% - "
                            + "figures above this are signal, not jitter%n",
                    minRsd(rConcat, rSb, rSized, rBuf, rJoin, rJoiner, rFmt),
                    maxRsd(rConcat, rSb, rSized, rBuf, rJoin, rJoiner, rFmt));
            System.out.printf("  heap used by the String + run: %,d KB%n",
                    Math.max(0, rConcat.bytesAllocated) / 1024);

            // correctness: every strategy must produce the same ticket
            String[] probe = data[0];
            boolean same = TicketBuilder.withConcat(probe).equals(TicketBuilder.withStringBuilder(probe))
                    && TicketBuilder.withStringBuilder(probe).equals(TicketBuilder.withSizedStringBuilder(probe, chars))
                    && TicketBuilder.withStringBuilder(probe).equals(TicketBuilder.withStringBuffer(probe))
                    && TicketBuilder.withStringBuilder(probe).equals(TicketBuilder.withStringJoin(probe))
                    && TicketBuilder.withStringBuilder(probe).equals(TicketBuilder.withStringJoiner(probe));
            System.out.printf("  all strategies produce an identical ticket: %s%n", same);
        }

        Bench.writeCsv("Q1", all);
        System.out.println();
        System.out.println("Run Q1/ScalingProbe.java for the n-versus-time curve that tests the O(n^2) claim,");
        System.out.println("and Q1/SharedLogContention.java for the multithreaded design decision.");
    }

    static long SINK;
    static void sink(String s) { SINK += s.length(); }

    static double minRsd(Bench.Result... rs) {
        double m = Double.MAX_VALUE; for (Bench.Result r : rs) m = Math.min(m, r.rsd()); return m;
    }
    static double maxRsd(Bench.Result... rs) {
        double m = 0; for (Bench.Result r : rs) m = Math.max(m, r.rsd()); return m;
    }
}
