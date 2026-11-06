package parking.model;

/**
 * Programming in Java - Assignment 1 (CO1), Task 11
 * Name: Vignesh S   Register Number: 192325002
 *
 * The abstract superclass. Holds the state every vehicle has and writes the
 * billing ALGORITHM once; only the rate and the daily cap vary by subclass.
 */
public abstract class Vehicle {

    public static final int MINUTES_PER_HOUR = 60;

    private final String regNo;
    private final int wheels;

    protected Vehicle(String regNo, int wheels) {
        if (regNo == null || regNo.isBlank())
            throw new IllegalArgumentException("registration number is mandatory");
        this.regNo = regNo.trim().toUpperCase();
        this.wheels = wheels;
    }

    protected abstract double ratePerHour();
    protected int dailyCap() { return 200; }

    /** Every started hour is charged in full; the daily maximum is never exceeded. */
    public final long calculateFee(int minutes) {
        if (minutes < 0) throw new IllegalArgumentException("duration cannot be negative");
        int hours = (minutes + MINUTES_PER_HOUR - 1) / MINUTES_PER_HOUR;   // integer ceiling
        long fee = (long) hours * (long) ratePerHour();
        return Math.min(fee, dailyCap());
    }

    /** Overload: the clock times are known, the duration is not. */
    public final long calculateFee(int entryMinute, int exitMinute) {
        int d = exitMinute - entryMinute;
        if (d < 0) d += 24 * 60;                      // the stay crossed midnight
        return calculateFee(d);
    }

    public String getRegNo() { return regNo; }
    public int getWheels() { return wheels; }
    public double rateForReport() { return ratePerHour(); }
    public int capForReport() { return dailyCap(); }

    @Override public boolean equals(Object o) {
        return o instanceof Vehicle && regNo.equals(((Vehicle) o).regNo);
    }
    @Override public int hashCode() { return regNo.hashCode(); }
    @Override public String toString() { return getClass().getSimpleName() + "(" + regNo + ")"; }
}
