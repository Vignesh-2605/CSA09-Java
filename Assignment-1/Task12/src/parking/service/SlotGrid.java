package parking.service;

import parking.model.Slot;
import parking.model.SlotFactory;
import parking.model.VehicleCategory;

/**
 * THE JAGGED ARRAY, UNCHANGED IN SHAPE.
 *
 * The facility grows from 3 floors to 5 with a DIFFERENT number of slots on
 * each.  This class absorbs that by being handed a longer int[]: there is no
 * code change at all, which is the point the Task 2 analysis anticipated.
 *
 * What IS new is slot sizing, because a delivery van does not fit a bike bay.
 */
class SlotGrid {

    private final Slot[][] slots;            // jagged: one row per floor

    SlotGrid(int[] slotsPerFloor, int[] unitsPerFloor) {
        slots = new Slot[slotsPerFloor.length][];
        for (int f = 0; f < slotsPerFloor.length; f++) {
            slots[f] = new Slot[slotsPerFloor[f]];
            for (int s = 0; s < slotsPerFloor[f]; s++)
                slots[f][s] = SlotFactory.create(f, s, unitsPerFloor[f]);
        }
    }

    /** First free slot that FITS the category, searched floor by floor. */
    Slot firstFreeSlot(VehicleCategory c, String regNo) {
        for (Slot[] row : slots) {
            if (row.length == 0 || !row[0].fits(c)) continue;   // whole floor is too small
            for (Slot s : row)
                if (s.isFree()) { SlotFactory.occupy(s, regNo); return s; }
        }
        return null;
    }

    void release(Slot target) {
        for (Slot[] row : slots)
            for (Slot s : row)
                if (s == target) { SlotFactory.free(s); return; }
        throw new IllegalArgumentException("slot " + target + " does not belong to this facility");
    }

    int freeSlots() {
        int n = 0;
        for (Slot[] row : slots) for (Slot s : row) if (s.isFree()) n++;
        return n;
    }
    int capacity() { int n = 0; for (Slot[] row : slots) n += row.length; return n; }
    int floors() { return slots.length; }
    int slotsOnFloor(int f) { return slots[f].length; }
    int unitsOnFloor(int f) { return slots[f].length == 0 ? 0 : slots[f][0].getUnits(); }
}
