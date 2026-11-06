package parking.model;

/** The owner of a vehicle. */
public class CampusUser {

    private final String name;
    private final UserCategory category;
    private final boolean hasMonthlyPass;

    public CampusUser(String name, UserCategory category, boolean hasMonthlyPass) {
        if (name == null || name.isBlank())
            throw new IllegalArgumentException("user name is mandatory");
        if (category == null)
            throw new IllegalArgumentException("user category is mandatory");
        this.name = name.trim();
        this.category = category;
        this.hasMonthlyPass = hasMonthlyPass;
    }

    /** The business rule: staff and students with a pass park free; visitors pay. */
    public boolean paysFee() { return !(hasMonthlyPass && category.passExempts()); }

    public String getName() { return name; }
    public UserCategory getCategory() { return category; }
    public boolean hasMonthlyPass() { return hasMonthlyPass; }

    @Override public String toString() {
        return name + " (" + category + (hasMonthlyPass ? ", pass" : "") + ")";
    }
}
