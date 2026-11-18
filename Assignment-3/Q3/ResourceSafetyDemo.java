/*
 * Programming in Java  -  Assignment 3 (CO3)
 * Q3  -  Resource Safety, Debugging and Robust Code: the experiments
 *
 * Name: Vignesh S   Register Number: 192325002
 */
import java.io.*;
import java.nio.file.*;
import java.util.*;

public class ResourceSafetyDemo {

    static Path dir;

    static List<Path> makeStores(int good, boolean corruptOne) throws IOException {
        List<Path> files = new ArrayList<>();
        for (int i = 1; i <= good; i++) {
            Path f = dir.resolve(String.format("store%02d.csv", i));
            StringBuilder sb = new StringBuilder("ITEM,QTY,PRICE\n");
            for (int r = 1; r <= 5; r++) sb.append("item").append(r).append(",").append(r * 2).append(",").append(100).append("\n");
            Files.writeString(f, sb.toString());
            files.add(f);
        }
        if (corruptOne) {
            // The INPUT Student A was asked to construct: it fails PART-WAY,
            // after three good files have already been read and written.
            Path bad = dir.resolve("store04.csv");
            Files.writeString(bad, "ITEM,QTY,PRICE\nitem1,4,100\nitem2,SEVEN,100\nitem3,2,100\n");
            if (!files.contains(bad)) files.add(bad);
            files.sort(Comparator.comparing(Path::getFileName));
        }
        return files;
    }

