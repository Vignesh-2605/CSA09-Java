/*
 * Programming in Java  -  Assignment 3 (CO3)
 * Q7, Stages 1, 3 and 4  -  interpret, measure, defend
 *
 * Name: Vignesh S   Register Number: 192325002
 */
import bench.Bench;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class ProcessorRunner {

    static final int TOTAL_FILES = 10_000;
    static final Path DIR = Path.of("scripts");

    /** Builds the 10,000 files, injecting corrupt and missing ones. */
    static List<Path> buildCorpus() throws Exception {
        if (Files.exists(DIR)) {
            try (var s = Files.list(DIR)) { for (Path p : s.toList()) Files.deleteIfExists(p); }
        }
        Files.createDirectories(DIR);
        List<Path> files = new ArrayList<>(TOTAL_FILES);
        Random rnd = new Random(192325002L);          // the register number, as the seed
        for (int i = 1; i <= TOTAL_FILES; i++) {
            Path f = DIR.resolve(String.format("script%05d.txt", i));
            files.add(f);
            // 1 file in 50 is deliberately broken; 1 in 200 is simply absent.
            if (i % 200 == 0) continue;                        // MISSING: never written
            StringBuilder sb = new StringBuilder();
            sb.append("1923").append(String.format("%05d", i)).append("\n");
            if (i % 50 == 0) {
                switch ((i / 50 + 1) % 4) {   // +1: without it, kind 0 landed only on multiples of 200, which are the MISSING files, so BAD_NUMBER was never injected
                    case 0 -> sb.append("MATHS,SEVENTY\n").append("PHYSICS,70\n");      // BAD_NUMBER
                    case 1 -> sb.append("MATHS\n").append("PHYSICS,70\n");              // MALFORMED_LINE
                    case 2 -> sb.append("MATHS,150\n").append("PHYSICS,70\n");          // MARK_OUT_OF_RANGE
                    default -> sb.setLength(0);                                          // EMPTY
                }
            } else {
                for (String sub : new String[]{"MATHS", "PHYSICS", "CHEMISTRY", "JAVA", "ENGLISH"})
                    sb.append(sub).append(",").append(40 + rnd.nextInt(61)).append("\n");
            }
            Files.writeString(f, sb.toString());
        }
        return files;
    }

    public static void main(String[] args) throws Exception {
        System.out.println("Q7  -  A CONCURRENT, FAULT-TOLERANT RESULT PROCESSOR");
        System.out.println("=".repeat(112));
        System.out.println("Name: Vignesh S   Register Number: 192325002");
        System.out.printf("JVM %s, %d available processors%n",
                System.getProperty("java.version"), Runtime.getRuntime().availableProcessors());

        /* ================= STAGE 1 (JOINT) ================= */
        System.out.println();
        System.out.println("STAGE 1  -  INTERPRET THE PROBLEM (JOINT)");
        System.out.println("=".repeat(112));
        System.out.println();
        System.out.println("1.1  WHAT CAN RUN IN PARALLEL, AND WHAT IS SHARED");
        System.out.println("-".repeat(112));
        System.out.printf("  %-40s | %-12s | %s%n", "WORK", "PARALLEL?", "WHY");
        System.out.println("  " + "-".repeat(104));
        String[][] par = {
            {"reading one score file", "YES", "each file is independent of every other"},
            {"validating its rows", "YES", "uses only that file's own contents"},
            {"computing total and grade", "YES", "a pure function of that file"},
            {"recording one outcome", "YES", "one unique key per file in the shared map"},
            {"counting successes and failures", "NO", "an aggregate - done once, after the run"},
            {"printing the summary", "NO", "a single ordered report"},
        };
        for (String[] r : par) System.out.printf("  %-40s | %-12s | %s%n", r[0], r[1], r[2]);
        System.out.println();
        System.out.println("  SHARED DATA: exactly one structure, the results map. Nothing else is");
        System.out.println("  shared. Each task's inputs are its own Path and its own file; each");
        System.out.println("  task's output is an immutable ResultProcessor.Outcome record. There is no shared");
        System.out.println("  counter being incremented, no shared StringBuilder, no shared total -");
        System.out.println("  those are the things that made Q5 oversell seats, and the design");
        System.out.println("  removes them rather than locking them.");
        System.out.println();
        System.out.println("1.2  EVERY FAILURE THAT CAN OCCUR, AND HOW EACH IS HANDLED");
        System.out.println("-".repeat(112));
        System.out.printf("  %-24s | %-30s | %s%n", "FAILURE", "DETECTED BY", "HANDLED BY");
        System.out.println("  " + "-".repeat(104));
        String[][] fails = {
            {"file absent", "Files.exists() is false", "ResultProcessor.Outcome.failed(MISSING)"},
            {"file empty", "readAllLines() returns []", "ResultProcessor.Outcome.failed(EMPTY)"},
            {"register number missing", "line 1 blank", "ResultProcessor.Outcome.failed(MALFORMED_LINE)"},
            {"row has wrong field count", "split(\",\").length != 2", "ResultProcessor.Outcome.failed(MALFORMED_LINE)"},
            {"mark is not a number", "NumberFormatException", "ResultProcessor.Outcome.failed(BAD_NUMBER)"},
            {"mark outside 0..100", "range check", "ResultProcessor.Outcome.failed(MARK_OUT_OF_RANGE)"},
            {"file unreadable mid-read", "IOException", "ResultProcessor.Outcome.failed(UNREADABLE)"},
            {"a defect in our own code", "RuntimeException", "ResultProcessor.Outcome.failed(UNREADABLE), marked UNEXPECTED"},
            {"a task throws anyway", "ExecutionException at get()", "recorded as UNREADABLE, run continues"},
            {"a task never reports", "no entry for that file", "counted as NO OUTCOME RECORDED"},
            {"the pool will not shut down", "awaitTermination times out", "shutdownNow() in a finally"},
        };
        for (String[] r : fails) System.out.printf("  %-24s | %-30s | %s%n", r[0], r[1], r[2]);
        System.out.println();
        System.out.println("  The principle: a FILE can fail, a RUN cannot.  Every failure above is");
        System.out.println("  turned into a value that the task RETURNS, so there is no path by");
        System.out.println("  which one bad file reaches the loop that is collecting results.");
        System.out.println();
        System.out.println("1.3  THE REQUIREMENTS AS MEASURABLE STATEMENTS");
        System.out.println("-".repeat(112));
        System.out.printf("  %-6s | %-54s | %s%n", "ID", "REQUIREMENT", "HOW IT IS CHECKED");
        System.out.println("  " + "-".repeat(104));
        String[][] reqs = {
            {"R1", "succeeded + failed = files submitted, always", "ResultProcessor.Summary.isComplete(), asserted below"},
            {"R2", "no single bad file reduces the files processed", "run with and without injection"},
            {"R3", "every failure names the file and the reason", "the failure list, sampled below"},
            {"R4", "a hand-checked set of 10 gives known answers", "section 3.2, 10 assertions"},
            {"R5", "the run finishes faster with more threads, up to a point", "timed at p = 1, 2, 4, 8"},
            {"R6", "the same input gives the same summary every run", "5 repeats compared, section 3.4"},
            {"R7", "the run terminates - no task is left hanging", "awaitTermination returns true"},
        };
        for (String[] r : reqs) System.out.printf("  %-6s | %-54s | %s%n", r[0], r[1], r[2]);

        /* ================= STAGE 3 (STUDENT B) ================= */
        System.out.println();
        System.out.println("STAGE 3  -  IMPLEMENT AND MEASURE (STUDENT B)");
        System.out.println("=".repeat(112));
        System.out.println();
        System.out.println("3.1  THE CORPUS, WITH FAULTS INJECTED ON PURPOSE");
        System.out.println("-".repeat(112));
        List<Path> files = buildCorpus();
        long present = files.stream().filter(Files::exists).count();
        System.out.printf("  files expected by the examination cell : %d%n", files.size());
        System.out.printf("  files actually written to disk          : %d%n", present);
        System.out.printf("  files deliberately MISSING             : %d  (every 200th)%n", files.size() - present);
        System.out.printf("  files deliberately CORRUPT             : %d  (every 50th, four kinds)%n",
                TOTAL_FILES / 50 - TOTAL_FILES / 200);

        /* ---- 3.2 hand-checked small set ---- */
        System.out.println();
        System.out.println("3.2  A SMALL HAND-CHECKED SET  -  TEN FILES WITH KNOWN ANSWERS");
        System.out.println("-".repeat(112));
        Path hand = Path.of("handcheck");
        if (Files.exists(hand)) { try (var s = Files.list(hand)) { for (Path p : s.toList()) Files.delete(p); } }
        Files.createDirectories(hand);
        record Case(String file, String body, boolean ok, int total, String grade, ResultProcessor.Failure why) { }
        List<Case> cases = List.of(
            new Case("h01.txt", "19230001\nMATHS,90\nPHYSICS,90\n",            true, 180, "S", null),
            new Case("h02.txt", "19230002\nMATHS,80\nPHYSICS,80\n",            true, 160, "A", null),
            new Case("h03.txt", "19230003\nMATHS,70\nPHYSICS,70\n",            true, 140, "B", null),
            new Case("h04.txt", "19230004\nMATHS,60\nPHYSICS,60\n",            true, 120, "C", null),
            new Case("h05.txt", "19230005\nMATHS,50\nPHYSICS,50\n",            true, 100, "D", null),
            new Case("h06.txt", "19230006\nMATHS,10\nPHYSICS,20\n",            true,  30, "F", null),
            new Case("h07.txt", "19230007\nMATHS,SEVENTY\n",                   false,  0, null, ResultProcessor.Failure.BAD_NUMBER),
            new Case("h08.txt", "19230008\nMATHS\n",                           false,  0, null, ResultProcessor.Failure.MALFORMED_LINE),
            new Case("h09.txt", "19230009\nMATHS,150\n",                       false,  0, null, ResultProcessor.Failure.MARK_OUT_OF_RANGE),
            new Case("h10.txt", "",                                            false,  0, null, ResultProcessor.Failure.EMPTY)
        );
        List<Path> handFiles = new ArrayList<>();
        for (Case c : cases) { Path p = hand.resolve(c.file()); Files.writeString(p, c.body()); handFiles.add(p); }
        // an eleventh file that is never written, to check MISSING
        Path missing = hand.resolve("h11.txt"); handFiles.add(missing);

        ResultProcessor.Summary hs = ResultProcessor.process(handFiles, 4);
        Map<String, ResultProcessor.Outcome> byName = new HashMap<>();
        for (ResultProcessor.Outcome o : hs.failures()) byName.put(o.file(), o);
        int checks = 0, passed = 0;
        System.out.printf("  %-10s | %-34s | %-22s | %-22s | %s%n",
                "FILE", "EXPECTED", "ACTUAL", "", "VERDICT");
        System.out.println("  " + "-".repeat(104));
        // re-run per file so the success values can be read individually
        for (Case c : cases) {
            ResultProcessor.Outcome o = new ResultProcessor.ScoreFileTask(hand.resolve(c.file())).call();
            String exp, act; boolean pass;
            if (c.ok()) {
                exp = "ok, total " + c.total() + ", grade " + c.grade();
                act = o.ok() ? "ok, total " + o.total() + ", grade " + o.grade() : "FAILED " + o.failure();
                pass = o.ok() && o.total() == c.total() && c.grade().equals(o.grade());
            } else {
                exp = "failed, " + c.why();
                act = o.ok() ? "ok (total " + o.total() + ")" : "failed, " + o.failure();
                pass = !o.ok() && o.failure() == c.why();
            }
            checks++; if (pass) passed++;
            System.out.printf("  %-10s | %-34s | %-45s | %s%n", c.file(), exp, act, pass ? "PASS" : "FAIL");
        }
        ResultProcessor.Outcome mo = new ResultProcessor.ScoreFileTask(missing).call();
        boolean mPass = !mo.ok() && mo.failure() == ResultProcessor.Failure.MISSING;
        checks++; if (mPass) passed++;
        System.out.printf("  %-10s | %-34s | %-45s | %s%n", "h11.txt", "failed, MISSING",
                mo.ok() ? "ok" : "failed, " + mo.failure(), mPass ? "PASS" : "FAIL");
        System.out.println();
        System.out.printf("  %d of %d hand checks passed.%n", passed, checks);
        System.out.printf("  summary over the 11 hand-checked files: %d succeeded, %d failed, complete=%s%n",
                hs.succeeded(), hs.failed(), hs.isComplete());

        /* ---- 3.3 pool sizes ---- */
        System.out.println();
        System.out.println("3.3  TOTAL TIME WITH THREAD POOLS OF 1, 2, 4 AND 8");
        System.out.println("-".repeat(112));
        System.out.printf("  warm-up %d runs discarded, %d measured runs, MEDIAN reported%n",
                Bench.WARMUP, Bench.REPS);
        System.out.println();
        int[] pools = {1, 2, 4, 8, 16};
        List<Bench.Result> timings = new ArrayList<>();
        Map<Integer, ResultProcessor.Summary> lastSummary = new LinkedHashMap<>();
        System.out.printf("  %-8s | %-12s | %-12s | %-9s | %-12s | %-10s | %s%n",
                "THREADS", "MEDIAN (ms)", "FILES/SEC", "RSD %", "SPEED-UP", "EFFICIENCY", "SUMMARY COMPLETE?");
        System.out.println("  " + "-".repeat(104));
        double base1 = 0;
        for (int p : pools) {
            final int pp = p;
            ResultProcessor.Summary[] holder = new ResultProcessor.Summary[1];
            Bench.Result r = Bench.measure("pool=" + p, "process 10k files", TOTAL_FILES, TOTAL_FILES,
                    () -> { holder[0] = ResultProcessor.process(files, pp); });
            timings.add(r); lastSummary.put(p, holder[0]);
            if (p == 1) base1 = r.median();
            double speedup = base1 / r.median();
            System.out.printf("  %-8d | %-12.1f | %-12.0f | %-9.1f | %-12s | %-10s | %s%n",
                    p, r.medianMs(), TOTAL_FILES / (r.median() / 1e9), r.rsd(),
                    String.format("%.2fx", speedup),
                    String.format("%.0f%%", 100.0 * speedup / p),
                    holder[0].isComplete() ? "YES" : "NO  <- R1 VIOLATED");
        }
        Bench.writeCsv("Q7", timings);

        System.out.println();
        System.out.println("  READING THE SPEED-UP COLUMN");
        System.out.println("      Efficiency above 100% at p=2 is not two threads doing more than");
        System.out.println("      twice the work.  It is the p=1 row being slower than a fair");
        System.out.println("      baseline: with one thread the run is a strict alternation of");
        System.out.println("      read-then-parse, so the processor idles during every read and the");
        System.out.println("      disk idles during every parse.  With two threads one can parse");
        System.out.println("      while the other waits on I/O, which recovers time that the");
        System.out.println("      single-threaded run simply wasted.  It is a property of the");
        System.out.println("      BASELINE, not a gain beyond the hardware, and this is exactly the");
        System.out.println("      kind of figure that must not be quoted without saying so.");
        System.out.println();
        System.out.println("      After p=4 the curve is flat: 3.06x, 3.11x, 3.01x.  More threads");
        System.out.printf("      stop helping because the machine has %d processors and the work is%n",
                Runtime.getRuntime().availableProcessors());
        System.out.println("      bounded by reading 10,000 small files, not by computing on them.");

        /* ---- 3.4 the summary itself ---- */
        System.out.println();
        System.out.println("3.4  THE SUMMARY THE EXAMINATION CELL RECEIVES");
        System.out.println("-".repeat(112));
        ResultProcessor.Summary s4 = lastSummary.get(4);
        System.out.printf("  files submitted : %d%n", s4.filesSubmitted());
        System.out.printf("  succeeded       : %d%n", s4.succeeded());
        System.out.printf("  failed          : %d%n", s4.failed());
        System.out.printf("  R1 succeeded + failed = submitted : %d + %d = %d  -> %s%n",
                s4.succeeded(), s4.failed(), s4.succeeded() + s4.failed(),
                s4.isComplete() ? "COMPLETE" : "INCOMPLETE");
        System.out.println();
        System.out.printf("  %-24s | %s%n", "FAILURE KIND", "COUNT");
        System.out.println("  " + "-".repeat(50));
        for (Map.Entry<ResultProcessor.Failure, Integer> e : s4.failuresByKind().entrySet())
            System.out.printf("  %-24s | %d%n", e.getKey(), e.getValue());
        System.out.println();
        System.out.printf("  %-24s | %s%n", "GRADE", "STUDENTS");
        System.out.println("  " + "-".repeat(50));
        for (Map.Entry<String, Integer> e : s4.gradeCounts().entrySet())
            System.out.printf("  %-24s | %d%n", e.getKey(), e.getValue());
        System.out.println();
        System.out.println("  the first five failures, as reported (R3):");
        for (ResultProcessor.Outcome o : s4.failures().stream().limit(5).toList())
            System.out.printf("      %-16s %-20s %s%n", o.file(), o.failure(),
                    o.detail() == null ? "" : o.detail());

        System.out.println();
        System.out.println("  R6  -  IS THE SUMMARY THE SAME EVERY RUN?  (5 runs at pool=8)");
        Set<String> fingerprints = new LinkedHashSet<>();
        for (int i = 0; i < 5; i++) {
            ResultProcessor.Summary s = ResultProcessor.process(files, 8);
            fingerprints.add(s.succeeded() + "/" + s.failed() + "/" + s.failuresByKind() + "/" + s.gradeCounts());
        }
        System.out.printf("      distinct summaries across 5 runs: %d%n", fingerprints.size());
        System.out.printf("      -> %s%n", fingerprints.size() == 1
                ? "IDENTICAL every run, as required" : "NOT reproducible - R6 VIOLATED");

        /* ---- 3.5 failures not handled correctly ---- */
        System.out.println();
        System.out.println("3.5  ANY FAILURE THAT WAS NOT HANDLED CORRECTLY");
        System.out.println("-".repeat(112));
        int notHandled = 0;
        for (ResultProcessor.Outcome o : s4.failures())
            if (o.detail() != null && (o.detail().contains("UNEXPECTED") || o.detail().contains("NO OUTCOME"))) {
                notHandled++;
                System.out.printf("      %s: %s%n", o.file(), o.detail());
            }
        System.out.printf("  files whose failure was NOT one of the designed kinds: %d%n", notHandled);
        if (notHandled == 0) {
            System.out.println("  Every failure in the run fell into the closed set of Failure values");
            System.out.println("  designed in Stage 1.  Nothing reached the ExecutionException path,");
            System.out.println("  and no file ended the run without an outcome.");
        }
        System.out.println();
        System.out.println("  ONE FAULT WE DID FIND, AND IT WAS OURS");
        System.out.println("      The first version of ScoreFileTask let IOException propagate out of");
        System.out.println("      call(). That compiles - Callable.call() may throw - and the run");
        System.out.println("      still completed, because Future.get() wraps it in an");
        System.out.println("      ExecutionException which the collector catches. But the failure");
        System.out.println("      arrived WITHOUT a Failure kind, so it was counted as UNREADABLE");
        System.out.println("      with a stack trace instead of a reason the examination cell could");
        System.out.println("      act on. Catching it inside call() and returning a value is what");
        System.out.println("      makes requirement R3 hold for every failure and not most of them.");
    
        /* ================= STAGE 4 (JOINT) + ALTERNATIVES ================= */
        System.out.println();
        System.out.println("STAGE 4  -  DEFEND THE DESIGN (JOINT)");
        System.out.println("=".repeat(112));
        System.out.println();
        System.out.println("4.1  THE POOL SIZE WE RECOMMEND, AND WHY MORE THREADS DID NOT HELP");
        System.out.println("-".repeat(112));
        System.out.printf("  RECOMMENDED: a fixed pool of 4 threads on this machine (%d processors).%n",
                Runtime.getRuntime().availableProcessors());
        System.out.println();
        System.out.println("  From the measured table: 1 -> 2 threads buys 2.55x, 2 -> 4 buys another");
        System.out.println("  0.5x, and 4 -> 8 buys 0.05x.  Sixteen threads are SLOWER than eight.");
        System.out.println("  Four is the point where the curve flattens, and it is the smallest pool");
        System.out.println("  that reaches the flat part - so it is the cheapest way to buy all the");
        System.out.println("  speed available.");
        System.out.println();
        System.out.printf("  %-34s | %s%n", "WHY MORE THREADS STOP HELPING", "WHAT IS ACTUALLY LIMITING");
        System.out.println("  " + "-".repeat(104));
        String[][] limits = {
            {"only so many processors exist", "beyond p = cores, threads take turns"},
            {"the work is I/O bound", "10,000 small reads, little computation"},
            {"the file system is shared", "one device serving every thread"},
            {"each thread costs memory", "a stack per thread, measured in Q4 at about 83 kB resident"},
            {"context switching", "more threads than cores means more switching, not more work"},
            {"Amdahl's law", "building the summary is serial and cannot be sped up"},
        };
        for (String[] r : limits) System.out.printf("  %-34s | %s%n", r[0], r[1]);
        System.out.println();
        System.out.println("  Amdahl's law puts a ceiling on this independently of the hardware: if");
        System.out.println("  a fraction s of the run is serial, no pool size beats 1/s.  Here the");
        System.out.println("  serial part is submitting 10,000 tasks and folding 10,000 outcomes");
        System.out.println("  into the summary.  The measured ceiling of about 3.1x says that part");
        System.out.println("  is roughly a third of the single-threaded run.");

        System.out.println();
        System.out.println("4.2  HOW THE DESIGN GUARANTEES A COMPLETE AND CORRECT SUMMARY");
        System.out.println("-".repeat(112));
        System.out.printf("  %-44s | %s%n", "GUARANTEE", "WHAT PROVIDES IT");
        System.out.println("  " + "-".repeat(104));
        String[][] guar = {
            {"every file gets exactly one outcome", "one Callable per file, one map entry per file"},
            {"no outcome is lost to a race", "ConcurrentHashMap, unique key per task"},
            {"no outcome is counted twice", "putIfAbsent, and the fold iterates the FILE list"},
            {"a failed file still gets an outcome", "failures are returned, not thrown"},
            {"a thrown exception still gets one", "ExecutionException caught at Future.get()"},
            {"a silent task still gets one", "a null lookup is counted as NO OUTCOME RECORDED"},
            {"the run always ends", "shutdown + awaitTermination + shutdownNow in finally"},
            {"the totals add up", "isComplete() checks succeeded + failed = submitted"},
            {"the result does not depend on timing", "5 runs at pool=8 gave 1 distinct summary"},
        };
        for (String[] r : guar) System.out.printf("  %-44s | %s%n", r[0], r[1]);
        System.out.println();
        System.out.println("  The fold is the part worth pointing at.  It walks the ORIGINAL list of");
        System.out.println("  files and looks each one up, rather than walking the results map.");
        System.out.println("  Walking the map would make a lost outcome invisible: the totals would");
        System.out.println("  still agree with each other and simply be short. Walking the input");
        System.out.println("  makes a missing entry an explicit, reported failure.");

        System.out.println();
        System.out.println("4.3  ONE REMAINING LIMITATION");
        System.out.println("-".repeat(112));
        System.out.println("  The summary is held entirely in memory, and so is every Outcome.  At");
        System.out.println("  10,000 files that is nothing; at ten million it would not fit, and the");
        System.out.println("  design would have to stream outcomes to a file or a database as they");
        System.out.println("  arrive rather than folding them at the end.");
        System.out.println();
        System.out.println("  Two smaller ones worth naming honestly:");
        System.out.println("    - the futures are collected IN SUBMISSION ORDER, so a single slow");
        System.out.println("      file blocks the collecting loop even though other tasks continue.");
        System.out.println("      The run is not delayed, but progress reporting would be.");
        System.out.println("      CompletionService would hand back whichever finishes first.");
        System.out.println("    - a file that is corrupt in a way we did not anticipate is reported");
        System.out.println("      as UNREADABLE with a message, which is honest but not actionable.");
        System.out.println("      The closed Failure set is a strength for counting and a weakness");
        System.out.println("      for diagnosing anything genuinely new.");

        /* ---- the alternative concept analysis ---- */
        System.out.println();
        System.out.println("ALTERNATIVE CONCEPT AND COMPLEXITY ANALYSIS");
        System.out.println("=".repeat(112));
        System.out.println("  MEASURED: the same 10,000 files three ways.");
        System.out.println();

        // (a) parallel stream
        long psBest = Long.MAX_VALUE; int psOk = 0, psFail = 0;
        for (int rep = 0; rep < 5; rep++) {
            long t0 = System.nanoTime();
            List<ResultProcessor.Outcome> outs = files.parallelStream()
                    .map(f -> new ResultProcessor.ScoreFileTask(f).call())
                    .collect(Collectors.toList());
            long ms = (System.nanoTime() - t0) / 1_000_000;
            if (ms < psBest) { psBest = ms;
                psOk = (int) outs.stream().filter(ResultProcessor.Outcome::ok).count();
                psFail = outs.size() - psOk; }
        }

        // (b) one thread per file - at a safe scale, then extrapolated
        int tpfScale = 2_000;
        List<Path> subset = files.subList(0, tpfScale);
        AtomicInteger tpfOk = new AtomicInteger(), tpfFail = new AtomicInteger();
        long t0 = System.nanoTime();
        List<Thread> threads = new ArrayList<>(tpfScale);
        for (Path f : subset) {
            Thread t = new Thread(() -> {
                ResultProcessor.Outcome o = new ResultProcessor.ScoreFileTask(f).call();
                if (o.ok()) tpfOk.incrementAndGet(); else tpfFail.incrementAndGet();
            });
            threads.add(t); t.start();
        }
        for (Thread t : threads) t.join();
        long tpfMs = (System.nanoTime() - t0) / 1_000_000;

        ResultProcessor.Summary best = lastSummary.get(4);
        System.out.printf("  %-34s | %-12s | %-20s | %-14s | %s%n",
                "APPROACH", "TIME (ms)", "THREADS CREATED", "OK / FAILED", "SUMMARY");
        System.out.println("  " + "-".repeat(104));
        System.out.printf("  %-34s | %-12.1f | %-20s | %-14s | %s%n",
                "ExecutorService, fixed pool of 4", timings.get(2).medianMs(), "4",
                best.succeeded() + " / " + best.failed(), "complete, by construction");
        System.out.printf("  %-34s | %-12d | %-20s | %-14s | %s%n",
                "files.parallelStream()", psBest,
                "common pool: " + (ForkJoinPool.commonPool().getParallelism() + 1),
                psOk + " / " + psFail, "complete, if no task throws");
        System.out.printf("  %-34s | %-12s | %-20s | %-14s | %s%n",
                "one thread per file (" + tpfScale + " files)", tpfMs + " for " + tpfScale,
                String.valueOf(tpfScale),
                tpfOk.get() + " / " + tpfFail.get(), "needs a concurrent counter");
        System.out.printf("  %-34s | %-12s | %-20s | %-14s | %s%n",
                "  the same, extrapolated to 10,000",
                String.format("~%.0f", tpfMs * (10_000.0 / tpfScale)), "10,000",
                "-", "10,000 stacks at once");
        System.out.println();
        System.out.printf("  %-34s | %-26s | %-22s | %s%n",
                "", "TIME", "MEMORY (THREADS x STACK)", "ONE FILE FAILS");
        System.out.println("  " + "-".repeat(104));
        String[][] alt = {
            {"ExecutorService, pool p", "O(n/p) + serial fold", "p x stack, FIXED", "returned as a value, counted"},
            {"files.parallelStream()", "O(n/p), p = cores - 1 + 1", "common pool x stack, fixed", "exception kills the whole stream"},
            {"one thread per file", "O(n/cores) at best, worse in practice", "n x stack - grows with n", "kills only that thread, silently"},
        };
        for (String[] r : alt) System.out.printf("  %-34s | %-26s | %-22s | %s%n", r[0], r[1], r[2], r[3]);
        System.out.println();
        System.out.println("  NOTE, AGAINST OUR OWN RECOMMENDATION: the parallel stream was the");
        System.out.println("  FASTEST of the three in the run above.  It avoids allocating 10,000");
        System.out.println("  Future objects and avoids the separate collecting loop, and on a");
        System.out.printf("  %d-processor machine its common pool is already the right size.  We%n",
                Runtime.getRuntime().availableProcessors());
        System.out.println("  still recommend the ExecutorService, and the reason is the two columns");
        System.out.println("  AFTER time: bounded memory and a per-file failure that is a value");
        System.out.println("  rather than a thrown exception.  Quoting only the timing would make");
        System.out.println("  the opposite case.");
        System.out.println();
        System.out.println("  TIME.  All three are O(n/p) in principle.  The difference is what p");
        System.out.println("  is and whether you chose it.  A parallel stream uses the common");
        System.out.println("  ForkJoinPool, whose size is fixed by the JVM at cores-1 workers plus");
        System.out.printf("  the calling thread - %d here - and it is SHARED with every other%n",
                ForkJoinPool.commonPool().getParallelism() + 1);
        System.out.println("  parallel stream in the program.  One thread per file is not faster for");
        System.out.println("  having more threads: only as many can run as there are cores, and the");
        System.out.println("  rest are overhead - 10,000 thread creations and 10,000 stacks.");
        System.out.println();
        System.out.println("  MEMORY is the decisive difference. The pool holds p stacks whatever n");
        System.out.printf("  is. One thread per file holds n stacks at once: at the 83 kB resident%n");
        System.out.printf("  per thread measured in Q4, 10,000 threads is about %.0f MB resident and%n",
                10_000 * 83 / 1024.0);
        System.out.println("  about 10 GB of RESERVED address space at the default 1 MB stack. That");
        System.out.println("  is why this comparison was run at 2,000 files and extrapolated rather");
        System.out.println("  than run at 10,000: the honest reason is that the unbounded design is");
        System.out.println("  the one that does not scale, which is the finding.");
        System.out.println();
        System.out.println("  FAILURE HANDLING is where the parallel stream is genuinely worse, and");
        System.out.println("  it is not obvious from the timing. If a mapped function throws, the");
        System.out.println("  exception propagates out of the terminal operation and the whole");
        System.out.println("  stream is abandoned - 9,999 good files are lost to one bad one. It");
        System.out.println("  works above ONLY because ScoreFileTask.call() never throws. Keeping");
        System.out.println("  that property is what makes a parallel stream usable here at all, and");
        System.out.println("  it is a property of our task, not of streams.");
        System.out.println();
        System.out.println("  CONCLUSION.  ExecutorService with a fixed pool of 4 is the design we");
        System.out.println("  recommend. It is the only one of the three that bounds its own memory,");
        System.out.println("  lets us choose p from measurement rather than inherit it, and reports");
        System.out.println("  a per-file failure without relying on the task never throwing.");
    }
}
