package parking.model;

/** Who the vehicle belongs to. Decides whether a monthly pass exempts the fee. */
public enum UserCategory {
    STUDENT(true), STAFF(true), VISITOR(false);

    private final boolean passExempts;
    UserCategory(boolean passExempts) { this.passExempts = passExempts; }

    /** A visitor's pass never exempts the fee, however it was obtained. */
    public boolean passExempts() { return passExempts; }
}
