package parking.service;

import parking.model.Slot;

/**
 * DEFAULT (package-private) CLASS.
 * SlotGrid is an implementation detail of parking.service: it is not declared
 * public, so parking.app cannot even name the type, let alone reach into the
 * array of slots.  Changing the storage from a jagged array to anything else
 * therefore cannot break application code.
 */
class SlotGrid {

    private final SlotRow[] rows;

    SlotGrid(int[] slotsPerFloor) {
        rows = new SlotRow[slotsPerFloor.length];
        for (int f = 0; f < slotsPerFloor.length; f++) rows[f] = new SlotRow(f, slotsPerFloor[f]);
    }

    Slot firstFreeSlot(String regNo) {
        for (SlotRow row : rows) {
            Slot s = row.firstFree(regNo);
            if (s != null) return s;
        }
        return null;
    }

    void release(Slot s) {
        for (SlotRow row : rows) if (row.release(s)) return;
        throw new IllegalArgumentException("slot " + s + " does not belong to this facility");
    }

    int freeSlots() { int n = 0; for (SlotRow r : rows) n += r.freeCount(); return n; }
    int capacity()  { int n = 0; for (SlotRow r : rows) n += r.size();      return n; }
}
