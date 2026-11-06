package parking.model;

public final class SlotFactory {
    private SlotFactory() { }

    public static Slot create(int floor, int number, int units) {
        if (floor < 0)  throw new IllegalArgumentException("floor cannot be negative");
        if (number < 0) throw new IllegalArgumentException("slot number cannot be negative");
        if (units < 1)  throw new IllegalArgumentException("a slot must hold at least 1 unit");
        return new Slot(floor, number, units);
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
