package parking.pricing;

/** The original tariff, now expressed as a rule: a flat rate per started hour. */
public final class HourlyRate implements PricingRule {

    private final long rupeesPerHour;

    public HourlyRate(long rupeesPerHour) {
        if (rupeesPerHour < 0) throw new IllegalArgumentException("rate cannot be negative");
        this.rupeesPerHour = rupeesPerHour;
    }

    @Override public long fee(int entryMinute, int exitMinute) {
        int minutes = PricingRule.duration(entryMinute, exitMinute);
        return (long) PricingRule.startedHours(minutes) * rupeesPerHour;
    }

    @Override public String describe() { return "Rs " + rupeesPerHour + "/started hour"; }
}
