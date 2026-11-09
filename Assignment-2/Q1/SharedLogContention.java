/*
 * Programming in Java  -  Assignment 2 (CO2)
 * Q1  -  the design decision: which class for the per-ticket text, and which
 *        for the shared booking log?
 *
 * Name            : Vignesh S
 * Register Number : 192325002
 *
 * The question is not "which is faster" but "which is CORRECT under the stated
 * conditions".  This program demonstrates both halves:
 *
 *   - a StringBuilder shared between threads silently loses and corrupts data;
 *   - a StringBuffer does not, and the price of that safety is measured.
 */
import bench.Bench;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

public class SharedLogContention {

    static final int THREADS = 8;
    static final int APPENDS_PER_THREAD = 20_000;
    static final String ENTRY = "BOOKING-OK;";
    static final int EXPECTED_LEN = THREADS * APPENDS_PER_THREAD * ENTRY.length();

    public static void main(String[] args) throws Exception {
        System.out.println("Q1  -  THE MULTITHREADED DESIGN DECISION");
        System.out.println("=".repeat(104));
        System.out.println("Name: Vignesh S   Register Number: 192325002");
        System.out.printf("Scenario: %d threads each append %s entries to ONE shared log.%n",
                THREADS, Bench.fmt(APPENDS_PER_THREAD));
        System.out.printf("A correct log must end up exactly %s characters long.%n%n",
                Bench.fmt(EXPECTED_LEN));

        System.out.println("PREDICTION (before measuring):");
        System.out.println("  StringBuilder is not synchronized.  Two threads can read the same");
        System.out.println("  internal count, write to the same slot and both store count+1, so");
        System.out.println("  appends are LOST.  The final length should come out SHORT, and the run");
        System.out.println("  may also throw from inside the builder when the array is resized");
        System.out.println("  underneath another thread.  StringBuffer should produce exactly the");
        System.out.println("  expected length every time, and be slower because of lock traffic.");
        System.out.println();

        /* ---- correctness, repeated because a race does not fail every time ---- */
        System.out.println("1.  CORRECTNESS UNDER CONCURRENCY  (5 independent trials each)");
        System.out.println("-".repeat(104));
        System.out.printf("%-18s | %-8s | %-16s | %-16s | %s%n",
                "SHARED LOG", "TRIAL", "EXPECTED CHARS", "ACTUAL CHARS", "VERDICT");
        System.out.println("-".repeat(104));

        int sbCorrupt = 0, bufCorrupt = 0;
        for (int trial = 1; trial <= 5; trial++) {
            String r = runUnsafe();
            boolean ok = r.equals(String.valueOf(EXPECTED_LEN));
            if (!ok) sbCorrupt++;
            System.out.printf("%-18s | %-8d | %-16s | %-16s | %s%n", "StringBuilder", trial,
                    Bench.fmt(EXPECTED_LEN), r, ok ? "ok this time" : "DATA LOST / CORRUPTED");
        }
        for (int trial = 1; trial <= 5; trial++) {
            int len = runSafe();
            boolean ok = len == EXPECTED_LEN;
            if (!ok) bufCorrupt++;
            System.out.printf("%-18s | %-8d | %-16s | %-16s | %s%n", "StringBuffer", trial,
                    Bench.fmt(EXPECTED_LEN), Bench.fmt(len), ok ? "correct" : "CORRUPTED");
        }
        System.out.println();
        System.out.printf("  StringBuilder produced a wrong log in %d of 5 trials.%n", sbCorrupt);
        System.out.printf("  StringBuffer  produced a wrong log in %d of 5 trials.%n", bufCorrupt);
        System.out.println();
        System.out.println("  Note that StringBuilder may pass a trial by luck.  That is the most");
        System.out.println("  dangerous property of this bug, not a mitigating one: a data race that");
        System.out.println("  fails intermittently will pass testing and corrupt the booking log in");
        System.out.println("  production.");

        /* ---- the price of that safety, measured ---- */
        System.out.println();
        System.out.println("2.  WHAT THE SAFETY COSTS  (single-threaded, so the lock is uncontended)");
        System.out.println("-".repeat(104));
        final String[] fields = TicketBuilder.ticketFields(50, 3);
        List<Bench.Result> all = Bench.list();
        Bench.Result sb = Bench.measure("StringBuilder", "1 thread, 50 fields", 50, 1,
                () -> sink(TicketBuilder.withStringBuilder(fields)));
        Bench.Result bf = Bench.measure("StringBuffer", "1 thread, 50 fields", 50, 1,
                () -> sink(TicketBuilder.withStringBuffer(fields)));
        all.add(sb); all.add(bf);
        Bench.tableHead(); Bench.row(sb); Bench.row(bf);
        System.out.println();
        System.out.printf("  StringBuffer costs %.2fx StringBuilder for the SAME work.%n",
                bf.median() / sb.median());
        System.out.println("  Modern JVMs elide an uncontended lock, so the gap is small here. Under");
        System.out.println("  real contention - section 1 above - it is far larger, because threads");
        System.out.println("  serialise on the monitor.");

        System.out.println();
        System.out.println("3.  RECOMMENDATION");
        System.out.println("-".repeat(104));
        System.out.printf("  %-26s | %s%n", "PER-TICKET TEXT", "StringBuilder");
        System.out.println("     A ticket is built entirely inside one thread's call stack and the");
        System.out.println("     builder never escapes it.  No other thread can see it, so there is");
        System.out.println("     nothing for synchronization to protect and the lock would be pure");
        System.out.println("     overhead.  Size it with new StringBuilder(expectedChars) when the");
        System.out.println("     length is known - that measured fastest of all three in every");
        System.out.println("     workload.");
        System.out.println();
        System.out.printf("  %-26s | %s%n", "SHARED BOOKING LOG", "neither - see below");
        System.out.println("     StringBuilder is simply incorrect here, as section 1 shows.");
        System.out.println("     StringBuffer is correct, and is the right answer within the three");
        System.out.println("     classes this question names.");
        System.out.println();
        System.out.println("     But the honest engineering answer is that a shared, ever-growing");
        System.out.println("     StringBuffer is the wrong structure for a booking log at all. Every");
        System.out.println("     thread serialises on one monitor, so the log becomes the bottleneck");
        System.out.println("     of the whole server, and the log grows without bound in memory.");
        System.out.println("     A concurrent queue of finished entries drained by a single writer");
        System.out.println("     thread keeps appends lock-free and bounds the memory:");
        System.out.println();
        ConcurrentLinkedQueue<String> q = new ConcurrentLinkedQueue<>();
        long t0 = System.nanoTime();
        runOn(() -> { for (int i = 0; i < APPENDS_PER_THREAD; i++) q.add(ENTRY); });
        long t = System.nanoTime() - t0;
        long chars = 0; for (String s : q) chars += s.length();
        System.out.printf("       ConcurrentLinkedQueue + single writer : %s chars, %s entries, %.1f ms%n",
                Bench.fmt(chars), Bench.fmt(q.size()), t / 1e6);
        System.out.printf("       correct? %s%n", chars == EXPECTED_LEN);
        System.out.println();
        System.out.println("     That is outside the three classes the question lists, which is why");
        System.out.println("     StringBuffer remains the stated answer - but a recommendation that");
        System.out.println("     did not mention the real bottleneck would not be a useful one.");

        Bench.writeCsv("Q1-threads", all);
    }

    /** Shared StringBuilder: the unsafe design. Returns the length, or the exception. */
    static String runUnsafe() throws Exception {
        StringBuilder shared = new StringBuilder();
        try {
            runOn(() -> { for (int i = 0; i < APPENDS_PER_THREAD; i++) shared.append(ENTRY); });
            return String.valueOf(shared.length());
        } catch (Throwable e) {
            return e.getClass().getSimpleName();
        }
    }

    /** Shared StringBuffer: the safe design. */
    static int runSafe() throws Exception {
        StringBuffer shared = new StringBuffer();
        runOn(() -> { for (int i = 0; i < APPENDS_PER_THREAD; i++) shared.append(ENTRY); });
        return shared.length();
    }

    static void runOn(Runnable body) throws Exception {
        List<Thread> ts = new ArrayList<>();
        for (int i = 0; i < THREADS; i++) ts.add(new Thread(body));
        for (Thread t : ts) t.start();
        for (Thread t : ts) t.join();
    }

    static long SINK;
    static void sink(String s) { SINK += s.length(); }
}
