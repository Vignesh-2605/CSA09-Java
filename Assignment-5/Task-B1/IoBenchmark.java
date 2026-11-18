/*
 * Assignment 5 (CO5) Task B1 / A2  -  the measurements.
 *
 *  - line counting at 1 MB, 10 MB and 50 MB, unbuffered vs buffered
 *  - pass search as the file grows from 1,000 to 100,000 records
 *  - scanning every find() vs loading a HashMap once
 *
 * Name: Vignesh S   Register Number: 192325002
 */
import bench.Bench;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalDate;
import java.util.*;

public class IoBenchmark {

    static long readCalls;                 // counted by the instrumented streams

    /** Counts a stream's read() calls, to show how many the OS is asked for. */
    static class CountingInputStream extends FilterInputStream {
        CountingInputStream(InputStream in) { super(in); }
        @Override public int read() throws IOException { readCalls++; return super.read(); }
        @Override public int read(byte[] b, int off, int len) throws IOException {
            readCalls++; return super.read(b, off, len);
        }
    }

    static Path makeLogFile(Path p, long targetBytes) throws IOException {
        if (Files.exists(p) && Files.size(p) >= targetBytes * 0.98) return p;
        try (BufferedWriter w = Files.newBufferedWriter(p, StandardCharsets.UTF_8)) {
            long written = 0; int i = 0;
            while (written < targetBytes) {
                String line = String.format("2026-11-%02d 08:%02d:%02d|1923%05d|PRESENT|ROOM-%03d%n",
                        (i % 28) + 1, i % 60, (i * 7) % 60, i % 100000, i % 400);
                w.write(line); written += line.length(); i++;
            }
        }
        return p;
    }

    static long countUnbuffered(Path p) throws IOException {
        long lines = 0; readCalls = 0;
        try (InputStream in = new CountingInputStream(Files.newInputStream(p))) {
            int b;
            while ((b = in.read()) != -1) if (b == '\n') lines++;   // ONE byte at a time
        }
        return lines;
    }

    static long countBuffered(Path p) throws IOException {
        long lines = 0; readCalls = 0;
        try (InputStream in = new BufferedInputStream(new CountingInputStream(Files.newInputStream(p)), 8192)) {
            int b;
            while ((b = in.read()) != -1) if (b == '\n') lines++;
        }
        return lines;
    }

    static long countReadAllLines(Path p) throws IOException {
        return Files.readAllLines(p, StandardCharsets.UTF_8).size();
    }

