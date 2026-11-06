package parking.model;

/**
 * Programming in Java - Assignment 1 (CO1), Task 10
 * Name: Vignesh S   Register Number: 192325002
 *
 * ACCESS MODIFIERS, AND WHY EACH ONE WAS CHOSEN
 *   public class        - the gate application in parking.app must name the type
 *   private regNo       - identity; nothing outside may change it, in any package
 *   protected ctor      - only subclasses create vehicles; the class is abstract
 *   protected abstract  - ratePerHour is an implementation detail of the model
 *                         package; parking.app has no business reading a raw rate
 *   public final fee    - the service package must bill; the RULE must not vary
 */
public abstract class Vehicle {

    private final String regNo;          // private: the strongest protection
    private final int wheels;

    protected Vehicle(String regNo, int wheels) {
        if (regNo == null || regNo.isBlank())
            throw new IllegalArgumentException("registration number is mandatory");
        this.regNo = regNo.trim().toUpperCase();
        this.wheels = wheels;
    }

    /** protected: visible to subclasses and to the rest of parking.model only. */
    protected abstract double ratePerHour();

    /** protected: a default that subclasses may specialise, outsiders may not read. */
    protected int dailyCap() { return 200; }

    /** public final: every caller may bill, no caller may redefine the rule. */
    public final long calculateFee(int minutes) {
        if (minutes < 0) throw new IllegalArgumentException("duration cannot be negative");
        long fee = (long) ((minutes + 59) / 60) * (long) ratePerHour();
        return Math.min(fee, dailyCap());
    }

    public String getRegNo() { return regNo; }   // getter only: there is no setter
    public int getWheels()   { return wheels; }

    @Override public boolean equals(Object o) {
        return o instanceof Vehicle && regNo.equals(((Vehicle) o).regNo);
    }
    @Override public int hashCode() { return regNo.hashCode(); }
    @Override public String toString() { return getClass().getSimpleName() + "(" + regNo + ")"; }
}
