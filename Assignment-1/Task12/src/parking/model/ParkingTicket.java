package parking.model;

/** Carries the day as well as the minute, because a one-day pass needs it. */
public class ParkingTicket {

    private static int lastTicketNo = 1000;

    private final int ticketNo;
    private final Vehicle vehicle;
    private final CampusUser owner;
    private final Slot slot;
    private final int dayOfYear;
    private final int entryMinute;
    private Integer exitMinute;
    private long feeCharged = -1;

    public ParkingTicket(Vehicle vehicle, CampusUser owner, Slot slot,
                         int dayOfYear, int entryMinute) {
        if (vehicle == null) throw new IllegalArgumentException("vehicle is mandatory");
        if (owner == null)   throw new IllegalArgumentException("owner is mandatory");
        if (slot == null)    throw new IllegalArgumentException("slot is mandatory");
        if (dayOfYear < 1 || dayOfYear > 366)
            throw new IllegalArgumentException("day must be 1..366, was " + dayOfYear);
        if (entryMinute < 0 || entryMinute > 1439)
            throw new IllegalArgumentException("entry minute must be 0..1439, was " + entryMinute);
        this.ticketNo = ++lastTicketNo;
        this.vehicle = vehicle; this.owner = owner; this.slot = slot;
        this.dayOfYear = dayOfYear; this.entryMinute = entryMinute;
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
    public int durationMinutes() {
        if (exitMinute == null) return -1;
        int d = exitMinute - entryMinute;
        return d < 0 ? d + 24 * 60 : d;
    }

    public int getTicketNo() { return ticketNo; }
    public Vehicle getVehicle() { return vehicle; }
    public CampusUser getOwner() { return owner; }
    public Slot getSlot() { return slot; }
    public int getDayOfYear() { return dayOfYear; }
    public int getEntryMinute() { return entryMinute; }
    public Integer getExitMinute() { return exitMinute; }
    public long getFeeCharged() { return feeCharged; }
    public static int issued() { return lastTicketNo - 1000; }

    @Override public String toString() {
        return String.format("Ticket#%d %s %s", ticketNo, vehicle.getRegNo(), slot);
    }
}
