/*
 * Programming in Java  -  Assignment 5 (CO5)
 * Task B1  -  the bus-pass text-file store.  COMPLETE THE CODE: TODO 1.
 *
 * NOTE ON PROVENANCE: the deck supplied to me is the summary version and does
 * not carry the skeleton source.  This file reconstructs it from the task
 * description - add, find and list over a delimited text file, with find()
 * reading line by line, skipping damaged lines and returning the matching
 * record.  TODO 1 is marked where the task places it and is completed below.
 *
 * Expected output for the supplied example: the P1003 record, then null for
 * the damaged P1002 line.
 *
 * Name: Vignesh S   Register Number: 192325002
 */
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

public class PassStore {

    private final Path file;
    /** Damaged lines met during the last operation, for reporting. */
    private final List<String> damaged = new ArrayList<>();

    public PassStore(Path file) { this.file = file; }

    /** Appends one record.  Append mode, so an existing file is never truncated. */
    public void add(BusPass p) throws IOException {
        try (BufferedWriter w = Files.newBufferedWriter(file, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {
            w.write(p.toLine());
            w.newLine();
        }
    }

    /* ======================= TODO 1 - COMPLETED ======================= */
    /**
     * Finds a pass by its ID.
     *
     * Reads the file line by line, SKIPS damaged lines rather than failing,
     * and returns the matching record - or null when no line matches.
     *
     * The method does not read the whole file into memory: it stops at the
     * first match, so a hit early in the file costs almost nothing.
     */
    public BusPass find(String passId) throws IOException {
        damaged.clear();
        if (!Files.exists(file))
            throw new NoSuchFileException(file.toString());

        int lineNo = 0;
        try (BufferedReader r = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            String line;
            while ((line = r.readLine()) != null) {
                lineNo++;
                if (line.isBlank()) continue;
                try {
                    BusPass p = BusPass.parse(line);
                    if (p.passId().equals(passId))      // equals, never ==
                        return p;                       // stop at the first match
                } catch (BusPass.DamagedRecordException e) {
                    // A damaged line is SKIPPED, not fatal.  It is recorded so
                    // the caller can report it, and the search continues - a
                    // single bad line must not hide every record after it.
                    damaged.add("line " + lineNo + ": " + e.getMessage());
                }
            }
        }
        return null;                                    // no such pass
    }
    /* ================= end of TODO 1 ================= */

    /** Every pass on one route. */
    public List<BusPass> listByRoute(String routeNo) throws IOException {
        damaged.clear();
        List<BusPass> out = new ArrayList<>();
        int lineNo = 0;
        try (BufferedReader r = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            String line;
            while ((line = r.readLine()) != null) {
                lineNo++;
                if (line.isBlank()) continue;
                try {
                    BusPass p = BusPass.parse(line);
                    if (p.routeNo().equals(routeNo)) out.add(p);
                } catch (BusPass.DamagedRecordException e) {
                    damaged.add("line " + lineNo + ": " + e.getMessage());
                }
            }
        }
        return out;
    }

    public List<String> damagedLines() { return List.copyOf(damaged); }

    /** Counts records, reporting how many lines were unusable. */
    public int[] countRecords() throws IOException {
        int good = 0, bad = 0;
        try (BufferedReader r = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            String line;
            while ((line = r.readLine()) != null) {
                if (line.isBlank()) continue;
                try { BusPass.parse(line); good++; }
                catch (BusPass.DamagedRecordException e) { bad++; }
            }
        }
        return new int[]{good, bad};
    }
}
