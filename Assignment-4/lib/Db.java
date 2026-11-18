/* Assignment 4 (CO4) / Assignment 5 (CO5) - one place for the test-database
 * connection details, so no task hard-codes them in a handler.
 *
 * The database is a LOCAL test instance created only for this assignment, and
 * the account below has rights on that one schema and nothing else.
 *
 * Name: Vignesh S   Register Number: 192325002 */
public final class Db {
    public static final String URL  = "jdbc:mysql://127.0.0.1:3306/csa09_test";
    public static final String USER = "csa09";
    public static final String PASS = "csa09pass";
    private Db() { }
}