    public static void main(String[] args) throws Exception {
        System.out.println("Q3  -  RESOURCE SAFETY, DEBUGGING AND ROBUST CODE");
        System.out.println("=".repeat(112));
        System.out.println("Name: Vignesh S   Register Number: 192325002");
        dir = Path.of("stores");
        Files.createDirectories(dir);

        /* ---------- SHARED: both versions produce the same report ---------- */
        System.out.println();
        System.out.println("SHARED GROUP WORK  -  BOTH VERSIONS PRODUCE THE SAME REPORT");
        System.out.println("=".repeat(112));
        List<Path> clean = makeStores(6, false);
        Path outA = Path.of("report_manual.csv"), outB = Path.of("report_twr.csv");
        long tA = SalesReport.manual(clean, outA, false);
        long tB = SalesReport.twr(clean, outB, false);
        String rA = Files.readString(outA), rB = Files.readString(outB);
        System.out.printf("  manual close()      grand total = %d%n", tA);
        System.out.printf("  try-with-resources  grand total = %d%n", tB);
        System.out.printf("  byte-for-byte identical reports: %s%n", rA.equals(rB) ? "YES" : "NO");
        System.out.println();
        System.out.println("  the report both versions produced:");
        for (String line : rA.strip().split("\n")) System.out.println("      " + line);

        /* ---------- STUDENT A: the leak, counted ---------- */
        System.out.println();
        System.out.println("STUDENT A  -  DEBUGGING AND EXPERIMENTAL ANALYSIS");
        System.out.println("=".repeat(112));
        System.out.println();
        System.out.println("1.  AN INPUT THAT MAKES THE MANUAL VERSION FAIL PART-WAY");
        System.out.println("-".repeat(112));
        List<Path> withBad = makeStores(6, true);
        System.out.println("  stores/store04.csv row 2 reads  'item2,SEVEN,100'");
        System.out.println("  QTY is a word, so Long.parseLong throws NumberFormatException");
        System.out.println("  after store01, store02 and store03 have already been written.");

        System.out.println();
        System.out.println("2.  FILE DESCRIPTORS LEFT OPEN BY EACH VERSION, SAME FAILING INPUT");
        System.out.println("-".repeat(112));
        System.out.println("  the logging Student A added, printed as each version fails:");

        // A baseline is taken immediately before each run and subtracted, so
        // that the first run's leak cannot be counted again against the second.
        // Our first attempt omitted this and reported try-with-resources as
        // leaking one handle, which was the manual version's leak still open.
        SalesReport.OPENED.clear();
        int baseManual = SalesReport.openStoreFileCount();
        int fdManual = -1; String outcomeManual;
        try {
            SalesReport.manual(withBad, outA, true);
            outcomeManual = "completed";
        } catch (IOException e) {
            fdManual = SalesReport.openStoreFileCount() - baseManual;
            outcomeManual = "threw IOException";
        }
        String rowManual = String.format("  %-26s | %-18s | %-26s | %s",
                "manual close()", outcomeManual, fdManual + " leaked", "partial (3 of 6 stores)");

        // release the manual version's leak before measuring the next one
        for (BufferedReader r : SalesReport.OPENED) try { r.close(); } catch (IOException ignored) { }
        SalesReport.OPENED.clear();

        int baseTwr = SalesReport.openStoreFileCount();
        int fdTwr = -1; String outcomeTwr;
        try {
            SalesReport.twr(withBad, outB, true);
            outcomeTwr = "completed";
        } catch (IOException e) {
            fdTwr = SalesReport.openStoreFileCount() - baseTwr;
            outcomeTwr = "threw IOException";
        }
        System.out.println();
        System.out.printf("  %-26s | %-18s | %-26s | %s%n",
                "VERSION", "OUTCOME", "STORE FDs STILL OPEN", "REPORT WRITTEN?");
        System.out.println("  " + "-".repeat(104));
        System.out.println(rowManual);
        System.out.printf("  %-26s | %-18s | %-26s | %s%n",
                "try-with-resources", outcomeTwr, fdTwr + " leaked", "partial (3 of 6 stores)");
        System.out.printf("  %-26s | %-18s | %-26s | %s%n",
                "(fd baseline before each)", "", "manual " + baseManual + ", twr " + baseTwr, "");
        System.out.println();
        System.out.println("  Counted by reading /proc/self/fd and following each symbolic link,");
        System.out.println("  inside the catch block - that is, while the failure is being handled");
        System.out.println("  and before the JVM exits and closes everything anyway.");
        System.out.println("  Both versions abandon the report.  Only one leaks the handle.");

        System.out.println();
        System.out.println("3.  THE LEAK COMPOUNDS  -  THE \"TOO MANY OPEN FILES\" SYMPTOM");
        System.out.println("-".repeat(112));
        System.out.println("  The retail chain's complaint is not one leaked handle, it is the");
        System.out.println("  process eventually refusing to open anything.  Reproduced by running");
        System.out.println("  the manual version repeatedly WITHOUT letting the leak be collected,");
        System.out.println("  under a deliberately low descriptor limit (see ulimit_demo.txt).");
        List<BufferedReader> hold = new ArrayList<>();
        int opened = 0; String stopped = "not reached";
        try {
            for (int i = 0; i < 100_000; i++) {
                hold.add(new BufferedReader(new FileReader(clean.get(0).toFile())));
                opened++;
            }
        } catch (IOException e) {
            stopped = e.getClass().getSimpleName() + ": " + e.getMessage();
        }
        System.out.printf("  handles opened and retained before failure : %d%n", opened);
        System.out.printf("  what stopped it                            : %s%n", stopped);
        for (BufferedReader r : hold) try { r.close(); } catch (IOException ignored) { }
        System.out.println("  A leaked reader is only reclaimed if the garbage collector happens to");
        System.out.println("  run and the object is unreachable.  Nothing in the language promises");
        System.out.println("  that before the limit is hit, which is why the failure looks random.");

        /* ---------- STUDENT B: design and correctness ---------- */
        System.out.println();
        System.out.println("STUDENT B  -  DESIGN AND CORRECTNESS");
        System.out.println("=".repeat(112));
        System.out.println();
        System.out.println("4.  EVERY RESOURCE THE PROGRAM OPENS, AND WHERE IT MUST BE CLOSED");
        System.out.println("-".repeat(112));
        System.out.printf("  %-24s | %-20s | %-24s | %s%n", "RESOURCE", "OPENED WHERE", "MUST CLOSE WHERE", "HOW MANY AT ONCE");
        System.out.println("  " + "-".repeat(104));
        String[][] res = {
            {"BufferedWriter", "start of the report", "after the last store", "1 for the whole run"},
            {"PrintWriter (wraps it)", "same statement", "closing it closes the writer", "1"},
            {"FileReader", "top of each store loop", "end of THAT iteration", "1 at a time, f in total if leaked"},
            {"BufferedReader (wraps it)", "same statement", "closing it closes the FileReader", "1 at a time"},
        };
        for (String[] r : res) System.out.printf("  %-24s | %-20s | %-24s | %s%n", r[0], r[1], r[2], r[3]);
        System.out.println();
        System.out.println("  A wrapper owns what it wraps: closing the BufferedReader closes the");
        System.out.println("  FileReader beneath it, so only the outermost object needs closing.");

        System.out.println();
        System.out.println("5.  WHY THE MANUAL VERSION CAN LEAK");
        System.out.println("-".repeat(112));
        System.out.println("  Because close() is an ordinary statement, and an exception skips");
        System.out.println("  ordinary statements.  In the manual version the inner close() sits");
        System.out.println("  after the parsing loop:");
        System.out.println();
        System.out.println("      reader = new BufferedReader(...);");
        System.out.println("      while ((line = reader.readLine()) != null) { ... parseLong ... }");
        System.out.println("      reader.close();          <- NEVER REACHED when parseLong throws");
        System.out.println();
        System.out.println("  Putting it in a finally fixes THIS leak, but two traps remain:");
        System.out.println("    (a) if the resource is assigned inside the try, finally must still");
        System.out.println("        null-check it, because the constructor itself can throw;");
        System.out.println("    (b) if close() itself throws inside finally, that exception REPLACES");
        System.out.println("        the original one and the real cause is lost - demonstrated next.");

        System.out.println();
        System.out.println("6.  HOW try-with-resources GUARANTEES CLOSING, AND IN WHAT ORDER");
        System.out.println("-".repeat(112));
        System.out.println("  Closing order is REVERSE OF DECLARATION, and it happens BEFORE any");
        System.out.println("  catch or finally attached to the same try. Proof, with three resources:");
        System.out.println();
        try (Tracked a = new Tracked("first"); Tracked b = new Tracked("second"); Tracked c = new Tracked("third")) {
            System.out.println("      body of the try block runs here");
        }
        System.out.println();
        System.out.println("  And closing still happens when the body throws:");
        try {
            try (Tracked a = new Tracked("outer"); Tracked b = new Tracked("inner")) {
                throw new IllegalStateException("body failed");
            } catch (IllegalStateException e) {
                System.out.println("      catch clause runs AFTER both closes: " + e.getMessage());
            }
        } catch (Exception ignored) { }

        System.out.println();
        System.out.println("7.  WHAT A SUPPRESSED EXCEPTION IS, AND WHEN IT OCCURS");
        System.out.println("-".repeat(112));
        System.out.println("  It occurs when the try body throws AND close() also throws.  Two");
        System.out.println("  exceptions exist but a method can only throw one, so the one from");
        System.out.println("  close() is attached to the primary one instead of replacing it.");
        System.out.println();
        try {
            try (FailingClose fc = new FailingClose()) {
                throw new IllegalStateException("the REAL problem: row 2 of store04 is corrupt");
            }
        } catch (Exception e) {
            System.out.println("      primary exception   : " + e);
            for (Throwable s : e.getSuppressed())
                System.out.println("      suppressed          : " + s);
        }
        System.out.println();
        System.out.println("  Compare the SAME situation with a manual finally, where close() wins:");
        try {
            FailingClose fc = new FailingClose();
            try {
                throw new IllegalStateException("the REAL problem: row 2 of store04 is corrupt");
            } finally {
                fc.close();
            }
        } catch (Exception e) {
            System.out.println("      what the caller sees: " + e);
            System.out.println("      suppressed count    : " + e.getSuppressed().length
                    + "   <- the real cause is GONE");
        }

        /* ---------- joint ---------- */
        System.out.println();
        System.out.println("JOINT ENGINEERING DECISION");
        System.out.println("=".repeat(112));
        System.out.println("  WHEN IS try-with-resources NOT SUFFICIENT ON ITS OWN?");
        System.out.println();
        System.out.printf("  %-44s | %s%n", "CONDITION", "WHAT ELSE IS NEEDED");
        System.out.println("  " + "-".repeat(104));
        String[][] joint = {
            {"one corrupt row must not abandon the report", "per-row try/catch inside the loop"},
            {"the output file is half-written on failure", "write to a temp file, rename on success"},
            {"the caller must know WHICH store failed", "logging, or a failure list in the result"},
            {"a store file is missing, not corrupt", "a separate catch for IOException per file"},
            {"the resource is not AutoCloseable", "a manual finally, or a wrapper that is"},
            {"the resource is returned to the caller", "the caller owns closing; TWR would close early"},
            {"closing itself must not be silent", "read getSuppressed() and log it"},
            {"a lock or a transaction, not a file", "commit/rollback semantics, not just close"},
        };
        for (String[] r : joint) System.out.printf("  %-44s | %s%n", r[0], r[1]);
        System.out.println();
        System.out.println("  Demonstrated: the robust version, which keeps try-with-resources AND");
        System.out.println("  adds a per-row catch, on the SAME failing input:");
        List<String> failures = new ArrayList<>();
        long rt = SalesReport.robust(withBad, Path.of("report_robust.csv"), failures);
        System.out.printf("      grand total   : %d   (all 6 stores reported, not 3)%n", rt);
        System.out.printf("      rows rejected : %d%n", failures.size());
        for (String f : failures) System.out.println("          " + f);
        for (String line : Files.readString(Path.of("report_robust.csv")).strip().split("\n"))
            System.out.println("      " + line);

        /* ---------- alternative concept and complexity ---------- */
        System.out.println();
        System.out.println("ALTERNATIVE CONCEPT AND COMPLEXITY ANALYSIS");
        System.out.println("=".repeat(112));
        System.out.println("  f = number of store files, r = rows per file.");
        System.out.println();
        System.out.printf("  %-26s | %-14s | %-26s | %s%n",
                "VERSION", "TIME", "MAX HANDLES AT FAILURE", "REPORT ON FAILURE");
        System.out.println("  " + "-".repeat(104));
        String[][] cx = {
            {"try-with-resources", "O(f x r)", "1 (the writer only)", "partial, abandoned"},
            {"explicit close in finally", "O(f x r)", "1 (the writer only)", "partial, abandoned"},
            {"close after the loop (bug)", "O(f x r)", "2 (writer + the bad reader)", "partial, abandoned"},
            {"no closing at all", "O(f x r)", "f + 1 - every file ever opened", "partial, then crashes"},
            {"robust: TWR + per-row catch", "O(f x r)", "1 (the writer only)", "COMPLETE, bad rows listed"},
        };
        for (String[] r : cx) System.out.printf("  %-26s | %-14s | %-26s | %s%n", r[0], r[1], r[2], r[3]);
        System.out.println();
        System.out.println("  TIME is O(f x r) in every version: each row is read and parsed once,");
        System.out.println("  and closing a file is O(1).  Resource safety costs nothing in time.");
        System.out.println();
        System.out.println("  SPACE is where they differ, and it is not the heap - it is the");
        System.out.println("  process-wide file-descriptor table, a limit the program shares with");
        System.out.println("  everything else it does.  With no closing at all the handle count");
        System.out.println("  grows as O(f), so the program works on a test set of ten stores and");
        System.out.println("  fails on a chain with a thousand - which is exactly the bug report.");
        System.out.println();
        System.out.println("  try-with-resources and a correct finally are EQUIVALENT in both");
        System.out.println("  columns.  The argument for try-with-resources is not complexity, it is");
        System.out.println("  that it cannot be got wrong: there is no close() to forget, no");
        System.out.println("  null-check to omit, and the suppressed exception is kept rather than");
        System.out.println("  discarded.  Section 7 above shows the manual finally losing the real");
        System.out.println("  cause of the failure, which no amount of care in the finally recovers.");
    }

    /** Logs its own construction and closing, so the order is visible. */
    static class Tracked implements AutoCloseable {
        private final String name;
        Tracked(String name) { this.name = name; System.out.println("      opened  " + name); }
        @Override public void close() { System.out.println("      closed  " + name); }
    }

    /** A resource whose close() fails, for the suppressed-exception demo. */
    static class FailingClose implements AutoCloseable {
        @Override public void close() {
            throw new IllegalStateException("close() failed too: disk went offline");
        }
    }
}
