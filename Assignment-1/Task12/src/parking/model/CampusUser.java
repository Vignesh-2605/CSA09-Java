package parking.model;

import parking.pass.Pass;

/**
 * THE REVISED CampusUser.
 *
 * Before: a boolean hasMonthlyPass, plus a rule in UserCategory saying a
 * visitor's pass never counts - which made a one-day visitor pass impossible to
 * express without changing the enum.
 *
 * After: the user HAS-A Pass, and the pass itself answers whether it covers the
 * stay.  Adding the one-day pass therefore added a class and changed nothing.
 */
public class CampusUser {

    private final String name;
    private final UserCategory category;
    private final Pass pass;

    public CampusUser(String name, UserCategory category, Pass pass) {
        if (name == null || name.isBlank())
            throw new IllegalArgumentException("user name is mandatory");
        if (category == null) throw new IllegalArgumentException("user category is mandatory");
        this.name = name.trim();
        this.category = category;
        this.pass = pass == null ? Pass.NONE : pass;
    }

    /** Free parking only when a pass actually covers THIS stay. */
    public boolean paysFee(int dayOfYear, int entryMinute) {
        return !pass.coversStay(dayOfYear, entryMinute);
    }

    public String getName() { return name; }
    public UserCategory getCategory() { return category; }
    public Pass getPass() { return pass; }

    @Override public String toString() { return name + " (" + category + ", " + pass.describe() + ")"; }
}
