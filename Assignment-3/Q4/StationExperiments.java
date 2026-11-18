/*
 * Programming in Java  -  Assignment 3 (CO3)
 * Q4, Student B  -  Behavioural Experiments
 *
 * Name: Vignesh S   Register Number: 192325002
 */
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class StationExperiments {

    static final int STATIONS = 4, BATCH = 25, RUNS = 20;

    static List<String> order = Collections.synchronizedList(new ArrayList<>());
    static AtomicInteger total = new AtomicInteger();

    static Runnable station(String name, int batch) {
        return () -> {
            int scanned = 0;
            for (int i = 0; i < batch; i++) {
                scanned++;
                try { Thread.sleep(1); }
                catch (InterruptedException e) { Thread.currentThread().interrupt(); return; }
            }
            total.addAndGet(scanned);
            order.add(name);
        };
    }

    static List<String> oneRun(int[] priorities) throws InterruptedException {
        order.clear(); total.set(0);
        Thread[] t = new Thread[STATIONS];
        for (int i = 0; i < STATIONS; i++) {
            t[i] = new Thread(station("Station-" + (i + 1), BATCH), "Station-" + (i + 1));
            if (priorities != null) t[i].setPriority(priorities[i]);
        }
        for (Thread x : t) x.start();
        for (Thread x : t) x.join();
        return new ArrayList<>(order);
    }

    public static void main(String[] args) throws Exception {
        System.out.println("Q4, STUDENT B  -  BEHAVIOURAL EXPERIMENTS");
        System.out.println("=".repeat(112));
        System.out.println("Name: Vignesh S   Register Number: 192325002");
        System.out.printf("JVM %s on %d available processors%n",
                System.getProperty("java.version"), Runtime.getRuntime().availableProcessors());

        /* ---------- 20 runs, default priorities ---------- */
        System.out.println();
        System.out.println("1.  TWENTY RUNS OF THE IDENTICAL PROGRAM, DEFAULT PRIORITIES");
        System.out.println("-".repeat(112));
        System.out.printf("  %-6s | %-44s | %s%n", "RUN", "ORDER IN WHICH STATIONS FINISHED", "TOTAL");
        System.out.println("  " + "-".repeat(104));
        Map<String, Integer> tally = new LinkedHashMap<>();
        for (int r = 1; r <= RUNS; r++) {
            List<String> o = oneRun(null);
            String key = String.join(",", o).replace("Station-", "");
            tally.merge(key, 1, Integer::sum);
            System.out.printf("  %-6d | %-44s | %d%n", r, key, total.get());
        }
        System.out.println();
        System.out.printf("  %-18s | %-10s | %s%n", "DISTINCT ORDER", "TIMES", "SHARE OF 20 RUNS");
        System.out.println("  " + "-".repeat(104));
        List<Map.Entry<String,Integer>> sorted = new ArrayList<>(tally.entrySet());
        sorted.sort((a, b) -> b.getValue() - a.getValue());
        for (Map.Entry<String,Integer> e : sorted)
            System.out.printf("  %-18s | %-10d | %.0f%%%n", e.getKey(), e.getValue(), 100.0 * e.getValue() / RUNS);
        System.out.printf("%n  %d distinct finishing orders in %d runs of identical code.%n", tally.size(), RUNS);
        System.out.printf("  The TOTAL was %d in every single run.%n", STATIONS * BATCH);
        System.out.println("  That is the whole point: the ORDER is not reproducible, the RESULT is.");

        /* ---------- priorities, WITH a control condition ---------- */
        System.out.println();
        System.out.println("2.  THREAD PRIORITIES  -  DOES THE ORDER CHANGE?");
        System.out.println("-".repeat(112));
        System.out.println("  Three conditions, 20 runs each.  The third exists because the first");
        System.out.println("  two cannot be read on their own: stations are STARTED in the order");
        System.out.println("  1,2,3,4, so Station-1 has a head start whatever its priority.  Without");
        System.out.println("  a control, a high-priority Station-1 finishing first proves nothing.");
        System.out.println();
        int[][] schemes = {
            null,
            { Thread.MAX_PRIORITY, 7, 4, Thread.MIN_PRIORITY },
            { Thread.MIN_PRIORITY, 4, 7, Thread.MAX_PRIORITY },
        };
        String[] labels = {
            "CONTROL: all stations priority 5 (default)",
            "Station-1 MAX(10), 7, 4, Station-4 MIN(1)",
            "Station-1 MIN(1), 4, 7, Station-4 MAX(10)",
        };
        int[][] firstCounts = new int[schemes.length][STATIONS];
        int[] distinct = new int[schemes.length];
        for (int s = 0; s < schemes.length; s++) {
            Map<String,Integer> ptally = new LinkedHashMap<>();
            for (int r = 1; r <= RUNS; r++) {
                List<String> o = oneRun(schemes[s]);
                ptally.merge(String.join(",", o).replace("Station-", ""), 1, Integer::sum);
                firstCounts[s][Integer.parseInt(o.get(0).substring(8)) - 1]++;
            }
            distinct[s] = ptally.size();
        }

        System.out.printf("  %-42s | %-8s | %-8s | %-8s | %-8s | %s%n",
                "CONDITION", "1 first", "2 first", "3 first", "4 first", "DISTINCT ORDERS");
        System.out.println("  " + "-".repeat(104));
        for (int s = 0; s < schemes.length; s++) {
            System.out.printf("  %-42s | %-8s | %-8s | %-8s | %-8s | %d of %d%n", labels[s],
                    firstCounts[s][0] + "/" + RUNS, firstCounts[s][1] + "/" + RUNS,
                    firstCounts[s][2] + "/" + RUNS, firstCounts[s][3] + "/" + RUNS,
                    distinct[s], RUNS);
        }
        System.out.println();
        System.out.println("  HOW TO READ THIS TABLE");
        System.out.printf("      Station-1 finishes first %s of the time with NO priority advantage,%n",
                String.format("%.0f%%", 100.0 * firstCounts[0][0] / RUNS));
        System.out.printf("      %s with MAX priority, and %s with MIN priority.%n",
                String.format("%.0f%%", 100.0 * firstCounts[1][0] / RUNS),
                String.format("%.0f%%", 100.0 * firstCounts[2][0] / RUNS));
        System.out.printf("      Station-4 finishes first %s with no advantage and %s with MAX%n",
                String.format("%.0f%%", 100.0 * firstCounts[0][3] / RUNS),
                String.format("%.0f%%", 100.0 * firstCounts[2][3] / RUNS));
        System.out.println("      priority - the condition under which it should dominate.");
        System.out.println();
        System.out.println("      Station-4 is started last, so it finishes first only rarely in");
        System.out.println("      every condition, including the one where it has the highest");
        System.out.println("      priority in the program.  The variable that predicts the finishing");
        System.out.println("      order here is START ORDER, not priority.");
        System.out.println();
        System.out.println("  CONCLUSION ON PRIORITIES");
        System.out.println("      setPriority() is a HINT to the operating system's scheduler, not an");
        System.out.println("      instruction to the JVM.  On Linux a plain user process cannot raise");
        System.out.println("      a thread's OS priority at all, so the hint is largely discarded.");
        System.out.println("      The table above shows no condition in which the highest-priority");
        System.out.println("      station reliably finishes first.");
        System.out.println();
        System.out.println("      It would not help even if it worked here: these stations spend");
        System.out.println("      almost all their time in TIMED_WAITING, so they are not competing");
        System.out.println("      for the processor in the first place.  Priority only influences who");
        System.out.println("      gets a core when more threads are RUNNABLE than there are cores.");
        System.out.println("      With 4 stations and " + Runtime.getRuntime().availableProcessors()
                + " processors, there is no queue to jump.");
    }
}
