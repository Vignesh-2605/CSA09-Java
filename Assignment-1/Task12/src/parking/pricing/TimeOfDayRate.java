package parking.pricing;

/**
 * NEW REQUIREMENT: a delivery van pays DOUBLE the car rate between 09:00 and
 * 17:00, and the normal rate outside that window.
 *
 * This is the rule the original design could not express.  It is not a property
 * of the van at all - it is a property of WHEN the van parked - so no amount of
 * subclassing Vehicle could have produced it.  Each started hour is priced
 * according to the window the hour BEGINS in, which is the rule the finance
 * office specified and is also the only reading that is monotonic in duration.
 */
public final class TimeOfDayRate implements PricingRule {

    private final long baseRatePerHour;
    private final double peakMultiplier;
    private final int peakFromMinute;          // inclusive
    private final int peakToMinute;            // exclusive

    public TimeOfDayRate(long baseRatePerHour, double peakMultiplier,
                         int peakFromHour, int peakToHour) {
        if (baseRatePerHour < 0) throw new IllegalArgumentException("rate cannot be negative");
        if (peakMultiplier < 1) throw new IllegalArgumentException("multiplier must be >= 1");
        if (peakFromHour < 0 || peakFromHour > 23 || peakToHour < 1 || peakToHour > 24)
            throw new IllegalArgumentException("peak window must lie inside one day");
        if (peakFromHour >= peakToHour)
            throw new IllegalArgumentException("peak window must start before it ends");
        this.baseRatePerHour = baseRatePerHour;
        this.peakMultiplier = peakMultiplier;
        this.peakFromMinute = peakFromHour * 60;
        this.peakToMinute = peakToHour * 60;
    }

    @Override public long fee(int entryMinute, int exitMinute) {
        int minutes = PricingRule.duration(entryMinute, exitMinute);
        int hours = PricingRule.startedHours(minutes);
        long total = 0;
        for (int h = 0; h < hours; h++) {
            int hourStart = (entryMinute + h * 60) % (24 * 60);
            total += isPeak(hourStart)
                     ? Math.round(baseRatePerHour * peakMultiplier)
                     : baseRatePerHour;
        }
        return total;
    }

    private boolean isPeak(int minuteOfDay) {
        return minuteOfDay >= peakFromMinute && minuteOfDay < peakToMinute;
    }

    /** Exposed for the test table: how many of the charged hours were peak hours. */
    public int peakHoursIn(int entryMinute, int exitMinute) {
        int hours = PricingRule.startedHours(PricingRule.duration(entryMinute, exitMinute));
        int peak = 0;
        for (int h = 0; h < hours; h++)
            if (isPeak((entryMinute + h * 60) % (24 * 60))) peak++;
        return peak;
    }

    @Override public String describe() {
        return String.format("Rs %d/h, x%.0f between %02d:00 and %02d:00",
                             baseRatePerHour, peakMultiplier,
                             peakFromMinute / 60, peakToMinute / 60);
    }
}
