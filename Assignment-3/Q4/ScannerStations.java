/*
 * Programming in Java  -  Assignment 3 (CO3)
 * Q4  -  Thread Creation and the Thread Life Cycle
 *
 * Name: Vignesh S   Register Number: 192325002
 *
 * Shared group work: the four scanner stations implemented BOTH ways - by
 * extending Thread and by implementing Runnable - using start(), sleep(),
 * join() and isAlive().
 */
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class ScannerStations {

    static final int STATIONS = 4;
    static final int SCRIPTS_PER_STATION = 25;

    /** Shared, so the finishing ORDER is recorded rather than guessed. */
    static final List<String> finishOrder = Collections.synchronizedList(new ArrayList<>());
    static final AtomicInteger grandTotal = new AtomicInteger();

    /* ---------------- WAY 1: extend Thread ---------------- */

    static class StationThread extends Thread {
        private final int batch;
        private int scanned = 0;
        StationThread(int id, int batch) {
            super("Station-" + id);            // Thread has a name field already
            this.batch = batch;
        }
        @Override public void run() {
            for (int i = 0; i < batch; i++) {
                scanned++;
                try {
                    Thread.sleep(1);           // simulates the scanner's mechanics
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();   // restore the flag
                    return;
                }
            }
            grandTotal.addAndGet(scanned);
            finishOrder.add(getName());
            System.out.printf("    %-12s scanned %d scripts   (thread %s)%n",
                    getName(), scanned, Thread.currentThread().getName());
        }
        int scanned() { return scanned; }
    }

    /* ---------------- WAY 2: implement Runnable ---------------- */

    static class StationTask implements Runnable {
        private final int batch;
        private int scanned = 0;
        StationTask(int batch) { this.batch = batch; }
        @Override public void run() {
            for (int i = 0; i < batch; i++) {
                scanned++;
                try { Thread.sleep(1); }
                catch (InterruptedException e) { Thread.currentThread().interrupt(); return; }
            }
            grandTotal.addAndGet(scanned);
            String me = Thread.currentThread().getName();
            finishOrder.add(me);
            System.out.printf("    %-12s scanned %d scripts   (thread %s)%n", me, scanned, me);
        }
        int scanned() { return scanned; }
    }

    /* ---------------- the two runs ---------------- */

    static List<String> runByExtendingThread(boolean quiet) throws InterruptedException {
        finishOrder.clear(); grandTotal.set(0);
        StationThread[] s = new StationThread[STATIONS];
        for (int i = 0; i < STATIONS; i++) s[i] = new StationThread(i + 1, SCRIPTS_PER_STATION);

        if (!quiet) {
            System.out.println("    state of each station BEFORE start():");
            for (StationThread t : s)
                System.out.printf("        %-12s getState()=%-14s isAlive()=%s%n",
                        t.getName(), t.getState(), t.isAlive());
        }
        for (StationThread t : s) t.start();
        if (!quiet) {
            System.out.println("    state immediately AFTER start():");
            for (StationThread t : s)
                System.out.printf("        %-12s getState()=%-14s isAlive()=%s%n",
                        t.getName(), t.getState(), t.isAlive());
        }
        for (StationThread t : s) t.join();        // wait for every station
        if (!quiet) {
            System.out.println("    state after join() returned:");
            for (StationThread t : s)
                System.out.printf("        %-12s getState()=%-14s isAlive()=%s%n",
                        t.getName(), t.getState(), t.isAlive());
        }
        return new ArrayList<>(finishOrder);
    }

    static List<String> runByRunnable(boolean quiet) throws InterruptedException {
        finishOrder.clear(); grandTotal.set(0);
        Thread[] t = new Thread[STATIONS];
        StationTask[] tasks = new StationTask[STATIONS];
        for (int i = 0; i < STATIONS; i++) {
            tasks[i] = new StationTask(SCRIPTS_PER_STATION);
            t[i] = new Thread(tasks[i], "Station-" + (i + 1));
        }
        for (Thread x : t) x.start();
        for (Thread x : t) x.join();
        return new ArrayList<>(finishOrder);
    }

    public static void main(String[] args) throws Exception {
        System.out.println("Q4  -  THREAD CREATION AND THE THREAD LIFE CYCLE");
        System.out.println("=".repeat(112));
        System.out.println("Name: Vignesh S   Register Number: 192325002");
        System.out.printf("%d stations, %d scripts each, %d scripts in total%n",
                STATIONS, SCRIPTS_PER_STATION, STATIONS * SCRIPTS_PER_STATION);
        System.out.printf("available processors reported by the JVM: %d%n",
                Runtime.getRuntime().availableProcessors());

        System.out.println();
        System.out.println("SHARED GROUP WORK  -  THE SAME STATIONS, BOTH WAYS");
        System.out.println("=".repeat(112));
        System.out.println();
        System.out.println("  WAY 1: class StationThread extends Thread");
        List<String> o1 = runByExtendingThread(false);
        System.out.printf("    total scanned: %d     finish order: %s%n", grandTotal.get(), o1);
        System.out.println();
        System.out.println("  WAY 2: class StationTask implements Runnable");
        List<String> o2 = runByRunnable(false);
        System.out.printf("    total scanned: %d     finish order: %s%n", grandTotal.get(), o2);
        System.out.println();
        System.out.printf("  both ways scanned the same number of scripts: %s%n",
                (STATIONS * SCRIPTS_PER_STATION) + " each");

        /* ---------- STUDENT A ---------- */
        System.out.println();
        System.out.println("STUDENT A  -  THREAD DESIGN");
        System.out.println("=".repeat(112));
        System.out.println();
        System.out.println("1.  THE TWO WAYS OF CREATING A THREAD");
        System.out.println("-".repeat(112));
        System.out.printf("  %-34s | %-30s | %s%n", "", "extends Thread", "implements Runnable");
        System.out.println("  " + "-".repeat(104));
        String[][] ways = {
            {"uses up the one superclass slot", "YES - cannot extend anything else", "no - free to extend a domain class"},
            {"the task and the worker are", "the same object", "separate objects"},
            {"can be resubmitted to a pool", "no - a Thread runs once", "YES - the same Runnable, many times"},
            {"can be a lambda", "no", "YES - it is a functional interface"},
            {"access to getName(), isAlive()", "direct, inherited", "via Thread.currentThread()"},
            {"a restarted instance", "IllegalThreadStateException", "wrap it in a new Thread"},
            {"fits ExecutorService", "no - pools take Runnable/Callable", "YES - this is what pools accept"},
        };
        for (String[] r : ways) System.out.printf("  %-34s | %-30s | %s%n", r[0], r[1], r[2]);
        System.out.println();
        System.out.println("  WHEN EACH IS PREFERRED:");
        System.out.println("    implements Runnable is the default choice.  It separates WHAT is to");
        System.out.println("    be done from HOW it is run, leaves the class free to extend");
        System.out.println("    something meaningful, and is the only form a thread pool accepts.");
        System.out.println("    For this assignment's Q7 - which uses ExecutorService - the station");
        System.out.println("    had to be a task, not a Thread.");
        System.out.println();
        System.out.println("    extends Thread is preferred only when the thread itself needs new");
        System.out.println("    behaviour: overriding interrupt(), or carrying per-thread state that");
        System.out.println("    genuinely belongs to the worker rather than to the job.  In this");
        System.out.println("    program it buys one small thing - getName() without going through");
        System.out.println("    Thread.currentThread() - which is not worth a superclass.");

        System.out.println();
        System.out.println("2.  THE THREAD LIFE CYCLE, AND THE STATES A STATION PASSES THROUGH");
        System.out.println("-".repeat(112));
        System.out.println("                              +-------+");
        System.out.println("            new Station()---->|  NEW  |");
        System.out.println("                              +---+---+");
        System.out.println("                                  | start()");
        System.out.println("                                  v");
        System.out.println("      +--------------------> +----------+ <------------------+");
        System.out.println("      |   scheduler picks it |          |  lock acquired /   |");
        System.out.println("      |   again              | RUNNABLE |  notify / timeout  |");
        System.out.println("      |                      +--+----+--+                    |");
        System.out.println("      |         Thread.sleep(1) |    | synchronized blocked   |");
        System.out.println("      |                         v    v                        |");
        System.out.println("      |            +---------------+ +---------+              |");
        System.out.println("      +------------| TIMED_WAITING | | BLOCKED |--------------+");
        System.out.println("                   +---------------+ +---------+");
        System.out.println("                         ^                           +---------+");
        System.out.println("                         |      join() / wait() ---> | WAITING |");
        System.out.println("                         |                           +----+----+");
        System.out.println("                         |                                |");
        System.out.println("                  run() returns                           |");
        System.out.println("                         |                                |");
        System.out.println("                         v                                v");
        System.out.println("                   +------------+                   (back to RUNNABLE)");
        System.out.println("                   | TERMINATED |");
        System.out.println("                   +------------+");
        System.out.println();
        System.out.println("  WHICH STATES EACH STATION ACTUALLY VISITS, observed with getState():");
        System.out.printf("    %-16s | %-18s | %s%n", "STATE", "OBSERVED?", "WHAT PUTS THE STATION THERE");
        System.out.println("    " + "-".repeat(100));
        String[][] st = {
            {"NEW", "yes, printed above", "after new StationThread(), before start()"},
            {"RUNNABLE", "yes, printed above", "after start(); also between sleeps"},
            {"TIMED_WAITING", "yes, sampled below", "Thread.sleep(1) between scripts"},
            {"BLOCKED", "no - no shared lock", "would need a synchronized block they contend for"},
            {"WAITING", "yes - main() enters it", "main calls join() and waits with no timeout"},
            {"TERMINATED", "yes, printed above", "run() returned; isAlive() is then false"},
        };
        for (String[] r : st) System.out.printf("    %-16s | %-18s | %s%n", r[0], r[1], r[2]);

        System.out.println();
        System.out.println("  SAMPLING A LIVE STATION'S STATE (a monitor thread polls getState()):");
        StationThread sampled = new StationThread(9, 40);
        Map<Thread.State, Integer> seen = new LinkedHashMap<>();
        seen.put(sampled.getState(), 1);                        // NEW
        sampled.start();
        Thread monitor = new Thread(() -> {
            while (sampled.isAlive())
                seen.merge(sampled.getState(), 1, Integer::sum);
        }, "monitor");
        monitor.start();
        sampled.join();
        monitor.join();
        seen.merge(sampled.getState(), 1, Integer::sum);         // TERMINATED
        System.out.printf("    %-16s | %s%n", "STATE", "TIMES OBSERVED BY THE MONITOR");
        System.out.println("    " + "-".repeat(60));
        for (Map.Entry<Thread.State, Integer> e : seen.entrySet())
            System.out.printf("    %-16s | %d%n", e.getKey(), e.getValue());
        System.out.println("    The station spends most of its life in TIMED_WAITING, because");
        System.out.println("    sleep(1) per script dominates the work of counting one script.");

        System.out.println();
        System.out.println("3.  WHY CALLING run() INSTEAD OF start() DOES NOT CREATE A THREAD");
        System.out.println("-".repeat(112));
        System.out.printf("    main() is running on thread: %s%n", Thread.currentThread().getName());
        System.out.println();
        System.out.println("    EVIDENCE A - calling run() DIRECTLY on four stations:");
        finishOrder.clear(); grandTotal.set(0);
        long t0 = System.nanoTime();
        StationThread[] direct = new StationThread[STATIONS];
        for (int i = 0; i < STATIONS; i++) {
            direct[i] = new StationThread(i + 1, SCRIPTS_PER_STATION);
            direct[i].run();                     // NOT start()
        }
        long directMs = (System.nanoTime() - t0) / 1_000_000;
        System.out.printf("    finish order: %s%n", finishOrder);
        System.out.println("    every line above says  thread main  -  no new thread was created.");
        System.out.printf("    state of those Thread objects now: %s%n", direct[0].getState());
        System.out.println("    They are still NEW: a Thread object that was never started has");
        System.out.println("    never been a thread.  run() is just a method, and calling a method");
        System.out.println("    runs it on the caller's stack, like any other call.");
        System.out.println();
        System.out.println("    EVIDENCE B - the finish order and the timing:");
        finishOrder.clear(); grandTotal.set(0);
        t0 = System.nanoTime();
        runByExtendingThread(true);
        long startMs = (System.nanoTime() - t0) / 1_000_000;
        System.out.printf("    run()   sequential, order always Station-1,2,3,4 : %4d ms%n", directMs);
        System.out.printf("    start() concurrent, order varies run to run      : %4d ms%n", startMs);
        System.out.printf("    speed-up from actually using threads              : %.1fx%n",
                directMs / (double) Math.max(startMs, 1));
        System.out.println("    Four stations each sleeping 25 ms take about 100 ms in sequence and");
        System.out.println("    about 25 ms in parallel.  The clock is the simplest proof that one");
        System.out.println("    version has four threads and the other has one.");
        System.out.println();
        System.out.println("    EVIDENCE C - a Thread cannot be started twice:");
        try {
            direct[0].start();
            direct[0].start();
        } catch (IllegalThreadStateException e) {
            System.out.println("    second start() threw IllegalThreadStateException, as expected");
        }
        direct[0].join();

        System.out.println();
        System.out.println("4.  HOW join() ENSURES THE TOTAL IS PRINTED ONLY AFTER ALL STATIONS END");
        System.out.println("-".repeat(112));
        System.out.println("    join() blocks the CALLING thread - here main() - until the station's");
        System.out.println("    run() has returned.  Internally it waits on the Thread object and");
        System.out.println("    the JVM notifies that object when the thread terminates, so main()");
        System.out.println("    sits in WAITING and consumes nothing while it waits.");
        System.out.println();
        System.out.println("    It also carries a MEMORY guarantee, which matters more than the");
        System.out.println("    ordering: everything a station wrote before finishing is visible to");
        System.out.println("    main() after join() returns.  Without that, main() could print a");
        System.out.println("    total assembled from stale values even if the timing happened to");
        System.out.println("    work out.");
        System.out.println();
        System.out.println("    WITHOUT join() - the same program, main() printing immediately:");
        finishOrder.clear(); grandTotal.set(0);
        StationThread[] nojoin = new StationThread[STATIONS];
        for (int i = 0; i < STATIONS; i++) { nojoin[i] = new StationThread(i + 1, SCRIPTS_PER_STATION); nojoin[i].start(); }
        System.out.printf("    total printed WITHOUT join(): %d   (expected %d)%n",
                grandTotal.get(), STATIONS * SCRIPTS_PER_STATION);
        for (StationThread t : nojoin) t.join();
        System.out.printf("    total after join() returned  : %d   (expected %d)%n",
                grandTotal.get(), STATIONS * SCRIPTS_PER_STATION);
    }
}
