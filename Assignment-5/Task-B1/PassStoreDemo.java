/*
 * Assignment 5 (CO5) Task B1 - the supplied example, plus the three test cases
 * the task asks for: normal, missing pass ID, and a damaged line.
 * Name: Vignesh S   Register Number: 192325002
 */
import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDate;
import java.util.*;

public class PassStoreDemo {
    public static void main(String[] args) throws Exception {
        System.out.println("TASK B1  -  THE BUS-PASS FILE STORE");
        System.out.println("=".repeat(112));
        System.out.println("Name: Vignesh S   Register Number: 192325002");

        Path f = Path.of("passes.txt");
        Files.deleteIfExists(f);
        PassStore store = new PassStore(f);

        /* ---------- the supplied example ---------- */
        System.out.println();
        System.out.println("1.  THE SUPPLIED EXAMPLE");
        System.out.println("-".repeat(112));
        store.add(new BusPass("P1001", "192325001", "Arun K",   "R12", "Poonamallee",
                LocalDate.parse("2027-03-31"), 450000L));
        // P1002 is written DAMAGED on purpose: only five fields
        Files.writeString(f, "P1002|192325002|Vignesh S|R12|Poonamallee\n",
                java.nio.file.StandardOpenOption.APPEND);
        store.add(new BusPass("P1003", "192325003", "Divya R",  "R07", "Tambaram",
                LocalDate.parse("2027-03-31"), 520000L));

        System.out.println("  the file as written:");
        for (String l : Files.readAllLines(f)) System.out.println("      " + l);
        System.out.println();
        System.out.println("  store.find(\"P1003\") -> " + store.find("P1003"));
        System.out.println("  store.find(\"P1002\") -> " + store.find("P1002")
                + "      <- the damaged line, skipped");
        System.out.println();
        System.out.println("  EXPECTED: the P1003 record, then null for the damaged P1002 line.");
        BusPass p1003 = store.find("P1003");
        BusPass p1002 = store.find("P1002");
        boolean ok = p1003 != null && p1003.passId().equals("P1003") && p1002 == null;
        System.out.println("  MATCHES EXPECTED OUTPUT: " + (ok ? "YES" : "NO"));
        System.out.println("  damaged lines reported : " + store.damagedLines());

        /* ---------- the three required test cases ---------- */
        System.out.println();
        System.out.println("2.  TEST TABLE");
        System.out.println("-".repeat(112));
        System.out.printf("  %-5s | %-34s | %-30s | %-22s | %s%n",
                "ID", "CASE", "EXPECTED", "ACTUAL", "VERDICT");
        System.out.println("  " + "-".repeat(104));
        int pass = 0, total = 0;

        total++; BusPass r1 = store.find("P1001");
        boolean v1 = r1 != null && r1.name().equals("Arun K");
        if (v1) pass++;
        System.out.printf("  %-5s | %-34s | %-30s | %-22s | %s%n", "N1", "normal: a pass that exists",
                "the P1001 record", r1 == null ? "null" : r1.passId() + " " + r1.name(), v1 ? "PASS" : "FAIL");

        total++; BusPass r2 = store.find("P9999");
        boolean v2 = r2 == null; if (v2) pass++;
        System.out.printf("  %-5s | %-34s | %-30s | %-22s | %s%n", "N2", "a pass ID that does not exist",
                "null", String.valueOf(r2), v2 ? "PASS" : "FAIL");

        total++; BusPass r3 = store.find("P1002");
        boolean v3 = r3 == null && !store.damagedLines().isEmpty(); if (v3) pass++;
        System.out.printf("  %-5s | %-34s | %-30s | %-22s | %s%n", "N3", "a damaged line (5 fields)",
                "null, line reported", r3 + ", " + store.damagedLines().size() + " reported", v3 ? "PASS" : "FAIL");

        total++; BusPass r4 = store.find("P1003");
        boolean v4 = r4 != null; if (v4) pass++;
        System.out.printf("  %-5s | %-34s | %-30s | %-22s | %s%n", "N4", "a record AFTER the damaged line",
                "found, not hidden", r4 == null ? "null" : r4.passId(), v4 ? "PASS" : "FAIL");

        // more damage: non-numeric amount, empty line, bad date
        Files.writeString(f, "P1004|192325004|Meena S|R07|Tambaram|2027-03-31|FIVELAKH\n", StandardOpenOption.APPEND);
        Files.writeString(f, "\n", StandardOpenOption.APPEND);
        Files.writeString(f, "P1005|192325005|Karthik|R12|Avadi|31-03-2027|450000\n", StandardOpenOption.APPEND);
        store.add(new BusPass("P1006", "192325006", "Latha M", "R12", "Avadi",
                LocalDate.parse("2027-03-31"), 480000L));

        total++; BusPass r5 = store.find("P1004");
        boolean v5 = r5 == null; if (v5) pass++;
        System.out.printf("  %-5s | %-34s | %-30s | %-22s | %s%n", "N5", "non-numeric amount",
                "null, reported", String.valueOf(r5), v5 ? "PASS" : "FAIL");

        total++; BusPass r6 = store.find("P1005");
        boolean v6 = r6 == null; if (v6) pass++;
        System.out.printf("  %-5s | %-34s | %-30s | %-22s | %s%n", "N6", "wrong date format",
                "null, reported", String.valueOf(r6), v6 ? "PASS" : "FAIL");

        total++; BusPass r7 = store.find("P1006");
        boolean v7 = r7 != null; if (v7) pass++;
        System.out.printf("  %-5s | %-34s | %-30s | %-22s | %s%n", "N7", "record after THREE damaged lines",
                "still found", r7 == null ? "null" : r7.passId(), v7 ? "PASS" : "FAIL");

        total++;
        List<BusPass> route12 = store.listByRoute("R12");
        boolean v8 = route12.size() == 2; if (v8) pass++;
        System.out.printf("  %-5s | %-34s | %-30s | %-22s | %s%n", "N8", "list passes on route R12",
                "2 (P1001, P1006)", route12.size() + " found", v8 ? "PASS" : "FAIL");

        total++;
        boolean v9;
        try { new PassStore(Path.of("no-such-file.txt")).find("P1001"); v9 = false; }
        catch (NoSuchFileException e) { v9 = true; }
        if (v9) pass++;
        System.out.printf("  %-5s | %-34s | %-30s | %-22s | %s%n", "N9", "the file itself is missing",
                "NoSuchFileException", v9 ? "NoSuchFileException" : "no exception", v9 ? "PASS" : "FAIL");

        System.out.println();
        System.out.printf("  %d of %d checks passed.%n", pass, total);
        int[] counts = store.countRecords();
        System.out.printf("  file now holds %d usable records and %d damaged lines.%n", counts[0], counts[1]);

        System.out.println();
        System.out.println("3.  WHY A DAMAGED LINE DOES NOT STOP THE SEARCH");
        System.out.println("-".repeat(112));
        System.out.println("  Because the record boundary is the NEWLINE, and the newline is intact");
        System.out.println("  even when the fields between two newlines are not.  BufferedReader");
        System.out.println("  finds the next record without understanding the damaged one, so the");
        System.out.println("  parse failure is contained to a single line.");
        System.out.println();
        System.out.println("  Test N7 is the evidence: P1006 is found even though three damaged");
        System.out.println("  lines sit between it and the start of the file.  That property is a");
        System.out.println("  consequence of the FORMAT, not of the parsing code - a fixed-length");
        System.out.println("  binary file has no such self-synchronising boundary, and one short");
        System.out.println("  write there misaligns every record that follows it.");
        System.exit(pass == total ? 0 : 1);
    }
}
