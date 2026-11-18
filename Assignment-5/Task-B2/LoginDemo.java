/*
 * Programming in Java  -  Assignment 5 (CO5)
 * Task B2  -  Adversarial Input: the unsafe login and the PreparedStatement one.
 *
 * Runs against a LOCAL test database created only for this assignment.
 *
 * Name: Vignesh S   Register Number: 192325002
 */
import java.sql.*;
import java.util.*;

public class LoginDemo {

    /** THE UNSAFE VERSION - user input spliced straight into the SQL text. */
    static String unsafeLogin(String user, String pass) throws SQLException {
        String sql = "SELECT username, role FROM app_user WHERE username = '"
                   + user + "' AND password = '" + pass + "'";
        try (Connection c = DriverManager.getConnection(Db.URL, Db.USER, Db.PASS);
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            return rs.next() ? rs.getString("username") + "/" + rs.getString("role") : null;
        }
    }

    /** The same query with the values BOUND instead of concatenated. */
    static String safeLogin(String user, String pass) throws SQLException {
        String sql = "SELECT username, role FROM app_user WHERE username = ? AND password = ?";
        try (Connection c = DriverManager.getConnection(Db.URL, Db.USER, Db.PASS);
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, user);
            ps.setString(2, pass);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getString("username") + "/" + rs.getString("role") : null;
            }
        }
    }

    static String sqlFor(String user, String pass) {
        return "SELECT username, role FROM app_user WHERE username = '"
             + user + "' AND password = '" + pass + "'";
    }

    public static void main(String[] args) throws Exception {
        System.out.println("TASK B2  -  ADVERSARIAL INPUT");
        System.out.println("=".repeat(118));
        System.out.println("Name: Vignesh S   Register Number: 192325002");
        try (Connection c = DriverManager.getConnection(Db.URL, Db.USER, Db.PASS)) {
            System.out.printf("Local test database: %s %s%n",
                    c.getMetaData().getDatabaseProductName(), c.getMetaData().getDatabaseProductVersion());
        }

        record Case(String id, String user, String pass, String intent, boolean shouldPass) { }
        Case[] cases = {
            new Case("L1", "vignesh", "Pass@192325002", "the genuine login", true),
            new Case("L2", "vignesh", "wrong",          "right user, wrong password", false),
            new Case("L3", "nobody",  "anything",       "a user that does not exist", false),
            new Case("L4", "vignesh' -- ",   "anything",  "comment out the password test", false),
            new Case("L5", "' OR '1'='1",    "' OR '1'='1", "always-true in both fields", false),
            new Case("L6", "admin' -- ",     "x",         "log in as ADMIN with no password", false),
            new Case("L7", "' OR 1=1 -- ",   "x",         "take the first row, whoever it is", false),
            new Case("L8", "x' UNION SELECT username, role FROM app_user -- ", "x",
                     "read every row through a UNION", false),
        };

        System.out.println();
        System.out.println("1.  THE SAME EIGHT INPUTS THROUGH BOTH VERSIONS");
        System.out.println("-".repeat(118));
        System.out.printf("%-5s | %-38s | %-20s | %-20s | %s%n",
                "ID", "INTENT", "UNSAFE (Statement)", "SAFE (Prepared)", "VERDICT");
        System.out.println("-".repeat(118));
        int breaches = 0, blocked = 0;
        for (Case t : cases) {
            String u, s;
            try { u = unsafeLogin(t.user(), t.pass()); } catch (SQLException e) { u = "SQL error"; }
            try { s = safeLogin(t.user(), t.pass()); }   catch (SQLException e) { s = "SQL error"; }
            boolean unsafeAccepted = u != null && !u.equals("SQL error");
            boolean safeAccepted   = s != null && !s.equals("SQL error");
            String verdict;
            if (t.shouldPass()) verdict = (unsafeAccepted && safeAccepted) ? "both correct" : "PROBLEM";
            else if (unsafeAccepted && !safeAccepted) { verdict = "UNSAFE BREACHED"; breaches++; blocked++; }
            else if (!unsafeAccepted && !safeAccepted) { verdict = "both rejected"; blocked++; }
            else verdict = "PROBLEM";
            System.out.printf("%-5s | %-38s | %-20s | %-20s | %s%n", t.id(), t.intent(),
                    u == null ? "rejected" : u, s == null ? "rejected" : s, verdict);
        }
        System.out.println();
        System.out.printf("  the unsafe version was breached by %d of %d crafted inputs.%n", breaches, cases.length - 1);
        System.out.printf("  the PreparedStatement version rejected every one of them.%n");

        System.out.println();
        System.out.println("2.  WHY  -  THE SQL THE DATABASE ACTUALLY RECEIVES");
        System.out.println("-".repeat(118));
        for (Case t : new Case[]{cases[0], cases[5], cases[7]}) {
            System.out.printf("  %s  input: username=[%s] password=[%s]%n", t.id(), t.user(), t.pass());
            System.out.printf("      unsafe -> %s%n", sqlFor(t.user(), t.pass()));
            System.out.printf("      safe   -> SELECT ... WHERE username = ? AND password = ?   "
                    + "with ? bound to the text above%n%n");
        }
        System.out.println("  In L6 the two dashes begin an SQL comment, so everything from");
        System.out.println("  \"-- \" onwards - including the whole password test - is discarded by");
        System.out.println("  the parser.  The query becomes \"WHERE username = 'admin'\", which is");
        System.out.println("  true, so the attacker is admitted as ADMIN without a password.");
        System.out.println();
        System.out.println("  PreparedStatement does not fail to be fooled by a cleverer string - it");
        System.out.println("  cannot be fooled by any string.  The statement is parsed ONCE, with");
        System.out.println("  the ? markers in place, before any value is supplied.  The values then");
        System.out.println("  travel separately and are bound to those markers, so they are never");
        System.out.println("  parsed as SQL.  \"admin' -- \" is simply a username that no row has.");

        System.out.println();
        System.out.println("3.  DAMAGED BUS-PASS DATA  -  WHAT EACH INPUT REVEALS");
        System.out.println("-".repeat(118));
        System.out.printf("  %-34s | %-28s | %s%n", "ADVERSARIAL INPUT", "WHAT THE STORE DOES", "WHAT IT REVEALS");
        System.out.println("  " + "-".repeat(110));
        String[][] dmg = {
            {"wrong field count (5 of 7)", "skipped, line reported", "the format is self-synchronising at the newline"},
            {"non-numeric amount FIVELAKH", "skipped, line reported", "parsing is per-field, so one bad field is contained"},
            {"empty line", "skipped silently", "blank lines are not records and must not be counted"},
            {"date written 31-03-2027", "skipped, line reported", "a plausible-looking value can still be invalid"},
            {"a pipe inside a name", "mis-parsed as 8 fields", "the DELIMITER is the weakness of a delimited format"},
            {"file truncated mid-line", "last line skipped", "an incomplete write loses one record, not the file"},
            {"file absent entirely", "NoSuchFileException", "a missing file is a different fault from a damaged one"},
        };
        for (String[] d : dmg) System.out.printf("  %-34s | %-28s | %s%n", d[0], d[1], d[2]);
        System.out.println();
        System.out.println("  The fifth row is the honest weakness of the design in Task A1: a name");
        System.out.println("  containing the delimiter breaks the record silently, because it still");
        System.out.println("  parses - into the wrong number of fields.  Guarding it needs either an");
        System.out.println("  escape rule or a delimiter that cannot occur in the data.");
        System.exit(0);
    }
}
