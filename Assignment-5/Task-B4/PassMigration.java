/*
 * Programming in Java  -  Assignment 5 (CO5)
 * Task B4  -  Final Engineering Decision.
 *
 * Reads the existing bus-pass text file, validates every record, loads the
 * good ones into the database in batches inside ONE transaction, and reports
 * the records that failed validation.
 *
 * Name: Vignesh S   Register Number: 192325002
 */
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.sql.*;
import java.time.LocalDate;
import java.util.*;

public class PassMigration {

    static final int BATCH = 1000;

    record Rejected(int line, String reason, String text) { }

    record Outcome(int linesRead, int migrated, List<Rejected> rejected, long millis) { }

    /** A record can be well-formed and still be unacceptable. */
    static String businessProblem(BusPass p) {
        if (p.passId().isEmpty())                      return "pass ID is empty";
        if (!p.registerNo().matches("\\d{9}"))         return "register number is not 9 digits";
        if (p.amountPaise() <= 0)                      return "amount is not positive";
        if (p.amountPaise() > 50_000_00L)              return "amount above the Rs 50,000 ceiling";
        if (p.validUntil().isBefore(LocalDate.of(2020, 1, 1))) return "validity date before 2020";
        if (p.name().isBlank())                        return "name is blank";
        return null;
    }

