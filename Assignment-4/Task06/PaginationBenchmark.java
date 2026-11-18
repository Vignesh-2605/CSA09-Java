/*
 * Programming in Java  -  Assignment 4 (CO4)
 * Task 6  -  loading ALL registrations versus paginated loading.
 *
 * Measures the three things the task asks for - loading time, memory, and
 * window responsiveness - and measures responsiveness properly, by timing how
 * long the Event Dispatch Thread takes to answer while each strategy runs.
 *
 * Name: Vignesh S   Register Number: 192325002
 */
import bench.Bench;
import java.util.*;
import java.util.concurrent.*;
import javax.swing.*;

public class PaginationBenchmark {

    static final int EVENT_ID = 3;              // Cultural Night, capacity 2000 (raised for the test)
    static final int ROWS = 100_000;
    static final int PAGE = 25;

    static long usedHeapKb() {
        Runtime r = Runtime.getRuntime();
        return (r.totalMemory() - r.freeMemory()) / 1024;
    }

    public static void main(String[] args) throws Exception {
        System.out.println("TASK 6  -  LOAD ALL VERSUS PAGINATED, AT 100,000 REGISTRATIONS");
        System.out.println("=".repeat(112));
        System.out.println("Name: Vignesh S   Register Number: 192325002");
        System.out.printf("JVM %s, max heap %d MB%n", System.getProperty("java.version"),
                Runtime.getRuntime().maxMemory() / (1024 * 1024));

        RegistrationDao dao = new RegistrationDao();
        RegistrationService svc = new RegistrationService(dao);

        System.out.println();
        System.out.println("1.  BUILDING THE TEST DATA");
        System.out.println("-".repeat(112));
        long t0 = System.nanoTime();
        int made = dao.bulkInsert(EVENT_ID, ROWS, 1000);
        System.out.printf("  inserted %,d registrations in %,d ms (batches of 1,000, one transaction)%n",
                made, (System.nanoTime() - t0) / 1_000_000);
        System.out.printf("  rows now visible to the application: %,d%n", svc.count(EVENT_ID));

        /* ---------- 2. loading time ---------- */
        System.out.println();
        System.out.println("2.  TIME TO PUT THE FIRST SCREEN IN FRONT OF THE USER");
        System.out.println("-".repeat(112));
        List<Bench.Result> results = new ArrayList<>();
        final List<?>[] sink = new List<?>[1];

        Bench.Result all = Bench.measure("load all", "first screen", ROWS, 1,
                () -> { sink[0] = svc.all(EVENT_ID); });
        Bench.Result one = Bench.measure("paginated", "first screen", ROWS, 1,
                () -> { sink[0] = svc.page(EVENT_ID, 0, PAGE); });
        results.add(all); results.add(one);

        System.out.printf("  %-16s | %-14s | %-10s | %-16s | %s%n",
                "STRATEGY", "MEDIAN (ms)", "RSD %", "ROWS FETCHED", "ROWS THE USER SEES");
        System.out.println("  " + "-".repeat(104));
        System.out.printf("  %-16s | %-14.1f | %-10.1f | %-16s | %s%n",
                "load all", all.medianMs(), all.rsd(), String.format("%,d", ROWS), PAGE);
        System.out.printf("  %-16s | %-14.1f | %-10.1f | %-16s | %s%n",
                "paginated", one.medianMs(), one.rsd(), String.valueOf(PAGE), PAGE);
        System.out.printf("  paginated is %.0fx faster to first screen%n", all.median() / one.median());

        /* ---------- 3. memory ---------- */
        System.out.println();
        System.out.println("3.  MEMORY HELD WHILE THE SCREEN IS SHOWING");
        System.out.println("-".repeat(112));
        System.gc(); Thread.sleep(200);
        long before = usedHeapKb();
        List<Model.Registration> allRows = svc.all(EVENT_ID);
        long afterAll = usedHeapKb();
        int kept = allRows.size();
        allRows = null; System.gc(); Thread.sleep(200);

        long before2 = usedHeapKb();
        List<Model.Registration> pageRows = svc.page(EVENT_ID, 0, PAGE);
        long afterPage = usedHeapKb();
        int keptPage = pageRows.size();
        pageRows = null;

        System.out.printf("  %-16s | %-18s | %-18s | %s%n", "STRATEGY", "HEAP BEFORE", "HEAP AFTER", "HELD FOR THE SCREEN");
        System.out.println("  " + "-".repeat(104));
        System.out.printf("  %-16s | %-18s | %-18s | %,d kB for %,d rows%n", "load all",
                String.format("%,d kB", before), String.format("%,d kB", afterAll), afterAll - before, kept);
        System.out.printf("  %-16s | %-18s | %-18s | %,d kB for %,d rows%n", "paginated",
                String.format("%,d kB", before2), String.format("%,d kB", afterPage), afterPage - before2, keptPage);
        System.out.printf("  bytes per registration (load all): about %,d%n",
                kept == 0 ? 0 : (afterAll - before) * 1024 / kept);

        /* ---------- 4. responsiveness ---------- */
        System.out.println();
        System.out.println("4.  WINDOW RESPONSIVENESS  -  HOW LONG THE EDT TAKES TO ANSWER");
        System.out.println("-".repeat(112));
        System.out.println("  A probe is posted to the Event Dispatch Thread every 20 ms while each");
        System.out.println("  strategy loads.  The number recorded is how long the EDT took to run");
        System.out.println("  that probe - which is exactly how long a click or a repaint would have");
        System.out.println("  waited.  Anything over about 100 ms is visible to a user as a freeze.");
        System.out.println();
        System.out.printf("  %-44s | %-14s | %-14s | %s%n",
                "HOW THE LOAD IS RUN", "WORST (ms)", "MEDIAN (ms)", "USER EXPERIENCE");
        System.out.println("  " + "-".repeat(104));
        report("load all, ON the EDT (the mistake)", probe(() -> {
            try { SwingUtilities.invokeAndWait(() -> {
                try { svc.all(EVENT_ID); } catch (Exception e) { throw new RuntimeException(e); } });
            } catch (Exception e) { throw new RuntimeException(e); }
        }));
        report("load all, in a SwingWorker", probe(() -> {
            try { runWorker(() -> svc.all(EVENT_ID)); } catch (Exception e) { throw new RuntimeException(e); }
        }));
        report("paginated, in a SwingWorker", probe(() -> {
            try { runWorker(() -> svc.page(EVENT_ID, 0, PAGE)); } catch (Exception e) { throw new RuntimeException(e); }
        }));

        Bench.writeCsv("Task06", results);

        System.out.println();
        System.out.println("5.  WHAT THIS MEANS FOR THE DESIGN");
        System.out.println("-".repeat(112));
        System.out.println("  The table shows TWO independent decisions, and both have to be right.");
        System.out.println();
        System.out.println("  Running the load on the EDT freezes the window for the whole fetch -");
        System.out.println("  the window cannot repaint, cannot be dragged and cannot be closed.");
        System.out.println("  Moving the SAME load into a SwingWorker fixes the freeze without");
        System.out.println("  making the load any faster: the user can still see and move the window");
        System.out.println("  while it runs.");
        System.out.println();
        System.out.println("  Pagination then fixes the load itself.  The user is shown 25 rows, so");
        System.out.println("  fetching 100,000 to display 25 is wasted work no matter which thread");
        System.out.println("  does it, and the wasted memory is held for as long as the screen is");
        System.out.println("  open.  This is why EventRegistrationApp uses LIMIT/OFFSET for the");
        System.out.println("  table and still fetches in a SwingWorker - and why the CSV export,");
        System.out.println("  which genuinely does need every row, walks the data a page at a time");
        System.out.println("  instead of holding the whole result set.");
        System.exit(0);
    }

