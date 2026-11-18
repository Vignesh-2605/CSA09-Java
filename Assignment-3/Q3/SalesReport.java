/*
 * Programming in Java  -  Assignment 3 (CO3)
 * Q3  -  Resource Safety, Debugging and Robust Code
 *
 * Name: Vignesh S   Register Number: 192325002
 *
 * Shared group work: the report generator written BOTH ways - manual
 * try-catch-finally with close(), and try-with-resources - and verified to
 * produce the same report.
 */
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class SalesReport {

    /** One open reader we can see from the outside, for leak counting. */
    static final List<BufferedReader> OPENED = new ArrayList<>();

    static int openStoreFileCount() {
        // Every file descriptor this JVM holds appears in /proc/self/fd.
        // We count only the ones pointing at our store files.
        int n = 0;
        try {
            for (Path fd : Files.newDirectoryStream(Path.of("/proc/self/fd"))) {
                try {
                    String target = Files.readSymbolicLink(fd).toString();
                    if (target.contains("/stores/") && target.endsWith(".csv")) n++;
                } catch (IOException ignored) { /* the fd closed while we looked */ }
            }
        } catch (IOException e) { return -1; }
        return n;
    }

    /* ================= VERSION 1: manual close() in finally ================= */

    /**
     * The version the retail chain actually deployed.  It closes the reader in
     * a finally block, which looks correct - and leaks anyway.  See the
     * comment at the catch for exactly why.
     */
    static long manual(List<Path> storeFiles, Path out, boolean verbose) throws IOException {
        long grandTotal = 0;
        PrintWriter writer = null;
        try {
            writer = new PrintWriter(Files.newBufferedWriter(out));
            writer.println("STORE,ITEMS,TOTAL");

            for (Path f : storeFiles) {
                BufferedReader reader = null;
                try {
                    reader = new BufferedReader(new FileReader(f.toFile()));
                    OPENED.add(reader);
                    long storeTotal = 0; int items = 0; String line;
                    reader.readLine();                       // header
                    while ((line = reader.readLine()) != null) {
                        if (line.isBlank()) continue;
                        String[] p = line.split(",");
                        // THE LEAK IS HERE.  If this line throws - and it does,
                        // for a corrupt row - control leaves the try block and
                        // this method, because nothing catches it at this level.
                        // 'reader' is never closed: there is NO finally on this
                        // inner try.  Every file opened before the bad one stays
                        // open too, because the same thing happened to none of
                        // them - they were closed - but the BAD one is leaked,
                        // and in the original program the inner close() sat
                        // after the loop instead of in a finally.
                        long qty   = Long.parseLong(p[1].trim());
                        long price = Long.parseLong(p[2].trim());
                        storeTotal += qty * price;
                        items++;
                    }
                    writer.printf("%s,%d,%d%n", f.getFileName(), items, storeTotal);
                    grandTotal += storeTotal;
                    reader.close();                          // reached only on success
                    OPENED.remove(reader);
                } catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
                    // The LOGGING Student A was asked to add: which file failed.
                    if (verbose)
                        System.out.printf("    LOG  manual: store file %-14s failed: %s: %s%n",
                                f.getFileName(), e.getClass().getSimpleName(), e.getMessage());
                    // reader is STILL OPEN at this point - this is the leak.
                    throw new IOException("report incomplete, failed at " + f.getFileName(), e);
                }
            }
            writer.printf("TOTAL,,%d%n", grandTotal);
            return grandTotal;
        } finally {
            if (writer != null) writer.close();   // the WRITER is closed properly
        }
    }

    /* ================= VERSION 2: try-with-resources ================= */

    static long twr(List<Path> storeFiles, Path out, boolean verbose) throws IOException {
        long grandTotal = 0;
        try (PrintWriter writer = new PrintWriter(Files.newBufferedWriter(out))) {
            writer.println("STORE,ITEMS,TOTAL");
            for (Path f : storeFiles) {
                try (BufferedReader reader = new BufferedReader(new FileReader(f.toFile()))) {
                    long storeTotal = 0; int items = 0; String line;
                    reader.readLine();
                    while ((line = reader.readLine()) != null) {
                        if (line.isBlank()) continue;
                        String[] p = line.split(",");
                        long qty   = Long.parseLong(p[1].trim());
                        long price = Long.parseLong(p[2].trim());
                        storeTotal += qty * price;
                        items++;
                    }
                    writer.printf("%s,%d,%d%n", f.getFileName(), items, storeTotal);
                    grandTotal += storeTotal;
                } catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
                    if (verbose)
                        System.out.printf("    LOG  twr:    store file %-14s failed: %s: %s%n",
                                f.getFileName(), e.getClass().getSimpleName(), e.getMessage());
                    // The reader is ALREADY CLOSED by the time we get here:
                    // try-with-resources closes it on the way out of the try
                    // block, BEFORE the catch clause runs.
                    throw new IOException("report incomplete, failed at " + f.getFileName(), e);
                }
            }
            writer.printf("TOTAL,,%d%n", grandTotal);
            return grandTotal;
        }
    }

    /* ================= the robust version, for the joint question ========= */

    /** Skips the bad file instead of abandoning the report. */
    static long robust(List<Path> storeFiles, Path out, List<String> failures) throws IOException {
        long grandTotal = 0;
        try (PrintWriter writer = new PrintWriter(Files.newBufferedWriter(out))) {
            writer.println("STORE,ITEMS,TOTAL");
            for (Path f : storeFiles) {
                try (BufferedReader reader = new BufferedReader(new FileReader(f.toFile()))) {
                    long storeTotal = 0; int items = 0; String line; int lineNo = 1;
                    reader.readLine();
                    while ((line = reader.readLine()) != null) {
                        lineNo++;
                        if (line.isBlank()) continue;
                        try {
                            String[] p = line.split(",");
                            storeTotal += Long.parseLong(p[1].trim()) * Long.parseLong(p[2].trim());
                            items++;
                        } catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
                            failures.add(f.getFileName() + " line " + lineNo + ": " + e.getMessage());
                        }
                    }
                    writer.printf("%s,%d,%d%n", f.getFileName(), items, storeTotal);
                    grandTotal += storeTotal;
                } catch (IOException e) {
                    failures.add(f.getFileName() + ": unreadable: " + e.getMessage());
                }
            }
            writer.printf("TOTAL,,%d%n", grandTotal);
        }
        return grandTotal;
    }
}
