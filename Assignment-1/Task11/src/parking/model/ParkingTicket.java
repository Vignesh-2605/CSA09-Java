package parking.model;

/** The proof of parking. Read-only once issued, apart from the exit time. */
public class ParkingTicket {

    private static int lastTicketNo = 1000;          // class-wide; never reused

    private final int ticketNo;
    private final Vehicle vehicle;
    private final CampusUser owner;
    private final Slot slot;
    private final int entryMinute;
    private Integer exitMinute;                      // null => still inside
    private long feeCharged = -1;
    private final boolean duplicate;

    public ParkingTicket(Vehicle vehicle, CampusUser owner, Slot slot, int entryMinute) {
        if (vehicle == null) throw new IllegalArgumentException("vehicle is mandatory");
        if (owner == null)   throw new IllegalArgumentException("owner is mandatory");
        if (slot == null)    throw new IllegalArgumentException("slot is mandatory");
        if (entryMinute < 0 || entryMinute > 1439)
            throw new IllegalArgumentException("entry minute must be 0..1439, was " + entryMinute);
        this.ticketNo = ++lastTicketNo;
        this.vehicle = vehicle; this.owner = owner; this.slot = slot;
        this.entryMinute = entryMinute; this.duplicate = false;
    }

    /** Copy constructor: re-issues a lost ticket with a NEW number. */
    public ParkingTicket(ParkingTicket other) {
        if (other == null) throw new IllegalArgumentException("nothing to copy");
        this.ticketNo = ++lastTicketNo;
        this.vehicle = other.vehicle; this.owner = other.owner; this.slot = other.slot;
        this.entryMinute = other.entryMinute; this.exitMinute = other.exitMinute;
        this.feeCharged = other.feeCharged; this.duplicate = true;
    }

    public void setExitMinute(int m) {
        if (m < 0 || m > 1439)
            throw new IllegalArgumentException("exit minute must be 0..1439, was " + m);
        if (exitMinute != null)
            throw new IllegalStateException("ticket " + ticketNo + " is already closed");
        this.exitMinute = m;
    }

    public void setFeeCharged(long fee) {
        if (fee < 0) throw new IllegalArgumentException("fee cannot be negative");
        this.feeCharged = fee;
    }

    public boolean isOpen() { return exitMinute == null; }

    /** The stay in whole minutes, handling a stay that crossed midnight. */
    public int durationMinutes() {
        if (exitMinute == null) return -1;
        int d = exitMinute - entryMinute;
        return d < 0 ? d + 24 * 60 : d;
    }

    public int getTicketNo() { return ticketNo; }
    public Vehicle getVehicle() { return vehicle; }
    public CampusUser getOwner() { return owner; }
    public Slot getSlot() { return slot; }
    public int getEntryMinute() { return entryMinute; }
    public Integer getExitMinute() { return exitMinute; }
    public long getFeeCharged() { return feeCharged; }
    public boolean isDuplicate() { return duplicate; }
    public static int issued() { return lastTicketNo - 1000; }

    @Override public String toString() {
        return String.format("Ticket#%d %s %s %s", ticketNo, vehicle.getRegNo(), slot,
                             isOpen() ? "(inside)" : "(" + durationMinutes() + " min, Rs "
                                                     + feeCharged + ")");
    }
}
