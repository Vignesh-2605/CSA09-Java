/*
 * Programming in Java  -  Assignment 3 (CO3)
 * Q6  -  Student A: making the if/while difference actually observable
 *
 * Name            : Vignesh S   Register Number: 192325002
 *
 * The supplied workload (20 tokens, 2 consumers) does NOT expose the if-version
 * defect: 12 consecutive runs all printed 210.  That is a finding, not a
 * failure to find one - it is the ordinary situation with concurrency defects,
 * and it is why "it worked when I ran it" is not evidence.
 *
 * This harness changes the TIMING, not the logic, so that the condition the
 * defect needs actually arises: a SLOW producer and SEVERAL consumers, so that
 * more than one consumer is genuinely waiting when notifyAll() fires.
 */
import java.util.concurrent.atomic.AtomicInteger;

public class IfVersusWhileStress {

    /** One counter, parameterised by whether it re-tests after waking. */
    static final class Counter {
        private final int[] buffer;
        private int count = 0, in = 0, out = 0;
        private final boolean useWhile;
        final AtomicInteger faults = new AtomicInteger();
        volatile int worstCount = 0;
        volatile String firstFault = null;

        Counter(int capacity, boolean useWhile) {
            this.buffer = new int[capacity]; this.useWhile = useWhile;
        }

        synchronized void put(int token) throws InterruptedException {
            if (useWhile) { while (count == buffer.length) wait(); }
            else          { if    (count == buffer.length) wait(); }
            if (count == buffer.length) {              // the condition it was told to avoid
                faults.incrementAndGet();
                if (firstFault == null) firstFault = "put() ran while FULL - a token is overwritten";
            }
            buffer[in] = token;
            in = (in + 1) % buffer.length;
            count++;
            notifyAll();
        }

        synchronized int take() throws InterruptedException {
            if (useWhile) { while (count == 0) wait(); }
            else          { if    (count == 0) wait(); }
            if (count == 0) {
                faults.incrementAndGet();
                if (firstFault == null) firstFault = "take() ran while EMPTY - a stale slot is read";
            }
            int token = buffer[out];
            out = (out + 1) % buffer.length;
            count--;
            if (count < worstCount) worstCount = count;   // should never go below 0
            notifyAll();
            return token;
        }
        int count() { return count; }
    }

    static final int TOKENS = 240;
    static final int CONSUMERS = 6;
    static final int CAPACITY = 2;
    static final int TRIALS = 25;

    record Outcome(int wrongSum, int faults, int worstCount, int timeouts) { }

