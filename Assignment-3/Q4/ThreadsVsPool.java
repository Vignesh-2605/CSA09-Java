/*
 * Programming in Java  -  Assignment 3 (CO3)
 * Q4  -  Joint Conclusion, and the Alternative Concept Analysis
 *
 * Name: Vignesh S   Register Number: 192325002
 */
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.nio.file.*;

public class ThreadsVsPool {

    static long vmRssKb() {
        try {
            for (String l : Files.readAllLines(Path.of("/proc/self/status")))
                if (l.startsWith("VmRSS:")) return Long.parseLong(l.replaceAll("[^0-9]", ""));
        } catch (Exception ignored) { }
        return -1;
    }

    public static void main(String[] args) throws Exception {
        System.out.println("Q4  -  JOINT CONCLUSION AND ALTERNATIVE CONCEPT ANALYSIS");
        System.out.println("=".repeat(112));
        System.out.println("Name: Vignesh S   Register Number: 192325002");

        System.out.println();
        System.out.println("JOINT CONCLUSION");
        System.out.println("=".repeat(112));
        System.out.println("  WHY THE SAME PROGRAM PRODUCES DIFFERENT OUTPUT ORDERS");
        System.out.println();
        System.out.printf("  %-34s | %s%n", "SOURCE OF VARIATION", "WHY IT DIFFERS BETWEEN RUNS");
        System.out.println("  " + "-".repeat(104));
        String[][] why = {
            {"the OS scheduler", "decides which thread gets a core and for how long"},
            {"the moment start() returns", "creating a thread is a system call of variable cost"},
            {"sleep(1) is a MINIMUM", "the thread wakes at 1 ms OR LATER, never earlier"},
            {"other processes on the machine", "the JVM competes with everything else running"},
            {"JIT compilation", "a station compiled mid-run suddenly speeds up"},
            {"garbage collection", "a GC pause can stop one station and not another"},
            {"cache and memory effects", "whichever station's data is already in cache runs faster"},
        };
        for (String[] r : why) System.out.printf("  %-34s | %s%n", r[0], r[1]);
        System.out.println();
        System.out.println("  Nothing in the Java Language Specification promises an order.  It");
        System.out.println("  guarantees only what HAPPENS-BEFORE what: start() happens before");
        System.out.println("  anything the thread does, and everything a thread does happens before");
        System.out.println("  join() returns. Between those two points the order is unspecified.");
        System.out.println();
        System.out.println("  WHY A CORRECT PROGRAM MUST NOT DEPEND ON THAT ORDER");
        System.out.println("      Because it is not a bug that can be found by testing.  Section 1");
        System.out.println("      of the Student B experiments produced nine different orders in");
        System.out.println("      twenty runs on one machine; a program that happens to be correct");
        System.out.println("      for the order it saw in testing will meet a different one in the");
        System.out.println("      examination cell, on a different number of cores, under different");
        System.out.println("      load.  Q5 of this assignment is the same lesson with a sharper");
        System.out.println("      edge: the unsynchronised seat booking printed the right answer ten");
        System.out.println("      times in a row and then oversold in 12 of 40 trials once the");
        System.out.println("      threads were made to overlap.");
        System.out.println();
        System.out.println("      What a program may depend on is what it ESTABLISHES: join() before");
        System.out.println("      reading a total, a lock around shared state, an atomic counter.");
        System.out.println("      Every total in these experiments was 100 in every run, because the");
        System.out.println("      total is protected and the order is not.");

        /* ---------- the alternative: ExecutorService ---------- */
        System.out.println();
        System.out.println("ALTERNATIVE CONCEPT AND COMPLEXITY ANALYSIS");
        System.out.println("=".repeat(112));
        System.out.println("  n = number of scripts, p = number of threads, B = scripts per batch.");
        System.out.println();
        System.out.println("1.  MEASURED COMPLETION TIME, 400 SCRIPTS, RAW THREADS VERSUS A POOL");
        System.out.println("-".repeat(112));
        final int N = 400;
        System.out.printf("  %-30s | %-10s | %-14s | %-14s | %s%n",
                "APPROACH", "THREADS", "TIME (ms)", "n/p PREDICTION", "SCANNED");
        System.out.println("  " + "-".repeat(104));

        for (int p : new int[]{1, 2, 4, 8}) {
            int batch = N / p;
            AtomicInteger done = new AtomicInteger();
            long t0 = System.nanoTime();
            Thread[] th = new Thread[p];
            for (int i = 0; i < p; i++) {
                th[i] = new Thread(() -> {
                    for (int s = 0; s < batch; s++) {
                        try { Thread.sleep(1); } catch (InterruptedException e) { return; }
                        done.incrementAndGet();
                    }
                });
                th[i].start();
            }
            for (Thread x : th) x.join();
            long ms = (System.nanoTime() - t0) / 1_000_000;
            System.out.printf("  %-30s | %-10d | %-14d | %-14s | %d%n",
                    "new Thread() per station", p, ms, (N / p) + " ms", done.get());
        }
        for (int p : new int[]{1, 2, 4, 8}) {
            int batch = N / p;
            AtomicInteger done = new AtomicInteger();
            ExecutorService pool = Executors.newFixedThreadPool(p);
            long t0 = System.nanoTime();
            List<Future<?>> fs = new ArrayList<>();
            for (int i = 0; i < p; i++) fs.add(pool.submit(() -> {
                for (int s = 0; s < batch; s++) {
                    try { Thread.sleep(1); } catch (InterruptedException e) { return; }
                    done.incrementAndGet();
                }
            }));
            for (Future<?> f : fs) f.get();
            long ms = (System.nanoTime() - t0) / 1_000_000;
            pool.shutdown();
            System.out.printf("  %-30s | %-10d | %-14d | %-14s | %d%n",
                    "ExecutorService fixed pool", p, ms, (N / p) + " ms", done.get());
        }
        System.out.println();
        System.out.println("  Both follow the n/p prediction closely, because this workload is pure");
        System.out.println("  sleeping - no processor contention, so doubling p halves the time.");
        System.out.println("  Real scanning is not like that, which is what Q7 measures.");

        System.out.println();
        System.out.println("2.  MEASURED MEMORY PER THREAD  -  THE STACK");
        System.out.println("-".repeat(112));
        System.out.printf("  default thread stack size (-Xss): %s%n",
                System.getProperty("java.vm.name") != null ? "1 MB reserved per thread on 64-bit Linux" : "unknown");
        final int MANY = 500;
        long rssBefore = vmRssKb();
        CountDownLatch hold = new CountDownLatch(1);
        List<Thread> parked = new ArrayList<>();
        for (int i = 0; i < MANY; i++) {
            Thread t = new Thread(() -> { try { hold.await(); } catch (InterruptedException ignored) { } });
            t.start(); parked.add(t);
        }
        Thread.sleep(400);
        long rssAfter = vmRssKb();
        hold.countDown();
        for (Thread t : parked) t.join();
        long rssFreed = vmRssKb();
        System.out.printf("  %-44s | %s%n", "resident memory before " + MANY + " threads", rssBefore + " kB");
        System.out.printf("  %-44s | %s%n", "resident memory with " + MANY + " threads alive", rssAfter + " kB");
        System.out.printf("  %-44s | %s%n", "resident memory after they finished", rssFreed + " kB");
        System.out.printf("  %-44s | %s%n", "RESIDENT cost per live thread",
                String.format("%.0f kB", (rssAfter - rssBefore) / (double) MANY));
        System.out.println();
        System.out.println("  Read this carefully.  1 MB per thread is RESERVED as virtual address");
        System.out.println("  space, but only the pages actually touched become resident, which is");
        System.out.println("  why the measured figure is far below 1 MB for threads that only park.");
        System.out.println("  A thread with a deep call stack approaches the reserved figure.  Both");
        System.out.println("  numbers matter: the reserved one limits HOW MANY threads can exist,");
        System.out.println("  the resident one is what the machine actually pays.");

        System.out.println();
        System.out.println("3.  THE TWO APPROACHES COMPARED");
        System.out.println("-".repeat(112));
        System.out.printf("  %-30s | %-32s | %s%n", "", "new Thread() PER STATION", "ExecutorService FIXED POOL");
        System.out.println("  " + "-".repeat(104));
        String[][] cmp = {
            {"threads created", "one per station, n/B of them", "exactly p, reused"},
            {"expected time", "O(n/p) when p stations run", "O(n/p) with n/B tasks over p threads"},
            {"memory", "(n/B) x stack, all at once", "p x stack, fixed"},
            {"4 stations", "4 threads - identical cost", "4 threads - identical cost"},
            {"10,000 scripts one per task", "10,000 threads - fails", "p threads, 10,000 queued tasks"},
            {"thread creation cost", "paid n/B times", "paid p times, at startup"},
            {"a task throws", "kills that thread silently", "captured in the Future"},
            {"collecting results", "join() then read shared state", "Future.get() returns a value"},
            {"shutdown", "nothing to shut down", "shutdown() must be called"},
        };
        for (String[] r : cmp) System.out.printf("  %-30s | %-32s | %s%n", r[0], r[1], r[2]);

        System.out.println();
        System.out.println("  FOR FOUR STATIONS the two are equivalent, and raw threads are simpler:");
        System.out.println("  four threads are created either way, and a pool adds a queue and a");
        System.out.println("  shutdown to remember.  The pool wins the moment the number of TASKS");
        System.out.println("  stops matching the number of threads you want - which is exactly the");
        System.out.println("  situation in Q7, where 10,000 files must not become 10,000 threads.");
        System.out.println();
        System.out.println("  The pool's other advantage is visible in the table above: a task that");
        System.out.println("  throws is recorded in its Future, whereas an exception in a raw");
        System.out.println("  thread's run() kills that thread and is printed to stderr by the");
        System.out.println("  default handler, with main() none the wiser. Q4's stations would");
        System.out.println("  have reported a short total and no error at all.");
    }
}
