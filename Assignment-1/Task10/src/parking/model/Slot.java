package parking.model;

/**
 * DEFAULT (package-private) CLASS-LEVEL MEMBERS.
 * The class is public because the service package returns slot positions, but
 * occupy() and free() are package-private: only parking.model may change a
 * slot's occupancy, so no application code can mark a slot free behind the
 * facility's back.
 */
public class Slot {

    private final int floor;
    private final int number;
    private String occupantRegNo;              // null => free

    Slot(int floor, int number) {              // package-private constructor
        this.floor = floor; this.number = number;
    }

    public boolean isFree() { return occupantRegNo == null; }
    public int getFloor()   { return floor; }
    public int getNumber()  { return number; }
    public String getOccupantRegNo() { return occupantRegNo; }

    /** package-private: callable from Facility, invisible to parking.app. */
    void occupy(String regNo) {
        if (!isFree())
            throw new IllegalStateException("slot " + this + " already holds " + occupantRegNo);
        this.occupantRegNo = regNo;
    }

    /** package-private for the same reason. */
    void free() { this.occupantRegNo = null; }

    @Override public String toString() { return floor + "/" + number; }
}
