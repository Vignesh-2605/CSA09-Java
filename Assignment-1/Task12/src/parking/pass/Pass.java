package parking.pass;

/**
 * Programming in Java - Assignment 1 (CO1), Task 12
 * Name: Vignesh S   Register Number: 192325002
 *
 * NEW REQUIREMENT: visitors may now buy a ONE-DAY pass.
 *
 * The original design encoded entitlement as a boolean 'hasMonthlyPass' plus a
 * rule in UserCategory saying a visitor's pass never counts.  A one-day pass
 * breaks that, because it is a visitor pass that DOES count - but only on the
 * day it was bought.  Entitlement therefore stops being a flag and becomes a
 * small object that can be asked a question.
 */
public interface Pass {

    /** Is this pass valid for a stay beginning on this day at this minute? */
    boolean coversStay(int dayOfYear, int entryMinute);

    /** What the holder pays for the pass itself, charged once when it is sold. */
    long priceWhenSold();

    String describe();

    /** The null object: no pass at all. Avoids a null check at every call site. */
    Pass NONE = new Pass() {
        @Override public boolean coversStay(int dayOfYear, int entryMinute) { return false; }
        @Override public long priceWhenSold() { return 0; }
        @Override public String describe() { return "no pass"; }
    };
}
