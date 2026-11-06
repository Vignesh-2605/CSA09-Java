package parking.pricing;

/**
 * A DECORATOR: applies a daily maximum on top of any other rule.
 *
 * The cap used to be an overridable method on Vehicle, which meant every new
 * vehicle type had to remember to set one.  As a wrapper it is applied once, at
 * the point the tariff is assembled, and it composes with any rule - including
 * rules written in the future, which is what "extend without modifying" means
 * in practice.
 */
public final class CappedRule implements PricingRule {

    private final PricingRule inner;
    private final long dailyCap;

    public CappedRule(PricingRule inner, long dailyCap) {
        if (inner == null) throw new IllegalArgumentException("a rule to cap is mandatory");
        if (dailyCap < 0) throw new IllegalArgumentException("cap cannot be negative");
        this.inner = inner;
        this.dailyCap = dailyCap;
    }

    @Override public long fee(int entryMinute, int exitMinute) {
        return Math.min(inner.fee(entryMinute, exitMinute), dailyCap);
    }

    @Override public String describe() {
        return inner.describe() + ", capped at Rs " + dailyCap + "/day";
    }
}
