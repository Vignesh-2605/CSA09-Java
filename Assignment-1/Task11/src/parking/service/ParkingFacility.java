package parking.service;

import parking.model.CampusUser;
import parking.model.Chargeable;
import parking.model.ParkingTicket;
import parking.model.Slot;
import parking.model.Vehicle;
import java.util.ArrayList;
import java.util.List;

/**
 * Programming in Java - Assignment 1 (CO1), Task 11
 * Name: Vignesh S   Register Number: 192325002
 *
 * The complete system. Entry allocates a slot and issues a ticket; exit frees
 * the slot and computes the fee. Every business rule is enforced here or in the
 * model, never left to the caller.
 */
public class ParkingFacility {

    private final String code;
    private final SlotGrid grid;
    private final List<ParkingTicket> active = new ArrayList<>();
    private long collected = 0;
    private int refusedFull = 0;

    public ParkingFacility(String code, int[] slotsPerFloor) {
        if (code == null || code.isBlank())
            throw new IllegalArgumentException("facility code is mandatory");
        if (slotsPerFloor == null || slotsPerFloor.length == 0)
            throw new IllegalArgumentException("a facility needs at least one floor");
        for (int n : slotsPerFloor)
            if (n <= 0) throw new IllegalArgumentException("every floor needs at least one slot");
        this.code = code;
        this.grid = new SlotGrid(slotsPerFloor);
    }

    /** Admits a vehicle. Returns the ticket, or null when the facility is full. */
    public ParkingTicket admit(Vehicle v, CampusUser owner, int entryMinute) {
        if (v == null)     throw new IllegalArgumentException("vehicle is mandatory");
        if (owner == null) throw new IllegalArgumentException("owner is mandatory");
        for (ParkingTicket t : active)
            if (t.getVehicle().equals(v))
                throw new IllegalStateException(v.getRegNo()
                        + " is already inside on ticket " + t.getTicketNo());
        Slot free = grid.firstFreeSlot(v.getRegNo());
        if (free == null) { refusedFull++; return null; }
        ParkingTicket t = new ParkingTicket(v, owner, free, entryMinute);
        active.add(t);
        return t;
    }

    /** Closes the ticket, frees the slot, computes and records the fee. */
    public long release(ParkingTicket t, int exitMinute) { return release(t, exitMinute, 0); }

    /** Exit with charging: kWh drawn is billed on top of the parking fee. */
    public long release(ParkingTicket t, int exitMinute, int kWhDrawn) {
        if (t == null) throw new IllegalArgumentException("ticket is mandatory");
        if (!active.contains(t))
            throw new IllegalStateException("ticket " + t.getTicketNo()
                    + " is not an active ticket of " + code);
        t.setExitMinute(exitMinute);
        long fee = 0;
        if (t.getOwner().paysFee()) fee = t.getVehicle().calculateFee(t.durationMinutes());
        if (kWhDrawn > 0) {
            if (!(t.getVehicle() instanceof Chargeable))
                throw new IllegalArgumentException(t.getVehicle().getRegNo()
                        + " cannot draw power: it is not chargeable");
            fee += (long) ((Chargeable) t.getVehicle()).chargingFee(kWhDrawn);
        }
        t.setFeeCharged(fee);
        grid.release(t.getSlot());
        active.remove(t);
        collected += fee;
        return fee;
    }

    /** Re-issues a lost ticket: same stay, new number, original stays invalid. */
    public ParkingTicket reissue(ParkingTicket lost) {
        if (!active.contains(lost))
            throw new IllegalStateException("ticket " + lost.getTicketNo() + " is not active");
        ParkingTicket copy = new ParkingTicket(lost);
        active.remove(lost);
        active.add(copy);
        return copy;
    }

    public String getCode() { return code; }
    public int vehiclesInside() { return active.size(); }
    public int freeSlots() { return grid.freeSlots(); }
    public int capacity() { return grid.capacity(); }
    public int floors() { return grid.floors(); }
    public long totalCollected() { return collected; }
    public int refusedBecauseFull() { return refusedFull; }
    public List<ParkingTicket> activeTickets() { return List.copyOf(active); }
}
