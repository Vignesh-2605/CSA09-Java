package parking.model;

/** The one public doorway for creating and mutating a Slot. */
public final class SlotFactory {

    private SlotFactory() { }

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
