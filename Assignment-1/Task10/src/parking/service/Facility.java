package parking.service;

import parking.model.ParkingTicket;
import parking.model.Slot;
import parking.model.Vehicle;
import java.util.ArrayList;
import java.util.List;

/**
 * Programming in Java - Assignment 1 (CO1), Task 10
 * Name: Vignesh S   Register Number: 192325002
 *
 * The SERVICE package holds the operations.  It depends on parking.model but
 * parking.model does not depend on it, so the model can be tested on its own.
 *
 * Note what this class CANNOT do, by design: Slot.occupy() and Slot.free() are
 * package-private to parking.model, so the facility cannot write a slot
 * directly.  It must go through the model's own factory method, which is the
 * encapsulation boundary doing real work rather than decorating the code.
 */
public class Facility {

    private final SlotGrid grid;                      // private: the layout is hidden
    private final List<ParkingTicket> active = new ArrayList<>();

    public Facility(int[] slotsPerFloor) {
        if (slotsPerFloor == null || slotsPerFloor.length == 0)
            throw new IllegalArgumentException("a facility needs at least one floor");
        for (int n : slotsPerFloor)
            if (n <= 0) throw new IllegalArgumentException("every floor needs at least one slot");
        this.grid = new SlotGrid(slotsPerFloor);
    }

    /** Admits a vehicle, or returns null when nothing suitable is free. */
    public ParkingTicket admit(Vehicle v, int entryMinute) {
        if (v == null) throw new IllegalArgumentException("vehicle is mandatory");
        for (ParkingTicket t : active)
            if (t.getVehicle().equals(v))
                throw new IllegalStateException(v.getRegNo() + " is already inside on ticket "
                                                + t.getTicketNo());
        Slot free = grid.firstFreeSlot(v.getRegNo());
        if (free == null) return null;
        ParkingTicket t = new ParkingTicket(v, free, entryMinute);
        active.add(t);
        return t;
    }

    /** Closes the ticket, frees the slot and returns the fee. */
    public long release(ParkingTicket t, int exitMinute, boolean hasValidPass) {
        if (t == null) throw new IllegalArgumentException("ticket is mandatory");
        if (!active.remove(t))
            throw new IllegalStateException("ticket " + t.getTicketNo() + " is not active");
        t.setExitMinute(exitMinute);
        grid.release(t.getSlot());
        if (hasValidPass) return 0;
        return t.getVehicle().calculateFee(t.durationMinutes());
    }

    public int vehiclesInside() { return active.size(); }
    public int freeSlots()      { return grid.freeSlots(); }
    public int capacity()       { return grid.capacity(); }
}