    interface Load { Object get() throws Exception; }
    static void runWorker(Load l) throws Exception {
        SwingWorker<Object, Void> w = new SwingWorker<>() {
            @Override protected Object doInBackground() throws Exception { return l.get(); }
        };
        w.execute(); w.get();
    }

    /** Posts probes to the EDT while `work` runs; returns the observed latencies. */
    static List<Double> probe(Runnable work) throws Exception {
        List<Double> lat = Collections.synchronizedList(new ArrayList<>());
        final boolean[] stop = {false};
        Thread prober = new Thread(() -> {
            while (!stop[0]) {
                long sent = System.nanoTime();
                try {
                    SwingUtilities.invokeAndWait(() -> lat.add((System.nanoTime() - sent) / 1e6));
                    Thread.sleep(20);
                } catch (Exception e) { return; }
            }
        });
        prober.start();
        Thread.sleep(60);
        work.run();
        stop[0] = true; prober.join(3000);
        return lat;
    }

    static void report(String label, List<Double> lat) {
        if (lat.isEmpty()) { System.out.printf("  %-44s | no probes%n", label); return; }
        List<Double> s = new ArrayList<>(lat); Collections.sort(s);
        double worst = s.get(s.size() - 1), median = s.get(s.size() / 2);
        String verdict = worst > 500 ? "FROZEN - visibly locked up"
                       : worst > 100 ? "stutters" : "stays responsive";
        System.out.printf("  %-44s | %-14.1f | %-14.1f | %s%n", label, worst, median, verdict);
    }
}
