package parking.model;

/**
 * VALIDATING GETTERS AND SETTERS, AND WRAPPER CLASSES.
 *
 * Every field is private.  The only mutable property is the exit time, and its
 * setter rejects a time that precedes entry - so an impossible stay cannot be
 * recorded, however it is called.
 *
 * exitMinute is the WRAPPER type Integer rather than int, because "has not left
 * yet" is genuinely absent, not zero: minute 0 is a real time (midnight).
 */
public class ParkingTicket {

    private static int lastTicketNo = 1000;

    private final int ticketNo;
    private final Vehicle vehicle;
    private final Slot slot;
    private final int entryMinute;
    private Integer exitMinute;                 // null => still inside

    public ParkingTicket(Vehicle vehicle, Slot slot, int entryMinute) {
        if (vehicle == null) throw new IllegalArgumentException("vehicle is mandatory");
        if (slot == null)    throw new IllegalArgumentException("slot is mandatory");
        setEntryMinuteChecked(entryMinute);
        this.ticketNo = ++lastTicketNo;
        this.vehicle = vehicle;
        this.slot = slot;
        this.entryMinute = entryMinute;
    }

    private void setEntryMinuteChecked(int m) {
        if (m < 0 || m > 1439)
            throw new IllegalArgumentException("entry minute must be 0..1439, was " + m);
    }

    /** VALIDATING SETTER - the only mutator on the class. */
    public void setExitMinute(int m) {
        if (m < 0 || m > 1439)
            throw new IllegalArgumentException("exit minute must be 0..1439, was " + m);
        if (m < entryMinute)
            throw new IllegalArgumentException("exit (" + m + ") cannot precede entry ("
                                              + entryMinute + ")");
        if (exitMinute != null)
            throw new IllegalStateException("ticket " + ticketNo + " is already closed");
        this.exitMinute = m;
    }

    public boolean isOpen() { return exitMinute == null; }

    /** Returns the duration, or -1 while the vehicle is still inside. */
    public int durationMinutes() {
        if (exitMinute == null) return -1;
        return exitMinute - entryMinute;        // unboxing happens here, safely
    }

    public int getTicketNo()  { return ticketNo; }
    public Vehicle getVehicle() { return vehicle; }
    public Slot getSlot()     { return slot; }
    public int getEntryMinute() { return entryMinute; }
    public Integer getExitMinute() { return exitMinute; }   // may legitimately be null

    public static int issued() { return lastTicketNo - 1000; }

    @Override public String toString() {
        return String.format("Ticket#%d %s at %s %s", ticketNo, vehicle.getRegNo(), slot,
                             isOpen() ? "(inside)" : "(" + durationMinutes() + " min)");
    }
}
