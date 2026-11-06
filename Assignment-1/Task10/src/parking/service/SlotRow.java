package parking.service;

import parking.model.Slot;
import parking.model.SlotFactory;

/** One floor. Package-private: nothing outside parking.service may touch it. */
class SlotRow {

    private final Slot[] slots;

    SlotRow(int floor, int count) {
        slots = new Slot[count];
        for (int i = 0; i < count; i++) slots[i] = SlotFactory.create(floor, i);
    }

    Slot firstFree(String regNo) {
        for (Slot s : slots)
            if (s.isFree()) { SlotFactory.occupy(s, regNo); return s; }
        return null;
    }

    boolean release(Slot s) {
        for (Slot mine : slots)
            if (mine == s) { SlotFactory.free(mine); return true; }
        return false;
    }

    int freeCount() { int n = 0; for (Slot s : slots) if (s.isFree()) n++; return n; }
    int size() { return slots.length; }
}
