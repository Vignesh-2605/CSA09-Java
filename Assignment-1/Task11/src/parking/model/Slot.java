package parking.model;

/** One numbered place. Occupancy may be changed only from inside parking.model. */
public class Slot {

    private final int floor;
    private final int number;
    private String occupantRegNo;

    Slot(int floor, int number) { this.floor = floor; this.number = number; }

    public boolean isFree() { return occupantRegNo == null; }
    public int getFloor() { return floor; }
    public int getNumber() { return number; }
    public String getOccupantRegNo() { return occupantRegNo; }

    void occupy(String regNo) {
        if (!isFree())
            throw new IllegalStateException("slot " + this + " already holds " + occupantRegNo);
        this.occupantRegNo = regNo;
    }
    void free() {
        if (isFree()) throw new IllegalStateException("slot " + this + " is already free");
        this.occupantRegNo = null;
    }

    @Override public String toString() { return floor + "/" + number; }
}
