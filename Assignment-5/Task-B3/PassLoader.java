/*
 * Programming in Java  -  Assignment 5 (CO5)
 * Task B3  -  moving bus-pass records into MySQL.  COMPLETE THE CODE: TODO 1-3.
 *
 * PROVENANCE: the deck supplied to me is the summary version without the
 * skeleton, so this file reconstructs it from the task description - three
 * insertion strategies (Statement, PreparedStatement, PreparedStatement
 * batches of 1,000), auto-commit against one transaction, and a deliberate
 * failure halfway.  TODO 1 to TODO 3 are placed where the task describes them
 * and all three are completed.
 *
 * Name: Vignesh S   Register Number: 192325002
 */
import java.sql.*;
import java.time.LocalDate;
import java.util.*;

public class PassLoader {

    public static final int BATCH_SIZE = 1000;

    static Connection open() throws SQLException {
        return DriverManager.getConnection(Db.URL, Db.USER, Db.PASS);
    }

    public static void truncate() throws SQLException {
        try (Connection c = open(); Statement s = c.createStatement()) {
            s.executeUpdate("DELETE FROM bus_pass");
        }
    }

    public static int count() throws SQLException {
        try (Connection c = open(); Statement s = c.createStatement();
             ResultSet rs = s.executeQuery("SELECT COUNT(*) FROM bus_pass")) {
            return rs.next() ? rs.getInt(1) : -1;
        }
    }

    /* =================== TODO 1 - COMPLETED =================== */
    /**
     * Insert with a plain Statement: the SQL text is rebuilt for every row,
     * so the server must parse and plan each one separately.
     *
     * This is also the form that is open to SQL injection - a name containing
     * an apostrophe would break the statement, which is why the value has to
     * be escaped by hand here.  Task B2 shows what happens when it is not.
     */
    public static long insertWithStatement(List<BusPass> passes) throws SQLException {
        long t0 = System.nanoTime();
        try (Connection c = open(); Statement st = c.createStatement()) {
            for (BusPass p : passes) {
                String sql = "INSERT INTO bus_pass VALUES('" + p.passId() + "','"
                        + p.registerNo() + "','" + p.name().replace("'", "''") + "','"
                        + p.routeNo() + "','" + p.boardingStop().replace("'", "''") + "','"
                        + p.validUntil() + "'," + p.amountPaise() + ")";
                st.executeUpdate(sql);
            }
        }
        return System.nanoTime() - t0;
    }

    /* =================== TODO 2 - COMPLETED =================== */
    /**
     * Insert with a PreparedStatement: the statement is parsed ONCE and the
     * values are bound for each row.  One round trip per row, but no reparse.
     */
    public static long insertWithPrepared(List<BusPass> passes) throws SQLException {
        String sql = "INSERT INTO bus_pass VALUES(?,?,?,?,?,?,?)";
        long t0 = System.nanoTime();
        try (Connection c = open(); PreparedStatement ps = c.prepareStatement(sql)) {
            for (BusPass p : passes) {
                bind(ps, p);
                ps.executeUpdate();
            }
        }
        return System.nanoTime() - t0;
    }

    /* =================== TODO 3 - COMPLETED =================== */
    /**
     * Insert with PreparedStatement BATCHES of 1,000 rows, inside ONE
     * transaction.  Parsed once, and one round trip per 1,000 rows instead of
     * one per row - which is where the time actually goes.
     */
    public static long insertWithBatch(List<BusPass> passes, int batchSize) throws SQLException {
        String sql = "INSERT INTO bus_pass VALUES(?,?,?,?,?,?,?)";
        long t0 = System.nanoTime();
        try (Connection c = open()) {
            c.setAutoCommit(false);                       // one transaction
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                int n = 0;
                for (BusPass p : passes) {
                    bind(ps, p);
                    ps.addBatch();
                    if (++n % batchSize == 0) ps.executeBatch();
                }
                ps.executeBatch();                        // the remainder
            }
            c.commit();
        }
        return System.nanoTime() - t0;
    }

    /** Batches WITHOUT a transaction, so auto-commit can be compared fairly. */
    public static long insertWithBatchAutoCommit(List<BusPass> passes, int batchSize) throws SQLException {
        String sql = "INSERT INTO bus_pass VALUES(?,?,?,?,?,?,?)";
        long t0 = System.nanoTime();
        try (Connection c = open()) {
            c.setAutoCommit(true);
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                int n = 0;
                for (BusPass p : passes) {
                    bind(ps, p);
                    ps.addBatch();
                    if (++n % batchSize == 0) ps.executeBatch();
                }
                ps.executeBatch();
            }
        }
        return System.nanoTime() - t0;
    }

    private static void bind(PreparedStatement ps, BusPass p) throws SQLException {
        ps.setString(1, p.passId());
        ps.setString(2, p.registerNo());
        ps.setString(3, p.name());
        ps.setString(4, p.routeNo());
        ps.setString(5, p.boardingStop());
        ps.setDate(6, java.sql.Date.valueOf(p.validUntil()));
        ps.setLong(7, p.amountPaise());
    }

    /** Generates n passes; if badIndex >= 0 that pass carries an invalid amount. */
    public static List<BusPass> generate(int n, int badIndex) {
        List<BusPass> out = new ArrayList<>(n);
        for (int i = 1; i <= n; i++) {
            long amount = (i - 1 == badIndex) ? -1L : 400000L + i;   // -1 violates chk_amount
            out.add(new BusPass(String.format("P%07d", i), String.format("1923%05d", i),
                    "Student " + i, "R" + (i % 20 + 1), "Stop " + (i % 50),
                    LocalDate.parse("2027-03-31"), amount));
        }
        return out;
    }
}
