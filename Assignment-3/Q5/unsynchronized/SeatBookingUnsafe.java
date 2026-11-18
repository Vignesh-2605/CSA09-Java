/*
 * Programming in Java  -  Assignment 3 (CO3)
 * Q5  -  the SAME program with TODO 1 written WITHOUT synchronized.
 *
 * Name            : Vignesh S
 * Register Number : 192325002
 *
 * The brief asks for this version to be run first and the result recorded over
 * 10 runs.  Keeping it as a separate file means the two results can be compared
 * directly instead of described from memory.
 */
public class SeatBookingUnsafe {
    private final boolean[] booked;
    private int bookingsMade = 0;

    SeatBookingUnsafe(int seats) { booked = new boolean[seats]; }

    /** TODO 1 deliberately written WITHOUT synchronized: the check and the
     *  book are two separate steps that another thread can interleave with. */
    public boolean book(int seat) {
        if (booked[seat]) return false;      // (1) check
        booked[seat] = true;                 // (2) act
        bookingsMade++;                      // (3) count - itself a read-modify-write
        return true;
    }

    public static void main(String[] args) throws InterruptedException {
        SeatBookingUnsafe screen = new SeatBookingUnsafe(10);
        Thread[] users = new Thread[100];
        for (int i = 0; i < users.length; i++) {
            final int seat = i % 10;
            users[i] = new Thread(() -> screen.book(seat));
            users[i].start();
        }
        for (Thread t : users) t.join();
        System.out.println("Seats sold: " + screen.bookingsMade);
    }
}
