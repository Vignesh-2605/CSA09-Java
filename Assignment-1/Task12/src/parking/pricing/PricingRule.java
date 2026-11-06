package parking.pricing;

/**
 * Programming in Java - Assignment 1 (CO1), Task 12
 * Name: Vignesh S   Register Number: 192325002
 *
 * THE KEY MOVE OF THE REDESIGN.
 *
 * In the original design the tariff was a rate returned by an overridden
 * method, so "what it costs" was welded to "what kind of vehicle it is".  The
 * delivery van breaks that: its rate depends on the TIME OF DAY, not on the
 * vehicle.  A rate is therefore no longer enough, and the whole fee calculation
 * becomes a strategy object that a vehicle HAS, rather than behaviour a vehicle
 * IS.
 *
 * This is an interface and not an abstract class because a pricing rule holds
 * no shared state and composes - a capped rule WRAPS another rule.
 */
public interface PricingRule {

    /**
     * @param entryMinute minutes from midnight at entry, 0..1439
     * @param exitMinute  minutes from midnight at exit, 0..1439 (may wrap past midnight)
     * @return whole rupees payable for the parking itself
     */
    long fee(int entryMinute, int exitMinute);

    /** A human-readable description, used in the receipt and the test tables. */
    String describe();

    /** Shared helper: whole minutes of a stay, handling a wrap past midnight. */
    static int duration(int entryMinute, int exitMinute) {
        if (entryMinute < 0 || entryMinute > 1439 || exitMinute < 0 || exitMinute > 1439)
            throw new IllegalArgumentException("minutes must be 0..1439");
        int d = exitMinute - entryMinute;
        return d < 0 ? d + 24 * 60 : d;
    }

    /** Shared helper: every started hour is charged in full, integer arithmetic only. */
    static int startedHours(int minutes) {
        if (minutes < 0) throw new IllegalArgumentException("negative duration");
        return (minutes + 59) / 60;
    }
}
