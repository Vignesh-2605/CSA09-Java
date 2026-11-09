package bench;

/**
 * Programming in Java - Assignment 2 (CO2)
 * Name: Vignesh S   Register Number: 192325002
 *
 * The measurement harness used by every question in this assignment.
 *
 * The brief states that "repeated, averaged measurements and operation counts
 * should be treated as the primary experimental evidence" and that "a single
 * execution time ... is not sufficient".  This class is what makes that true
 * for every figure reported:
 *
 *   - a warm-up phase runs the workload before anything is recorded, so the
 *     JIT has compiled it and the first measured run is not timing the
 *     interpreter;
 *   - the workload is then repeated REPS times and every run is kept;
 *   - the report quotes the MEDIAN rather than the mean, because one run
 *     interrupted by a garbage collection skews a mean and not a median;
 *   - the standard deviation and the min/max are reported alongside, so a
 *     noisy measurement is visible as noise instead of being hidden by an
 *     average;
 *   - every individual run is written to CSV, so the raw data behind each
 *     table can be re-analysed independently.
 */
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class Bench {

    public static final int WARMUP = 5;
    public static final int REPS   = 11;        // odd, so the median is a real run

    private Bench() { }

    /** One measured workload: its name, the size it ran at, and every run. */
    public static final class Result {
        public final String design, operation;
        public final long size;
        public final double[] nanos;            // one entry per repetition
        public final long opsPerRun;            // operations performed per run
        public long objectsCreated = -1;        // -1 when not counted
        public long bytesAllocated = -1;

        Result(String design, String operation, long size, double[] nanos, long opsPerRun) {
            this.design = design; this.operation = operation; this.size = size;
            this.nanos = nanos; this.opsPerRun = opsPerRun;
        }
        public double median()  { double[] c = nanos.clone(); Arrays.sort(c); return c[c.length / 2]; }
        public double mean()    { double s = 0; for (double v : nanos) s += v; return s / nanos.length; }
        public double stdev()   {
            double m = mean(), s = 0;
            for (double v : nanos) s += (v - m) * (v - m);
            return Math.sqrt(s / nanos.length);
        }
        public double min()     { double m = Double.MAX_VALUE; for (double v : nanos) m = Math.min(m, v); return m; }
        public double max()     { double m = 0; for (double v : nanos) m = Math.max(m, v); return m; }
        /** Relative standard deviation: how noisy this measurement was, as a percentage. */
        public double rsd()     { return mean() == 0 ? 0 : 100.0 * stdev() / mean(); }
        /** Median cost of ONE operation, in nanoseconds. */
        public double perOp()   { return opsPerRun == 0 ? 0 : median() / opsPerRun; }
        public double medianMs(){ return median() / 1e6; }
    }

    public interface Workload { void run() throws Exception; }

    /** Warms up, then times the workload REPS times and returns every run. */
    public static Result measure(String design, String operation, long size,
                                 long opsPerRun, Workload w) {
        try {
            for (int i = 0; i < WARMUP; i++) w.run();
            double[] runs = new double[REPS];
            for (int i = 0; i < REPS; i++) {
                long t0 = System.nanoTime();
                w.run();
                runs[i] = System.nanoTime() - t0;
            }
            return new Result(design, operation, size, runs, opsPerRun);
        } catch (Exception e) {
            throw new RuntimeException("workload '" + design + "/" + operation + "' failed", e);
        }
    }

    /** Used-heap bytes after a best-effort collection. Indicative, not exact. */
    public static long usedHeap() {
        Runtime rt = Runtime.getRuntime();
        for (int i = 0; i < 4; i++) {
            System.gc();
            try { Thread.sleep(25); } catch (InterruptedException ignored) { }
        }
        return rt.totalMemory() - rt.freeMemory();
    }

    /* ---------------- reporting ---------------- */

    public static void header(String... cols) {
        StringBuilder sb = new StringBuilder();
        for (String c : cols) sb.append(c);
        System.out.println(sb);
    }

    public static void tableHead() {
        System.out.printf("%-26s | %-22s | %-10s | %-12s | %-12s | %-9s | %s%n",
                "DESIGN", "OPERATION", "SIZE", "MEDIAN (ms)", "PER OP (ns)", "RSD %", "MIN..MAX (ms)");
        System.out.println("-".repeat(118));
    }

    public static void row(Result r) {
        System.out.printf("%-26s | %-22s | %-10s | %-12.3f | %-12.2f | %-9.1f | %.3f..%.3f%n",
                r.design, r.operation, fmt(r.size), r.medianMs(), r.perOp(), r.rsd(),
                r.min() / 1e6, r.max() / 1e6);
    }

    public static String fmt(long n) { return String.format("%,d", n); }

    /** Appends every individual repetition to the shared raw-data CSV. */
    public static void writeCsv(String question, List<Result> results) {
        Path dir = Paths.get("..", "data");
        try {
            Files.createDirectories(dir);
            Path f = dir.resolve(question + "-raw.csv");
            try (PrintWriter out = new PrintWriter(Files.newBufferedWriter(f))) {
                out.println("question,design,operation,size,repetition,nanos,ops_per_run,ns_per_op");
                for (Result r : results)
                    for (int i = 0; i < r.nanos.length; i++)
                        out.printf("%s,%s,%s,%d,%d,%.0f,%d,%.4f%n",
                                question, r.design, r.operation, r.size, i + 1, r.nanos[i],
                                r.opsPerRun, r.opsPerRun == 0 ? 0 : r.nanos[i] / r.opsPerRun);
            }
            Path s = dir.resolve(question + "-summary.csv");
            try (PrintWriter out = new PrintWriter(Files.newBufferedWriter(s))) {
                out.println("question,design,operation,size,median_ns,mean_ns,stdev_ns,rsd_pct,min_ns,max_ns,ops_per_run,ns_per_op,objects,bytes");
                for (Result r : results)
                    out.printf("%s,%s,%s,%d,%.0f,%.0f,%.0f,%.2f,%.0f,%.0f,%d,%.4f,%d,%d%n",
                            question, r.design, r.operation, r.size, r.median(), r.mean(), r.stdev(),
                            r.rsd(), r.min(), r.max(), r.opsPerRun, r.perOp(),
                            r.objectsCreated, r.bytesAllocated);
            }
            System.out.println();
            System.out.println("raw data   -> data/" + question + "-raw.csv  ("
                    + results.stream().mapToInt(x -> x.nanos.length).sum() + " individual runs)");
            System.out.println("summary    -> data/" + question + "-summary.csv");
        } catch (IOException e) {
            System.out.println("could not write CSV: " + e.getMessage());
        }
    }

    public static List<Result> list() { return new ArrayList<>(); }
}
