/* Assignment 4 (CO4) Task 1 - thrown when the user's input cannot be accepted.
 * Name: Vignesh S   Register Number: 192325002 */
public class ValidationException extends Exception {
    private final String field;
    public ValidationException(String field, String message) { super(message); this.field = field; }
    public String field() { return field; }
}
