package parking.model;

import parking.pricing.PricingRule;

/**
 * Programming in Java - Assignment 1 (CO1), Task 12
 * Name: Vignesh S   Register Number: 192325002
 *
 * THE REVISED Vehicle.
 *
 * It is still an abstract class, and the hierarchy is still here - that part of
 * the original design is RETAINED, because identity, the number-plate invariant
 * and the validating constructor all still belong in one superclass.
 *
 * What CHANGED is one line: the abstract ratePerHour() is gone, replaced by a
 * composed PricingRule.  calculateFee() is still final, so the guarantee that
 * no subclass can redefine the billing rule survives the redesign intact.
 */
public abstract class Vehicle {

    private final String regNo;
    private final VehicleCategory category;
    private final PricingRule pricing;            // HAS-A, not IS-A

    protected Vehicle(String regNo, VehicleCategory category, PricingRule pricing) {
        if (regNo == null || regNo.isBlank())
            throw new IllegalArgumentException("registration number is mandatory");
        if (category == null) throw new IllegalArgumentException("category is mandatory");
        if (pricing == null)  throw new IllegalArgumentException("a pricing rule is mandatory");
        this.regNo = regNo.trim().toUpperCase();
        this.category = category;
        this.pricing = pricing;
    }

    /** final: the rule for WHICH rule applies cannot be overridden either. */
    public final long parkingFee(int entryMinute, int exitMinute) {
        return pricing.fee(entryMinute, exitMinute);
    }

    public final String tariffDescription() { return pricing.describe(); }

    public String getRegNo() { return regNo; }
    public VehicleCategory getCategory() { return category; }

    @Override public boolean equals(Object o) {
        return o instanceof Vehicle && regNo.equals(((Vehicle) o).regNo);
    }
    @Override public int hashCode() { return regNo.hashCode(); }
    @Override public String toString() { return getClass().getSimpleName() + "(" + regNo + ")"; }
}