    public static Outcome migrate(Path source) throws IOException, SQLException {
        long t0 = System.nanoTime();
        List<Rejected> rejected = new ArrayList<>();
        int read = 0, migrated = 0;
        String sql = "INSERT INTO bus_pass VALUES(?,?,?,?,?,?,?)";

        try (Connection c = DriverManager.getConnection(Db.URL, Db.USER, Db.PASS);
             BufferedReader r = Files.newBufferedReader(source, StandardCharsets.UTF_8)) {
            c.setAutoCommit(false);                        // ONE transaction
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                String line; int n = 0;
                Set<String> seen = new HashSet<>();
                while ((line = r.readLine()) != null) {
                    read++;
                    if (line.isBlank()) { rejected.add(new Rejected(read, "blank line", "")); continue; }
                    BusPass p;
                    try {
                        p = BusPass.parse(line);
                    } catch (BusPass.DamagedRecordException e) {
                        rejected.add(new Rejected(read, e.getMessage(), snip(line)));
                        continue;
                    }
                    String problem = businessProblem(p);
                    if (problem != null) { rejected.add(new Rejected(read, problem, snip(line))); continue; }
                    if (!seen.add(p.passId())) {
                        rejected.add(new Rejected(read, "duplicate pass ID in the file", snip(line)));
                        continue;
                    }
                    ps.setString(1, p.passId()); ps.setString(2, p.registerNo());
                    ps.setString(3, p.name());   ps.setString(4, p.routeNo());
                    ps.setString(5, p.boardingStop());
                    ps.setDate(6, java.sql.Date.valueOf(p.validUntil()));
                    ps.setLong(7, p.amountPaise());
                    ps.addBatch();
                    migrated++;
                    if (++n % BATCH == 0) ps.executeBatch();
                }
                ps.executeBatch();
            } catch (SQLException e) {
                c.rollback();                              // nothing partial survives
                throw e;
            }
            c.commit();
        }
        return new Outcome(read, migrated, rejected, (System.nanoTime() - t0) / 1_000_000);
    }

    static String snip(String s) { return s.length() > 44 ? s.substring(0, 42) + ".." : s; }

    public static void main(String[] args) throws Exception {
        System.out.println("TASK B4  -  FINAL ENGINEERING DECISION");
        System.out.println("=".repeat(118));
        System.out.println("Name: Vignesh S   Register Number: 192325002");

        /* ---------- 1. DatabaseMetaData ---------- */
        System.out.println();
        System.out.println("1.  WHAT DatabaseMetaData REPORTS ABOUT THIS DATABASE");
        System.out.println("-".repeat(118));
        try (Connection c = DriverManager.getConnection(Db.URL, Db.USER, Db.PASS)) {
            DatabaseMetaData m = c.getMetaData();
            System.out.printf("  %-34s | %s%n", "database product", m.getDatabaseProductName());
            System.out.printf("  %-34s | %s%n", "database version", m.getDatabaseProductVersion());
            System.out.printf("  %-34s | %s%n", "JDBC driver", m.getDriverName());
            System.out.printf("  %-34s | %s%n", "driver version", m.getDriverVersion());
            System.out.printf("  %-34s | %d.%d%n", "JDBC specification",
                    m.getJDBCMajorVersion(), m.getJDBCMinorVersion());
            System.out.printf("  %-34s | %s%n", "supports transactions", m.supportsTransactions());
            System.out.printf("  %-34s | %s%n", "supports batch updates", m.supportsBatchUpdates());
            System.out.printf("  %-34s | %s%n", "default isolation",
                    isolationName(m.getDefaultTransactionIsolation()));
            System.out.printf("  %-34s | %s%n", "max connections", m.getMaxConnections());
            System.out.println();
            System.out.println("  tables available in csa09_test:");
            System.out.printf("      %-22s | %-10s | %s%n", "TABLE", "TYPE", "COLUMNS");
            System.out.println("      " + "-".repeat(76));
            try (ResultSet rs = m.getTables("csa09_test", null, "%", new String[]{"TABLE"})) {
                while (rs.next()) {
                    String t = rs.getString("TABLE_NAME");
                    StringBuilder cols = new StringBuilder();
                    try (ResultSet cr = m.getColumns("csa09_test", null, t, "%")) {
                        int k = 0;
                        while (cr.next() && k++ < 4)
                            cols.append(cr.getString("COLUMN_NAME")).append(" ");
                        if (k >= 4) cols.append("...");
                    }
                    System.out.printf("      %-22s | %-10s | %s%n", t, rs.getString("TABLE_TYPE"), cols);
                }
            }
        }

        /* ---------- 2. the migration ---------- */
        System.out.println();
        System.out.println("2.  MIGRATING THE EXISTING PASS FILE");
        System.out.println("-".repeat(118));
        Path src = Path.of("..", "data", "migration-source.txt");
        Files.createDirectories(src.getParent());
        buildSource(src, 50_000);
        System.out.printf("  source file: %s  (%,d bytes)%n", src.getFileName(), Files.size(src));

        PassLoaderTruncate();
        Outcome o = migrate(src);
        System.out.println();
        System.out.printf("  %-34s | %s%n", "lines read", String.format("%,d", o.linesRead()));
        System.out.printf("  %-34s | %s%n", "records migrated", String.format("%,d", o.migrated()));
        System.out.printf("  %-34s | %s%n", "records rejected", String.format("%,d", o.rejected().size()));
        System.out.printf("  %-34s | %s%n", "time", o.millis() + " ms");
        System.out.printf("  %-34s | %s%n", "rows now in bus_pass", String.format("%,d", countRows()));
        System.out.printf("  %-34s | %s%n", "migrated + rejected = lines read",
                (o.migrated() + o.rejected().size() == o.linesRead()) ? "YES - every line accounted for" : "NO");

        System.out.println();
        System.out.println("  EVERY RECORD THAT FAILED VALIDATION");
        System.out.printf("      %-8s | %-38s | %s%n", "LINE", "REASON", "THE LINE");
        System.out.println("      " + "-".repeat(104));
        for (Rejected r : o.rejected())
            System.out.printf("      %-8d | %-38s | %s%n", r.line(), r.reason(), r.text());

        Map<String, Integer> byReason = new TreeMap<>();
        for (Rejected r : o.rejected()) byReason.merge(r.reason(), 1, Integer::sum);
        System.out.println();
        System.out.printf("      %-38s | %s%n", "REASON", "COUNT");
        System.out.println("      " + "-".repeat(60));
        for (Map.Entry<String, Integer> e : byReason.entrySet())
            System.out.printf("      %-38s | %d%n", e.getKey(), e.getValue());

        /* ---------- 3. the expanded system ---------- */
        System.out.println();
        System.out.println("3.  THE EXPANDED SYSTEM: 12 CAMPUSES, 100x THE PASSES");
        System.out.println("-".repeat(118));
        long passes = 8_000L * 100;
        System.out.printf("  today      : about 8,000 passes a year, one office%n");
        System.out.printf("  expanded   : about %,d passes a year, 12 campuses issuing at once%n", passes);
        System.out.println();
        System.out.printf("  %-26s | %-22s | %-22s | %s%n",
                "REQUIREMENT", "TEXT FILE", "SERIALIZATION", "DATABASE");
        System.out.println("  " + "-".repeat(110));
        String[][] dec = {
            {"find one pass by ID", "O(n) scan - measured 35 ms at 100,000", "load the whole graph first", "O(log n) on the primary key"},
            {"12 counters writing at once", "file corruption - no locking", "whole-file rewrite each time", "row locks and transactions"},
            {"a failed half-write", "a torn last line", "the whole file is unreadable", "rolled back - measured in B3"},
            {"search by route", "O(n) scan", "O(n) in memory", "an index, O(log n)"},
            {"adding a phone column", "every reader must change", "breaks old files, see A4", "ALTER TABLE, old code unaffected"},
            {"who changed what", "nothing", "nothing", "timestamps, audit tables"},
            {"memory to serve a query", "one 8 kB buffer", "the entire object graph", "whatever one page needs"},
            {"concurrent read while writing", "may read a half-written line", "not possible", "isolation levels"},
        };
        for (String[] d : dec) System.out.printf("  %-26s | %-22s | %-22s | %s%n", d[0], d[1], d[2], d[3]);

        System.out.println();
        System.out.println("  DECISION: a DATABASE, and the measurements in this assignment are why.");
        System.out.println();
        System.out.println("    Searching.  B1 measured a text-file search at about 35 ms per lookup");
        System.out.println("    over 100,000 records, and that is O(n): at 800,000 it is roughly");
        System.out.println("    280 ms for ONE counter query. The HashMap alternative fixes the");
        System.out.println("    lookup but costs about 39 MB for 100,000 passes and has to be");
        System.out.println("    rebuilt in every process - 12 campuses means 12 copies, each stale");
        System.out.println("    the moment another campus issues a pass.");
        System.out.println();
        System.out.println("    Concurrent counters.  This is the requirement that settles it. A");
        System.out.println("    text file has no way for 12 processes to append safely, and a");
        System.out.println("    serialized file is rewritten whole, so two campuses saving at once");
        System.out.println("    lose one of the two. The database was built for exactly this.");
        System.out.println();
        System.out.println("    Safety.  B3 showed 499 orphan rows left behind by an auto-commit");
        System.out.println("    import that failed halfway, and zero left behind by the same import");
        System.out.println("    inside one transaction. Neither file format offers rollback at all.");
        System.out.println();
        System.out.println("    Loading speed is NOT the reason. B3 measured 100,000 rows inserted");
        System.out.println("    in about 2.7 seconds by batch - fast enough, but a text file would");
        System.out.println("    append them faster. Speed of writing is the one contest the files");
        System.out.println("    win, and it is the least important requirement on the list.");

        System.out.println();
        System.out.println("4.  ONE SHARED DATABASE, OR ONE SERIALIZED FILE PER CAMPUS?");
        System.out.println("-".repeat(118));
        System.out.printf("  %-30s | %-32s | %s%n", "", "12 SERIALIZED FILES", "ONE SHARED DATABASE");
        System.out.println("  " + "-".repeat(110));
        String[][] shared = {
            {"a student at two campuses", "invisible - no file knows", "one UNIQUE constraint"},
            {"total passes issued", "open and merge 12 files", "one COUNT query"},
            {"one campus offline", "its file is simply missing", "its rows are still there"},
            {"schema change", "12 files to migrate, see A4", "one ALTER TABLE"},
            {"a campus-local query", "fast - its own file only", "indexed, still fast"},
            {"failure blast radius", "one campus", "all 12 - the honest cost"},
            {"backup", "12 files to keep in step", "one consistent backup"},
        };
        for (String[] d : shared) System.out.printf("  %-30s | %-32s | %s%n", d[0], d[1], d[2]);
        System.out.println();
        System.out.println("  One shared database, with a campus_id column. The deciding row is the");
        System.out.println("  first: a transport office that cannot tell whether a student already");
        System.out.println("  holds a pass at another campus cannot enforce its own rule, and no");
        System.out.println("  arrangement of 12 separate files fixes that without rebuilding the");
        System.out.println("  thing a database already is.");
        System.out.println();
        System.out.println("  The honest cost is row 6: one shared database is one thing to lose.");
        System.out.println("  That is answered with backups and replication, not by splitting the");
        System.out.println("  data into 12 files that cannot answer the first question.");
        System.exit(0);
    }

    static void buildSource(Path p, int good) throws IOException {
        try (BufferedWriter w = Files.newBufferedWriter(p, StandardCharsets.UTF_8)) {
            for (int i = 1; i <= good; i++) {
                w.write(new BusPass(String.format("P%07d", i), String.format("1923%05d", i),
                        "Student " + i, "R" + (i % 20 + 1), "Stop " + (i % 50),
                        LocalDate.parse("2027-03-31"), 400000L + i).toLine());
                w.newLine();
                // injected faults, at known places
                if (i == 1000) w.write("P9000001|1923|Short Reg|R1|Stop 1|2027-03-31|450000\n");
                if (i == 2000) w.write("P9000002|192399999|Bad Amount|R1|Stop 1|2027-03-31|-5\n");
                if (i == 3000) w.write("P9000003|192399998|Too Much|R1|Stop 1|2027-03-31|9900000\n");
                if (i == 4000) w.write("P9000004|192399997|Five Fields|R1|Stop 1\n");
                if (i == 5000) w.write("\n");
                if (i == 6000) w.write("P9000006|192399996|Old Pass|R1|Stop 1|2019-01-01|450000\n");
                if (i == 7000) w.write("P0000001|192399995|Duplicate Id|R1|Stop 1|2027-03-31|450000\n");
                if (i == 8000) w.write("P9000008|192399994|Bad Date|R1|Stop 1|31-03-2027|450000\n");
            }
        }
    }
    static void PassLoaderTruncate() throws SQLException {
        try (Connection c = DriverManager.getConnection(Db.URL, Db.USER, Db.PASS);
             Statement s = c.createStatement()) { s.executeUpdate("DELETE FROM bus_pass"); }
    }
    static int countRows() throws SQLException {
        try (Connection c = DriverManager.getConnection(Db.URL, Db.USER, Db.PASS);
             Statement s = c.createStatement();
             ResultSet rs = s.executeQuery("SELECT COUNT(*) FROM bus_pass")) {
            return rs.next() ? rs.getInt(1) : -1;
        }
    }
    static String isolationName(int level) {
        return switch (level) {
            case Connection.TRANSACTION_READ_UNCOMMITTED -> "READ UNCOMMITTED";
            case Connection.TRANSACTION_READ_COMMITTED   -> "READ COMMITTED";
            case Connection.TRANSACTION_REPEATABLE_READ  -> "REPEATABLE READ";
            case Connection.TRANSACTION_SERIALIZABLE     -> "SERIALIZABLE";
            default -> "none (" + level + ")";
        };
    }
}
