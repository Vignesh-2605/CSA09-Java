package parking.service;

import parking.model.*;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Programming in Java - Assignment 1 (CO1), Task 12
 * Name: Vignesh S   Register Number: 192325002
 *
 * The revised facility.  Two changes from Task 11:
 *   1. the active-ticket store is the LinkedHashMap that Task 11's measurements
 *      recommended - O(1) exit lookup, entry order preserved for the report;
 *   2. admit() passes the vehicle's CATEGORY to the grid, so slot fitting works.
 *
 * The public method signatures gained a dayOfYear parameter, which the one-day
 * pass needs.  That is the one API change the new requirements forced.
 */
public class ParkingFacility {

    private final String code;
    private final SlotGrid grid;
    private final Map<String, ParkingTicket> open = new LinkedHashMap<>();
    private long collected = 0;
    private int refusedFull = 0, refusedNoFit = 0;

    public ParkingFacility(String code, int[] slotsPerFloor, int[] unitsPerFloor) {
        if (code == null || code.isBlank())
            throw new IllegalArgumentException("facility code is mandatory");
        if (slotsPerFloor == null || slotsPerFloor.length == 0)
            throw new IllegalArgumentException("a facility needs at least one floor");
        if (unitsPerFloor == null || unitsPerFloor.length != slotsPerFloor.length)
            throw new IllegalArgumentException("one slot size must be given per floor");
        for (int n : slotsPerFloor)
            if (n <= 0) throw new IllegalArgumentException("every floor needs at least one slot");
        this.code = code;
        this.grid = new SlotGrid(slotsPerFloor, unitsPerFloor);
    }

    public ParkingTicket admit(Vehicle v, CampusUser owner, int dayOfYear, int entryMinute) {
        if (v == null)     throw new IllegalArgumentException("vehicle is mandatory");
        if (owner == null) throw new IllegalArgumentException("owner is mandatory");
        if (open.containsKey(v.getRegNo()))                         // O(1), was O(n)
            throw new IllegalStateException(v.getRegNo() + " is already inside on ticket "
                    + open.get(v.getRegNo()).getTicketNo());
        Slot free = grid.firstFreeSlot(v.getCategory(), v.getRegNo());
        if (free == null) {
            if (grid.freeSlots() == 0) refusedFull++; else refusedNoFit++;
            return null;
        }
        ParkingTicket t = new ParkingTicket(v, owner, free, dayOfYear, entryMinute);
        open.put(v.getRegNo(), t);
        return t;
    }

    public long release(String regNo, int exitMinute) { return release(regNo, exitMinute, 0); }

    public long release(String regNo, int exitMinute, int kWhDrawn) {
        if (regNo == null) throw new IllegalArgumentException("registration number is mandatory");
        ParkingTicket t = open.get(regNo.trim().toUpperCase());     // O(1), was O(n)
        if (t == null) throw new IllegalStateException(regNo + " has no open ticket at " + code);
        t.setExitMinute(exitMinute);

        long fee = 0;
        if (t.getOwner().paysFee(t.getDayOfYear(), t.getEntryMinute()))
            fee = t.getVehicle().parkingFee(t.getEntryMinute(), exitMinute);
        if (kWhDrawn > 0) {
            if (!(t.getVehicle() instanceof Chargeable))
                throw new IllegalArgumentException(t.getVehicle().getRegNo()
                        + " cannot draw power: it is not chargeable");
            fee += (long) ((Chargeable) t.getVehicle()).chargingFee(kWhDrawn);
        }
        t.setFeeCharged(fee);
        grid.release(t.getSlot());
        open.remove(t.getVehicle().getRegNo());
        collected += fee;
        return fee;
    }

    /** Selling a pass is revenue too, and it is recorded in the same total. */
    public long sellPass(CampusUser u) {
        long price = u.getPass().priceWhenSold();
        collected += price;
        return price;
    }

    public String getCode() { return code; }
    public int vehiclesInside() { return open.size(); }
    public int freeSlots() { return grid.freeSlots(); }
    public int capacity() { return grid.capacity(); }
    public int floors() { return grid.floors(); }
    public int slotsOnFloor(int f) { return grid.slotsOnFloor(f); }
    public int unitsOnFloor(int f) { return grid.unitsOnFloor(f); }
    public long totalCollected() { return collected; }
    public int refusedBecauseFull() { return refusedFull; }
    public int refusedBecauseNoFit() { return refusedNoFit; }
    public Map<String, ParkingTicket> openTickets() { return Map.copyOf(open); }
    public Iterable<ParkingTicket> inEntryOrder() { return open.values(); }
}
