package parking.model;

/**
 * The encapsulation boundary made explicit.
 *
 * Slot's constructor, occupy() and free() are package-private, so no other
 * package can create or mutate a Slot directly.  This class is the ONE public
 * doorway through which parking.service is permitted to do so, which means
 * every change to a slot's occupancy passes through code that parking.model
 * owns and can validate.
 */
public final class SlotFactory {

    private SlotFactory() { }                     // a utility class is never instantiated

    public static Slot create(int floor, int number) {
        if (floor < 0)  throw new IllegalArgumentException("floor cannot be negative");
        if (number < 0) throw new IllegalArgumentException("slot number cannot be negative");
        return new Slot(floor, number);
    }

    public static void occupy(Slot s, String regNo) {
        if (s == null) throw new IllegalArgumentException("slot is mandatory");
        if (regNo == null || regNo.isBlank())
            throw new IllegalArgumentException("registration number is mandatory");
        s.occupy(regNo);
    }

    public static void free(Slot s) {
        if (s == null) throw new IllegalArgumentException("slot is mandatory");
        s.free();
    }
}