    public static void main(String[] args) throws Exception {
        System.out.println("Q6  -  WHY wait() MUST BE CALLED IN A LOOP");
        System.out.println("=".repeat(104));
        System.out.println("Name: Vignesh S   Register Number: 192325002");
        System.out.println("Primary responsibility: Student A");
        System.out.println();
        System.out.println("1.  THE SUPPLIED WORKLOAD DOES NOT EXPOSE THE DEFECT");
        System.out.println("-".repeat(104));
        System.out.println("  20 tokens, 2 consumers, buffer of 5: 12 consecutive runs of the if");
        System.out.println("  version all printed the correct 210 (see if_output.txt).  The reason is");
        System.out.println("  that the producer is fast and the buffer is large, so a consumer almost");
        System.out.println("  never has to wait - and a consumer that never waits cannot be woken");
        System.out.println("  spuriously into a stale state.");
        System.out.println();
        System.out.println("  The defect is still there.  It is simply not REACHED by that timing,");
        System.out.println("  which is the ordinary situation with concurrency bugs and the reason");
        System.out.println("  a passing run is not evidence of correctness.");

        System.out.println();
        System.out.println("2.  A WORKLOAD DESIGNED TO REACH IT");
        System.out.println("-".repeat(104));
        System.out.printf("  %d tokens, %d consumers, buffer capacity %d, producer paused between tokens.%n",
                TOKENS, CONSUMERS, CAPACITY);
        System.out.println("  Several consumers are therefore waiting on an empty counter when the");
        System.out.println("  producer calls notifyAll().  notifyAll() wakes ALL of them; exactly one");
        System.out.println("  can consume the single token, and the rest must re-test.  The while");
        System.out.println("  version re-tests and waits again; the if version does not.");
        System.out.println();
        System.out.printf("  Running %d independent trials of each version.%n", TRIALS);
        System.out.println();

        Outcome ifV = trials(false);
        Outcome whileV = trials(true);

        System.out.printf("%-26s | %-14s | %-16s | %-18s | %s%n",
                "VERSION", "WRONG SUM", "CONTRACT FAULTS", "WORST count VALUE", "HUNG RUNS");
        System.out.println("-".repeat(104));
        System.out.printf("%-26s | %-14s | %-16s | %-18s | %s%n", "if  (no re-test)",
                ifV.wrongSum() + " of " + TRIALS, ifV.faults(), ifV.worstCount(), ifV.timeouts());
        System.out.printf("%-26s | %-14s | %-16s | %-18s | %s%n", "while (re-tests)",
                whileV.wrongSum() + " of " + TRIALS, whileV.faults(), whileV.worstCount(), whileV.timeouts());

        System.out.println();
        System.out.println("3.  WHAT THE FAILURE ACTUALLY IS");
        System.out.println("-".repeat(104));
        System.out.println("  interleaving that breaks the if version, with consumers C1 and C2:");
        System.out.println("    1. C1 calls take(), finds count == 0, calls wait() and releases the lock");
        System.out.println("    2. C2 calls take(), finds count == 0, calls wait() and releases the lock");
        System.out.println("    3. producer calls put(), stores one token, count becomes 1, notifyAll()");
        System.out.println("    4. C1 re-acquires the lock, and because it used if it does NOT re-test;");
        System.out.println("       it consumes the token, count becomes 0");
        System.out.println("    5. C2 re-acquires the lock and also does not re-test.  It reads");
        System.out.println("       buffer[out] - a slot that holds a STALE token - and decrements");
        System.out.println("       count to -1");
        System.out.println();
        System.out.println("  So a token is counted twice and the invariant 0 <= count <= capacity is");
        System.out.println("  broken.  The negative count in the table above is that invariant being");
        System.out.println("  violated, observed rather than argued.");
        System.out.println();
        System.out.println("  WHY wait() MUST ALSO BE INSIDE synchronized: wait() releases the");
        System.out.println("  monitor, so the caller must hold it first.  Calling it without the lock");
        System.out.println("  throws IllegalMonitorStateException:");
        try {
            Object o = new Object();
            o.wait(1);
        } catch (IllegalMonitorStateException e) {
            System.out.println("    new Object().wait(1) outside synchronized -> "
                    + e.getClass().getSimpleName());
        } catch (InterruptedException ignored) { }
        System.out.println();
        System.out.println("  More fundamentally, testing the condition and waiting must be ONE");
        System.out.println("  atomic step.  If the test were outside the lock, the condition could");
        System.out.println("  change between the test and the wait, and the thread would sleep");
        System.out.println("  waiting for a notification that has already been sent - a lost wakeup,");
        System.out.println("  which hangs the program permanently.");

        System.out.println();
        System.out.println("4.  sleep() AND wait() ARE NOT INTERCHANGEABLE");
        System.out.println("-".repeat(104));
        System.out.printf("%-22s | %-26s | %-26s | %s%n", "", "Thread.sleep(ms)", "Object.wait()", "CONSEQUENCE HERE");
        System.out.println("-".repeat(104));
        String[][] rows = {
            {"declared on",    "Thread (static)",        "Object (instance)",      "wait belongs to the monitor"},
            {"releases the lock","NO",                   "YES",                    "sleep inside put() would freeze the counter"},
            {"woken by",       "time only",              "notify/notifyAll or time","wait reacts to the state changing"},
            {"needs the lock", "no",                     "yes",                    "IllegalMonitorStateException otherwise"},
            {"used for",       "pacing / simulation",    "waiting for a condition","they solve different problems"},
        };
        for (String[] r : rows) System.out.printf("%-22s | %-26s | %-26s | %s%n", r[0], r[1], r[2], r[3]);
        System.out.println();
        System.out.println("  The decisive row is the second.  If the consumer used sleep() while the");
        System.out.println("  counter was empty, it would hold the lock the whole time, so the");
        System.out.println("  producer could never enter put() to supply the token the consumer is");
        System.out.println("  waiting for.  That is not slow - it is a permanent deadlock.");
    }

    static Outcome trials(boolean useWhile) throws Exception {
        int wrong = 0, faults = 0, worst = 0, timeouts = 0;
        for (int t = 0; t < TRIALS; t++) {
            Counter c = new Counter(CAPACITY, useWhile);
            int perConsumer = TOKENS / CONSUMERS;
            long[] sums = new long[CONSUMERS];
            Thread prod = new Thread(() -> {
                try {
                    for (int i = 1; i <= TOKENS; i++) { c.put(i); Thread.sleep(0, 200_000); }
                } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            });
            Thread[] cons = new Thread[CONSUMERS];
            for (int k = 0; k < CONSUMERS; k++) {
                final int id = k;
                cons[k] = new Thread(() -> {
                    try { for (int i = 0; i < perConsumer; i++) sums[id] += c.take(); }
                    catch (InterruptedException e) { Thread.currentThread().interrupt(); }
                });
            }
            prod.start(); for (Thread x : cons) x.start();
            prod.join(8000);
            boolean hung = false;
            for (Thread x : cons) { x.join(8000); if (x.isAlive()) hung = true; }
            if (hung) { timeouts++; for (Thread x : cons) x.interrupt(); prod.interrupt(); }
            long sum = 0; for (long s : sums) sum += s;
            long expected = (long) TOKENS * (TOKENS + 1) / 2;
            if (!hung && sum != expected) wrong++;
            faults += c.faults.get();
            worst = Math.min(worst, c.worstCount);
        }
        return new Outcome(wrong, faults, worst, timeouts);
    }
}
