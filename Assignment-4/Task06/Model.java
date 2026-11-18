/* Assignment 4 (CO4) Task 6 - the data, as immutable records.
 * Name: Vignesh S   Register Number: 192325002 */
import java.time.LocalDate;

public final class Model {
    public record CampusEvent(int id, String name, String venue, LocalDate date, int capacity) {
        @Override public String toString() { return name + "  (" + date + ")"; }
    }
    public record Registration(long regId, int eventId, String regNo,
                               String studentName, String email, String registeredAt) { }
    private Model() { }
}
