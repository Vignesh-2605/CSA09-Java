package parking.service;

import parking.model.Slot;

/** The jagged slot store: one SlotRow per floor, each sized independently. */
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
    int floors()    { return rows.length; }
}
