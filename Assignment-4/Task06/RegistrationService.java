/* Assignment 4 (CO4) Task 6 - THE SERVICE LAYER: every rule, no Swing, no SQL.
 * Testable headlessly, exactly as in Task 1.
 * Name: Vignesh S   Register Number: 192325002 */
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.util.List;

public class RegistrationService {
    private final RegistrationDao dao;
    public RegistrationService(RegistrationDao dao) { this.dao = dao; }

    public List<Model.CampusEvent> events() throws SQLException { return dao.findAllEvents(); }

    public void validate(Model.CampusEvent event, String regNo, String name, String email)
            throws RegistrationException {
        if (event == null)
            throw new RegistrationException(RegistrationException.Reason.NO_SUCH_EVENT, "event", "Choose an event");
        if (regNo == null || !regNo.trim().matches("\\d{9}"))
            throw new RegistrationException(RegistrationException.Reason.INVALID_INPUT, "regNo",
                    "Register number must be exactly 9 digits");
        if (name == null || !name.trim().matches("[A-Za-z .]{2,60}"))
            throw new RegistrationException(RegistrationException.Reason.INVALID_INPUT, "name",
                    "Name may contain letters, spaces and dots only");
        if (email == null || !email.trim().matches("[\\w.+-]+@[\\w-]+\\.[\\w.]{2,}"))
            throw new RegistrationException(RegistrationException.Reason.INVALID_INPUT, "email",
                    "Enter a valid email address");
    }

    /** Validates, checks the duplicate and the capacity, then inserts. */
    public long register(Model.CampusEvent event, String regNo, String name, String email)
            throws RegistrationException {
        validate(event, regNo, name, email);
        String r = regNo.trim();
        try {
            if (dao.alreadyRegistered(event.id(), r))
                throw new RegistrationException(RegistrationException.Reason.DUPLICATE, "regNo",
                        r + " is already registered for " + event.name());
            if (dao.countFor(event.id()) >= event.capacity())
                throw new RegistrationException(RegistrationException.Reason.EVENT_FULL, "event",
                        event.name() + " is full (" + event.capacity() + " places)");
            return dao.insert(event.id(), r, name.trim(), email.trim());
        } catch (SQLIntegrityConstraintViolationException e) {
            // The application check above lost a race with another user.  The
            // UNIQUE key caught it, and the user still gets the right message.
            throw new RegistrationException(RegistrationException.Reason.DUPLICATE, "regNo",
                    r + " is already registered for " + event.name() + " (caught by the database)");
        } catch (SQLException e) {
            throw new RegistrationException(RegistrationException.Reason.STORAGE, "event",
                    "Could not save the registration: " + e.getMessage());
        }
    }

    public List<Model.Registration> page(int eventId, int pageNo, int pageSize) throws SQLException {
        return dao.findPage(eventId, pageSize, pageNo * pageSize);
    }
    public List<Model.Registration> all(int eventId) throws SQLException { return dao.findAll(eventId); }
    public int count(int eventId) throws SQLException { return dao.countFor(eventId); }
}
