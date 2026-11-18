/* Assignment 4 (CO4) Task 6 - THE DATA-ACCESS LAYER.
 *
 * The only class in Task 6 that mentions SQL.  Every statement is a
 * PreparedStatement and every resource is opened in try-with-resources.
 *
 * Name: Vignesh S   Register Number: 192325002 */
import java.sql.*;
import java.time.LocalDate;
import java.util.*;

public class RegistrationDao {

    private Connection open() throws SQLException {
        return DriverManager.getConnection(Db.URL, Db.USER, Db.PASS);
    }

    public List<Model.CampusEvent> findAllEvents() throws SQLException {
        String sql = "SELECT event_id, name, venue, event_date, capacity FROM campus_event ORDER BY event_date";
        List<Model.CampusEvent> out = new ArrayList<>();
        try (Connection c = open(); PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next())
                out.add(new Model.CampusEvent(rs.getInt(1), rs.getString(2), rs.getString(3),
                        rs.getDate(4).toLocalDate(), rs.getInt(5)));
        }
        return out;
    }

    public boolean alreadyRegistered(int eventId, String regNo) throws SQLException {
        String sql = "SELECT 1 FROM registration WHERE event_id = ? AND reg_no = ?";
        try (Connection c = open(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, eventId); ps.setString(2, regNo);
            try (ResultSet rs = ps.executeQuery()) { return rs.next(); }
        }
    }

    public int countFor(int eventId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM registration WHERE event_id = ?";
        try (Connection c = open(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, eventId);
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? rs.getInt(1) : 0; }
        }
    }

    public long insert(int eventId, String regNo, String name, String email)
            throws SQLException {
        String sql = "INSERT INTO registration(event_id, reg_no, student_name, email) VALUES(?,?,?,?)";
        try (Connection c = open();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, eventId); ps.setString(2, regNo);
            ps.setString(3, name); ps.setString(4, email);
            ps.executeUpdate();
            try (ResultSet k = ps.getGeneratedKeys()) { return k.next() ? k.getLong(1) : -1; }
        }
    }

    /** PAGINATED read - the recommended path.  Holds one page in memory. */
    public List<Model.Registration> findPage(int eventId, int limit, int offset) throws SQLException {
        String sql = "SELECT reg_id, event_id, reg_no, student_name, email, registered_at "
                   + "FROM registration WHERE event_id = ? ORDER BY reg_id LIMIT ? OFFSET ?";
        List<Model.Registration> out = new ArrayList<>(limit);
        try (Connection c = open(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, eventId); ps.setInt(2, limit); ps.setInt(3, offset);
            try (ResultSet rs = ps.executeQuery()) { while (rs.next()) out.add(map(rs)); }
        }
        return out;
    }

    /** LOAD-ALL - kept only so the two can be measured against each other. */
    public List<Model.Registration> findAll(int eventId) throws SQLException {
        String sql = "SELECT reg_id, event_id, reg_no, student_name, email, registered_at "
                   + "FROM registration WHERE event_id = ? ORDER BY reg_id";
        List<Model.Registration> out = new ArrayList<>();
        try (Connection c = open(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, eventId);
            try (ResultSet rs = ps.executeQuery()) { while (rs.next()) out.add(map(rs)); }
        }
        return out;
    }

    private Model.Registration map(ResultSet rs) throws SQLException {
        return new Model.Registration(rs.getLong(1), rs.getInt(2), rs.getString(3),
                rs.getString(4), rs.getString(5), String.valueOf(rs.getTimestamp(6)));
    }

    /** Used by the benchmark to create 100,000 rows quickly. */
    public int bulkInsert(int eventId, int rows, int batchSize) throws SQLException {
        String sql = "INSERT INTO registration(event_id, reg_no, student_name, email) VALUES(?,?,?,?)";
        int done = 0;
        try (Connection c = open()) {
            c.setAutoCommit(false);
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                for (int i = 1; i <= rows; i++) {
                    ps.setInt(1, eventId);
                    ps.setString(2, String.format("1925%05d", i));
                    ps.setString(3, "Student " + i);
                    ps.setString(4, "s" + i + "@campus.example");
                    ps.addBatch();
                    if (i % batchSize == 0) { done += ps.executeBatch().length; }
                }
                done += ps.executeBatch().length;
            }
            c.commit();
        }
        return done;
    }
}
