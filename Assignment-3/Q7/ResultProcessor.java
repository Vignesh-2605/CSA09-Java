/*
 * Programming in Java  -  Assignment 3 (CO3)
 * Q7  -  Design a Concurrent, Fault-Tolerant Processor from First Principles
 *
 * Name: Vignesh S   Register Number: 192325002
 *
 * Stage 2 (design) implemented here; Stage 3 (measurement) in ProcessorRunner.
 *
 * THE DESIGN, in one sentence: one Callable per file, submitted to a fixed
 * ExecutorService, each returning an immutable outcome; failures are VALUES
 * returned by the task, never exceptions that escape it, so no single bad
 * file can end the run.
 */
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

public class ResultProcessor {

    /* ---------------- the domain ---------------- */

    /** Why a file could not be used.  A closed set, so the summary is total. */
    public enum Failure { MISSING, EMPTY, MALFORMED_LINE, BAD_NUMBER, MARK_OUT_OF_RANGE, UNREADABLE }

    /**
     * The outcome of ONE file.  A record, so it is immutable and therefore
     * safe to hand between threads with no locking at all.
     */
    public record Outcome(String file, boolean ok, String register, int total,
                          String grade, Failure failure, String detail) {
        static Outcome success(String f, String reg, int total, String grade) {
            return new Outcome(f, true, reg, total, grade, null, null);
        }
        static Outcome failed(String f, Failure why, String detail) {
            return new Outcome(f, false, null, 0, null, why, detail);
        }
    }

    /** One file's worth of work.  Returns an Outcome; never throws. */
    public static class ScoreFileTask implements Callable<Outcome> {
        private final Path file;
        public ScoreFileTask(Path file) { this.file = file; }

        @Override public Outcome call() {
            String name = file.getFileName().toString();
            try {
                if (!Files.exists(file))
                    return Outcome.failed(name, Failure.MISSING, "no such file");

                List<String> lines = Files.readAllLines(file);
                if (lines.isEmpty())
                    return Outcome.failed(name, Failure.EMPTY, "file has no lines");

                String register = lines.get(0).trim();
                if (register.isEmpty())
                    return Outcome.failed(name, Failure.MALFORMED_LINE, "line 1: register number missing");

                int total = 0, subjects = 0;
                for (int i = 1; i < lines.size(); i++) {
                    String line = lines.get(i).trim();
                    if (line.isEmpty()) continue;
                    String[] parts = line.split(",");
                    if (parts.length != 2)
                        return Outcome.failed(name, Failure.MALFORMED_LINE,
                                "line " + (i + 1) + ": expected SUBJECT,MARK but found " + parts.length + " field(s)");
                    int mark;
                    try {
                        mark = Integer.parseInt(parts[1].trim());
                    } catch (NumberFormatException e) {
                        return Outcome.failed(name, Failure.BAD_NUMBER,
                                "line " + (i + 1) + ": \"" + parts[1].trim() + "\" is not a mark");
                    }
                    if (mark < 0 || mark > 100)
                        return Outcome.failed(name, Failure.MARK_OUT_OF_RANGE,
                                "line " + (i + 1) + ": mark " + mark + " outside 0..100");
                    total += mark; subjects++;
                }
                if (subjects == 0)
                    return Outcome.failed(name, Failure.MALFORMED_LINE, "no subject rows");

                return Outcome.success(name, register, total, grade(total / subjects));

            } catch (IOException e) {
                // An I/O fault is a FAILED FILE, not a failed run.
                return Outcome.failed(name, Failure.UNREADABLE, e.getClass().getSimpleName() + ": " + e.getMessage());
            } catch (RuntimeException e) {
                // A defect in OUR code must also not end the run, but it must
                // be distinguishable from a bad input file.
                return Outcome.failed(name, Failure.UNREADABLE,
                        "UNEXPECTED " + e.getClass().getName() + ": " + e.getMessage());
            }
        }

        static String grade(int average) {
            if (average >= 90) return "S";
            if (average >= 80) return "A";
            if (average >= 70) return "B";
            if (average >= 60) return "C";
            if (average >= 50) return "D";
            return "F";
        }
    }

    /** What the examination cell receives at the end of the run. */
    public record Summary(int filesSubmitted, int succeeded, int failed,
                          Map<Failure, Integer> failuresByKind,
                          Map<String, Integer> gradeCounts,
                          List<Outcome> failures,
                          long millis, int poolSize) {
        public boolean isComplete() { return filesSubmitted == succeeded + failed; }
    }

    /**
     * Processes every file with a fixed pool of the given size.
     *
     * The shared result store is a ConcurrentHashMap keyed by file name.
     * Justification: every task writes exactly one entry under its own unique
     * key, so writes never contend for the same key; the map's striped
     * locking means p threads inserting p different keys do not serialise,
     * which a synchronized HashMap would. Reads happen only after the run.
     */
    public static Summary process(List<Path> files, int poolSize) throws InterruptedException {
        ConcurrentMap<String, Outcome> results = new ConcurrentHashMap<>();
        ExecutorService pool = Executors.newFixedThreadPool(poolSize);
        long t0 = System.nanoTime();
        try {
            List<Future<Outcome>> futures = new ArrayList<>(files.size());
            for (Path f : files) futures.add(pool.submit(new ScoreFileTask(f)));

            for (int i = 0; i < futures.size(); i++) {
                String name = files.get(i).getFileName().toString();
                try {
                    Outcome o = futures.get(i).get();          // blocks until that task is done
                    results.putIfAbsent(o.file(), o);
                } catch (ExecutionException e) {
                    // The task threw despite the belt and braces above.  The
                    // run still completes, and the file is accounted for.
                    results.putIfAbsent(name, Outcome.failed(name, Failure.UNREADABLE,
                            "task threw: " + e.getCause()));
                } catch (CancellationException e) {
                    results.putIfAbsent(name, Outcome.failed(name, Failure.UNREADABLE, "task cancelled"));
                }
            }
        } finally {
            pool.shutdown();
            if (!pool.awaitTermination(60, TimeUnit.SECONDS)) pool.shutdownNow();
        }
        long millis = (System.nanoTime() - t0) / 1_000_000;

        Map<Failure, Integer> byKind = new EnumMap<>(Failure.class);
        Map<String, Integer> grades = new TreeMap<>();
        List<Outcome> failures = new ArrayList<>();
        int ok = 0, bad = 0;
        for (Path f : files) {
            Outcome o = results.get(f.getFileName().toString());
            if (o == null) {        // must never happen; counted rather than ignored
                bad++;
                failures.add(Outcome.failed(f.getFileName().toString(), Failure.UNREADABLE, "NO OUTCOME RECORDED"));
                continue;
            }
            if (o.ok()) { ok++; grades.merge(o.grade(), 1, Integer::sum); }
            else { bad++; byKind.merge(o.failure(), 1, Integer::sum); failures.add(o); }
        }
        return new Summary(files.size(), ok, bad, byKind, grades, failures, millis, poolSize);
    }
}
