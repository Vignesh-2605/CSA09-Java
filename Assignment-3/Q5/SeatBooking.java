/*
 * Programming in Java  -  Assignment 3 (CO3)
 * Q5  -  Race Conditions in a Cinema Seat-Booking System  ("Complete the Code")
 *
 * Name            : Vignesh S
 * Register Number : 192325002
 * Primary responsibility for this file: Student B (synchronization comparison)
 *
 * The given skeleton is unchanged apart from TODO 1.
 * Q5/unsynchronized/SeatBookingUnsafe.java is the SAME program with TODO 1
 * written WITHOUT synchronized, kept so the 10-run comparison the brief asks
 * for can be reproduced rather than described.
 */
public class SeatBooking {
    private final boolean[] booked;
    private int bookingsMade = 0;

    SeatBooking(int seats) { booked = new boolean[seats]; }

    // TODO 1: Make the check-then-book operation atomic so that a seat can never be
    //   sold twice
    //
    // The critical section is the whole check-then-act sequence: reading
    // booked[seat], deciding, writing booked[seat] and incrementing
    // bookingsMade.  Synchronizing the method makes those four steps one
    // indivisible unit under the object's intrinsic lock, so no second thread
    // can observe booked[seat] as false after the first has decided to take it.
    //
    // bookingsMade must be inside the same lock as the array write.  Guarding
    // only the array would still lose increments, because ++ is itself a
    // read-modify-write.
    public synchronized boolean book(int seat) {
        if (booked[seat]) return false;      // already sold
        booked[seat] = true;                 // take it
        bookingsMade++;                      // count it
        return true;
    }

    public static void main(String[] args) throws InterruptedException {
        SeatBooking screen = new SeatBooking(10);
        Thread[] users = new Thread[100];
        for (int i = 0; i < users.length; i++) {
            final int seat = i % 10;                     // 10 users compete for every seat
            users[i] = new Thread(() -> screen.book(seat));
            users[i].start();
        }
        for (Thread t : users) t.join();
        System.out.println("Seats sold: " + screen.bookingsMade);
    }
}
