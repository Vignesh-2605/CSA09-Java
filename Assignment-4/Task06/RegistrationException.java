/* Assignment 4 (CO4) Task 6 - a rejected registration, with the reason the UI
 * must show and the field it belongs to.
 * Name: Vignesh S   Register Number: 192325002 */
public class RegistrationException extends Exception {
    public enum Reason { INVALID_INPUT, DUPLICATE, EVENT_FULL, NO_SUCH_EVENT, STORAGE }
    private final Reason reason; private final String field;
    public RegistrationException(Reason reason, String field, String message) {
        super(message); this.reason = reason; this.field = field;
    }
    public Reason reason() { return reason; }
    public String field()  { return field; }
}
