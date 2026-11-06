package parking.pricing;

/** NEW REQUIREMENT: e-bikes are charged a flat fee, whatever the duration. */
public final class FlatFee implements PricingRule {

    private final long rupees;

    public FlatFee(long rupees) {
        if (rupees < 0) throw new IllegalArgumentException("fee cannot be negative");
        this.rupees = rupees;
    }

    @Override public long fee(int entryMinute, int exitMinute) {
        int minutes = PricingRule.duration(entryMinute, exitMinute);
        return minutes == 0 ? 0 : rupees;        // a drive-through is still free
    }

    @Override public String describe() { return "flat Rs " + rupees + " per stay"; }
}