    public static void main(String[] args) throws Exception {
        System.out.println("TASK B1 / A2  -  I/O MEASUREMENTS");
        System.out.println("=".repeat(112));
        System.out.println("Name: Vignesh S   Register Number: 192325002");
        System.out.printf("JVM %s%n", System.getProperty("java.version"));

        Path dir = Path.of("..", "data");
        Files.createDirectories(dir);

        /* ---------- 1. line counting ---------- */
        System.out.println();
        System.out.println("1.  COUNTING LINES: ONE BYTE AT A TIME, BUFFERED, AND readAllLines");
        System.out.println("-".repeat(112));
        long[] sizes = {1L << 20, 10L << 20, 50L << 20};
        String[] names = {"1 MB", "10 MB", "50 MB"};
        List<Bench.Result> results = new ArrayList<>();

        System.out.printf("  %-8s | %-22s | %-12s | %-16s | %-12s | %s%n",
                "SIZE", "METHOD", "TIME (ms)", "read() CALLS", "LINES", "RELATIVE");
        System.out.println("  " + "-".repeat(104));
        for (int i = 0; i < sizes.length; i++) {
            Path log = makeLogFile(dir.resolve("attendance-" + names[i].replace(" ", "") + ".log"), sizes[i]);
            long bytes = Files.size(log);

            long t0 = System.nanoTime(); long linesU = countUnbuffered(log);
            long msU = (System.nanoTime() - t0) / 1_000_000; long callsU = readCalls;

            t0 = System.nanoTime(); long linesB = countBuffered(log);
            long msB = (System.nanoTime() - t0) / 1_000_000; long callsB = readCalls;

            t0 = System.nanoTime(); long linesA = countReadAllLines(log);
            long msA = (System.nanoTime() - t0) / 1_000_000;

            System.out.printf("  %-8s | %-22s | %-12d | %-16s | %-12s | %s%n", names[i],
                    "unbuffered, 1 byte", msU, String.format("%,d", callsU), String.format("%,d", linesU), "baseline");
            System.out.printf("  %-8s | %-22s | %-12d | %-16s | %-12s | %.0fx faster%n", "",
                    "BufferedInputStream", msB, String.format("%,d", callsB), String.format("%,d", linesB),
                    msB == 0 ? msU : (double) msU / Math.max(msB, 1));
            System.out.printf("  %-8s | %-22s | %-12d | %-16s | %-12s | %.0fx faster%n", "",
                    "Files.readAllLines", msA, "n/a", String.format("%,d", linesA),
                    msA == 0 ? msU : (double) msU / Math.max(msA, 1));
            System.out.printf("  %-8s | %-22s | %s%n", "", "file size", String.format("%,d bytes", bytes));
            System.out.println("  " + "-".repeat(104));
        }

        /* ---------- 2. memory: readAllLines vs streaming ---------- */
        System.out.println();
        System.out.println("2.  MEMORY: readAllLines HOLDS THE WHOLE FILE, BUFFERED READING DOES NOT");
        System.out.println("-".repeat(112));
        Path big = dir.resolve("attendance-50MB.log");
        Runtime rt = Runtime.getRuntime();
        System.gc(); Thread.sleep(200);
        long before = (rt.totalMemory() - rt.freeMemory()) / 1024;
        List<String> all = Files.readAllLines(big, StandardCharsets.UTF_8);
        long after = (rt.totalMemory() - rt.freeMemory()) / 1024;
        int n = all.size();
        all = null; System.gc(); Thread.sleep(200);
        long before2 = (rt.totalMemory() - rt.freeMemory()) / 1024;
        countBuffered(big);
        long after2 = (rt.totalMemory() - rt.freeMemory()) / 1024;
        System.out.printf("  %-28s | %-18s | %s%n", "METHOD", "HEAP GROWTH", "WHAT IS HELD");
        System.out.println("  " + "-".repeat(104));
        System.out.printf("  %-28s | %-18s | %,d String objects%n", "Files.readAllLines",
                String.format("%,d kB", after - before), n);
        System.out.printf("  %-28s | %-18s | one 8 kB buffer%n", "BufferedInputStream",
                String.format("%,d kB", Math.max(0, after2 - before2)));
        System.out.printf("  file on disk: %,d kB%n", Files.size(big) / 1024);

        /* ---------- 3. search as the file grows ---------- */
        System.out.println();
        System.out.println("3.  PASS SEARCH AS THE FILE GROWS FROM 1,000 TO 100,000 RECORDS");
        System.out.println("-".repeat(112));
        int[] counts = {1_000, 10_000, 50_000, 100_000};
        System.out.printf("  %-12s | %-16s | %-16s | %-16s | %s%n",
                "RECORDS", "FIRST (ms)", "MIDDLE (ms)", "LAST (ms)", "MISSING (ms)");
        System.out.println("  " + "-".repeat(104));
        Map<Integer, Path> files = new LinkedHashMap<>();
        for (int c : counts) {
            Path pf = dir.resolve("passes-" + c + ".txt");
            if (!Files.exists(pf) || Files.readAllLines(pf).size() != c) {
                Files.deleteIfExists(pf);
                try (BufferedWriter w = Files.newBufferedWriter(pf, StandardCharsets.UTF_8)) {
                    for (int i = 1; i <= c; i++) {
                        w.write(new BusPass(String.format("P%06d", i), String.format("1923%05d", i),
                                "Student " + i, "R" + (i % 20 + 1), "Stop " + (i % 50),
                                LocalDate.parse("2027-03-31"), 400000L + i).toLine());
                        w.newLine();
                    }
                }
            }
            files.put(c, pf);
            PassStore st = new PassStore(pf);
            double first = time(() -> st.find("P000001"));
            double mid   = time(() -> st.find(String.format("P%06d", c / 2)));
            double last  = time(() -> st.find(String.format("P%06d", c)));
            double miss  = time(() -> st.find("P999999"));
            System.out.printf("  %-12s | %-16.2f | %-16.2f | %-16.2f | %.2f%n",
                    String.format("%,d", c), first, mid, last, miss);
        }
        System.out.println();
        System.out.println("  The FIRST column barely moves: find() stops at the first match, so a");
        System.out.println("  hit at the top of the file costs the same however big the file is.");
        System.out.println("  The LAST and MISSING columns grow with the file, because both read");
        System.out.println("  every line.  That is O(n) per search, and the missing case is the");
        System.out.println("  worst case every time.");

        /* ---------- 4. scan vs HashMap ---------- */
        System.out.println();
        System.out.println("4.  SCANNING EVERY find() VERSUS LOADING A HashMap ONCE");
        System.out.println("-".repeat(112));
        Path pf = files.get(100_000);
        PassStore scanStore = new PassStore(pf);
        int[] queryCounts = {1, 10, 100, 1000};
        System.out.printf("  %-12s | %-20s | %-20s | %-16s | %s%n",
                "q SEARCHES", "SCAN TOTAL (ms)", "HASHMAP TOTAL (ms)", "MAP BUILD (ms)", "WINNER");
        System.out.println("  " + "-".repeat(104));

        long tb = System.nanoTime();
        Map<String, BusPass> index = new HashMap<>();
        try (BufferedReader r = Files.newBufferedReader(pf, StandardCharsets.UTF_8)) {
            String line;
            while ((line = r.readLine()) != null) {
                try { BusPass p = BusPass.parse(line); index.put(p.passId(), p); }
                catch (BusPass.DamagedRecordException ignored) { }
            }
        }
        double buildMs = (System.nanoTime() - tb) / 1e6;

        Random rnd = new Random(192325002L);
        for (int q : queryCounts) {
            String[] ids = new String[q];
            for (int i = 0; i < q; i++) ids[i] = String.format("P%06d", 1 + rnd.nextInt(100_000));
            long t0 = System.nanoTime();
            for (String id : ids) scanStore.find(id);
            double scanMs = (System.nanoTime() - t0) / 1e6;
            t0 = System.nanoTime();
            for (String id : ids) index.get(id);
            double mapMs = (System.nanoTime() - t0) / 1e6;
            System.out.printf("  %-12s | %-20.1f | %-20.3f | %-16.1f | %s%n",
                    String.format("%,d", q), scanMs, mapMs + buildMs, buildMs,
                    (mapMs + buildMs) < scanMs ? "HashMap" : "scan");
        }
        System.out.printf("%n  HashMap memory for 100,000 passes: about %,d kB%n", mapMemoryKb(index));
        System.out.println("  Scan memory: one 8 kB buffer and one record at a time.");
        System.exit(0);
    }

    interface Call { Object run() throws Exception; }
    static double time(Call c) throws Exception {
        c.run();                                    // warm the page cache
        long t0 = System.nanoTime();
        for (int i = 0; i < 3; i++) c.run();
        return (System.nanoTime() - t0) / 3e6;
    }
    static long mapMemoryKb(Map<String, BusPass> m) throws Exception {
        Runtime rt = Runtime.getRuntime();
        System.gc(); Thread.sleep(150);
        return (rt.totalMemory() - rt.freeMemory()) / 1024;
    }
}
