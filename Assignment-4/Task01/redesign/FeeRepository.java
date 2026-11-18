/* Assignment 4 (CO4) Task 1 - the ONLY class that knows SQL exists.
 * Name: Vignesh S   Register Number: 192325002 */
import java.sql.*;

public class FeeRepository {
    private final String url, user, password;
    public FeeRepository(String url, String user, String password) {
        this.url = url; this.user = user; this.password = password;
    }

    /** Returns the generated receipt number. */
    public long save(Payment p) throws SQLException {
        String sql = "INSERT INTO fee_payment(reg_no, fee_type, amount_paise) VALUES(?,?,?)";
        // try-with-resources: both the connection and the statement are closed
        // on every path, including the one where executeUpdate throws.
        try (Connection con = DriverManager.getConnection(url, user, password);
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            // Parameters are BOUND, not concatenated.  The driver sends the
            // SQL and the values separately, so a value can never be parsed
            // as SQL however it is spelled.
            ps.setString(1, p.registerNumber());
            ps.setString(2, p.feeType());
            ps.setLong(3, p.amountPaise());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                return keys.next() ? keys.getLong(1) : -1;
            }
        }
    }

    public Payment findLatest(String registerNumber) throws SQLException {
        String sql = "SELECT reg_no, fee_type, amount_paise FROM fee_payment "
                   + "WHERE reg_no = ? ORDER BY id DESC LIMIT 1";
        try (Connection con = DriverManager.getConnection(url, user, password);
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, registerNumber);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                return new Payment(rs.getString(1), rs.getString(2), rs.getLong(3));
            }
        }
    }
}
