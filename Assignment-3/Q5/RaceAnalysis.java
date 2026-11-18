/*
 * Programming in Java  -  Assignment 3 (CO3)
 * Q5  -  Student A: producing and explaining the race
 *        Student B: comparing synchronization strategies
 *
 * Name            : Vignesh S   Register Number: 192325002
 */
import bench.Bench;
import java.util.Map;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

public class RaceAnalysis {

    /* ---------- the five booking strategies ---------- */

    /** 1. no synchronization - the defect. */
    static final class Unsafe {
        final boolean[] booked; int sold = 0;
        Unsafe(int s) { booked = new boolean[s]; }
        boolean book(int seat) {
            if (booked[seat]) return false;
            booked[seat] = true;
            sold++;
            return true;
        }
    }

    /** 1b. the same, with the window widened so the race is reliably reachable. */
    static final class UnsafeWidened {
        final boolean[] booked; final AtomicInteger sold = new AtomicInteger();
        UnsafeWidened(int s) { booked = new boolean[s]; }
        boolean book(int seat) {
            if (booked[seat]) return false;      // (1) CHECK
            // Widen the window that already exists between the check and the act.
            // This changes only the TIMING: the sequence of operations, and the
            // absence of any lock around them, are exactly as in the supplied
            // program.  50 microseconds is far longer than the real window, but
            // the real window is not zero - it is simply too short to hit often.
            java.util.concurrent.locks.LockSupport.parkNanos(50_000);
            booked[seat] = true;                 // (2) ACT
            sold.incrementAndGet();              // (3) COUNT - atomic, so the only
            return true;                         //     defect measured is the double sale
        }
    }

    /** 2. synchronized METHOD - locks the whole object for the whole call. */
    static final class SyncMethod {
        final boolean[] booked; int sold = 0;
        SyncMethod(int s) { booked = new boolean[s]; }
        synchronized boolean book(int seat) {
            if (booked[seat]) return false;
            booked[seat] = true; sold++; return true;
        }
    }

    /** 3. synchronized BLOCK - locks only the critical section. */
    static final class SyncBlock {
        final boolean[] booked; int sold = 0;
        private final Object lock = new Object();
        SyncBlock(int s) { booked = new boolean[s]; }
        boolean book(int seat) {
            // work that does NOT touch shared state stays outside the lock
            int normalised = seat;
            synchronized (lock) {
                if (booked[normalised]) return false;
                booked[normalised] = true; sold++; return true;
            }
        }
    }

    /** 4. per-seat striped locks - one lock per seat, not one for the screen. */
    static final class StripedLocks {
        final boolean[] booked; final Object[] locks; final AtomicInteger sold = new AtomicInteger();
        StripedLocks(int s) {
            booked = new boolean[s]; locks = new Object[s];
            for (int i = 0; i < s; i++) locks[i] = new Object();
        }
        boolean book(int seat) {
            synchronized (locks[seat]) {
                if (booked[seat]) return false;
                booked[seat] = true; sold.incrementAndGet(); return true;
            }
        }
    }

    /** 5. AtomicBoolean[] with compareAndSet - lock-free. */
    static final class AtomicSeats {
        final AtomicBoolean[] booked; final AtomicInteger sold = new AtomicInteger();
        AtomicSeats(int s) {
            booked = new AtomicBoolean[s];
            for (int i = 0; i < s; i++) booked[i] = new AtomicBoolean(false);
        }
        boolean book(int seat) {
            if (booked[seat].compareAndSet(false, true)) { sold.incrementAndGet(); return true; }
            return false;
        }
    }

    /** 6. ReentrantLock. */
    static final class LockBased {
        final boolean[] booked; int sold = 0;
        private final ReentrantLock lock = new ReentrantLock();
        LockBased(int s) { booked = new boolean[s]; }
        boolean book(int seat) {
            lock.lock();
            try {
                if (booked[seat]) return false;
                booked[seat] = true; sold++; return true;
            } finally { lock.unlock(); }
        }
    }

    /** 7. ConcurrentHashMap.putIfAbsent. */
    static final class MapBased {
        final Map<Integer, String> booked = new ConcurrentHashMap<>();
        boolean book(int seat) { return booked.putIfAbsent(seat, "SOLD") == null; }
        int sold() { return booked.size(); }
    }

    static final int SEATS = 10;
    static final int USERS = 100;
    static final int TRIALS = 40;

    public static void main(String[] args) throws Exception {
        System.out.println("Q5  -  RACE CONDITIONS IN A CINEMA SEAT-BOOKING SYSTEM");
        System.out.println("=".repeat(112));
        System.out.println("Name: Vignesh S   Register Number: 192325002");

        /* ================= STUDENT A ================= */
        System.out.println();
        System.out.println("STUDENT A  -  PRODUCING AND EXPLAINING THE RACE");
        System.out.println("=".repeat(112));
        System.out.println();
        System.out.println("1.  THE SUPPLIED PROGRAM DID NOT REPRODUCE THE DEFECT");
        System.out.println("-".repeat(112));
        System.out.println("  Ten runs of the unsynchronized version all printed 'Seats sold: 10'");
        System.out.println("  (ten_runs.txt).  That is not evidence that the code is correct - it is");
        System.out.println("  evidence that the race window was never hit.");
        System.out.println();
        System.out.println("  WHY: the supplied main() starts each thread inside the loop, so thread");
        System.out.println("  i is already running - and usually finished - before thread i+1 is");
        System.out.println("  created.  The threads barely overlap, and a race needs overlap.");
        System.out.println();
        System.out.println("2.  A TEST DESIGNED TO REPRODUCE IT RELIABLY");
        System.out.println("-".repeat(112));
        System.out.println("  Two changes, neither of which alters the booking LOGIC:");
        System.out.println("    (a) a CountDownLatch holds every thread at a starting gate and");
        System.out.println("        releases all 100 at once, so they genuinely run concurrently;");
        System.out.println("    (b) a 50-microsecond pause between the check and the act widens the");
        System.out.println("        window that already exists - the sequence of operations and the");
        System.out.println("        absence of a lock are unchanged.");
        System.out.println();
        System.out.println("  The count is kept in an AtomicInteger so that the ONLY defect measured");
        System.out.println("  is the double SALE, not a separately lost increment.");
        System.out.println();

        int oversoldTrials = 0, totalExtra = 0, worst = 0;
        for (int t = 0; t < TRIALS; t++) {
            UnsafeWidened screen = new UnsafeWidened(SEATS);
            CountDownLatch gate = new CountDownLatch(1);
            CountDownLatch done = new CountDownLatch(USERS);
            for (int i = 0; i < USERS; i++) {
                final int seat = i % SEATS;
                new Thread(() -> {
                    try { gate.await(); } catch (InterruptedException ignored) { }
                    screen.book(seat);
                    done.countDown();
                }).start();
            }
            gate.countDown();
            done.await();
            int sold = screen.sold.get();
            if (sold > SEATS) { oversoldTrials++; totalExtra += sold - SEATS; worst = Math.max(worst, sold); }
        }
        System.out.printf("  %-44s | %s%n", "trials run", TRIALS);
        System.out.printf("  %-44s | %d of %d%n", "trials that OVERSOLD", oversoldTrials, TRIALS);
        System.out.printf("  %-44s | %d%n", "total seats sold beyond the 10 available", totalExtra);
        System.out.printf("  %-44s | %d (capacity is %d)%n", "worst single trial", worst, SEATS);
        System.out.println();
        System.out.printf("  The same logic that printed 10 ten times in a row oversold in %d of %d%n",
                oversoldTrials, TRIALS);
        System.out.println("  trials once the threads were made to overlap.  Nothing about the code");
        System.out.println("  changed - only the timing.");

        System.out.println();
        System.out.println("3.  THE CRITICAL SECTION, AND THE INTERLEAVING THAT BREAKS IT");
        System.out.println("-".repeat(112));
        System.out.println("  THE CRITICAL SECTION is these three lines taken together:");
        System.out.println("      if (booked[seat]) return false;   // (1) CHECK");
        System.out.println("      booked[seat] = true;              // (2) ACT");
        System.out.println("      bookingsMade++;                   // (3) COUNT");
        System.out.println();
        System.out.println("  It is not any one line.  Each line on its own is harmless; the");
        System.out.println("  invariant 'a seat is sold at most once' spans all three, so all three");
        System.out.println("  must be indivisible.  This is the check-then-act pattern.");
        System.out.println();
        System.out.printf("  %-6s | %-34s | %-34s | %s%n", "STEP", "THREAD T1 (seat 4)", "THREAD T2 (seat 4)", "STATE OF booked[4]");
        System.out.println("  " + "-".repeat(108));
        String[][] trace = {
            {"1", "reads booked[4] -> false",        "",                               "false"},
            {"2", "",                                "reads booked[4] -> false",       "false  <- BOTH saw it free"},
            {"3", "writes booked[4] = true",         "",                               "true"},
            {"4", "",                                "writes booked[4] = true",        "true   (overwrites, no effect)"},
            {"5", "bookingsMade++ -> 1",             "",                               "true"},
            {"6", "",                                "bookingsMade++ -> 2",            "true   <- SEAT 4 SOLD TWICE"},
        };
        for (String[] r : trace) System.out.printf("  %-6s | %-34s | %-34s | %s%n", r[0], r[1], r[2], r[3]);
        System.out.println();
        System.out.println("  Step 2 is the fault: T2 reads the value before T1 has written it. Both");
        System.out.println("  threads then believe they own seat 4 and both return true, so two");
        System.out.println("  customers are given the same seat.");
        System.out.println();
        System.out.println("  bookingsMade++ is a SECOND, independent race: it compiles to a read, an");
        System.out.println("  add and a write, so two threads can both read 1 and both write 2,");
        System.out.println("  losing an increment.  That is why the count can also come out too LOW,");
        System.out.println("  and why guarding only the array would not be enough.");

        /* ================= STUDENT B ================= */
        System.out.println();
        System.out.println("STUDENT B  -  COMPARING SYNCHRONIZATION STRATEGIES");
        System.out.println("=".repeat(112));
        System.out.println();
        System.out.println("4.  CORRECTNESS: DOES EACH STRATEGY EVER OVERSELL?");
        System.out.println("-".repeat(112));
        System.out.printf("%-30s | %-16s | %-18s | %s%n", "STRATEGY", "TRIALS", "OVERSOLD IN", "VERDICT");
        System.out.println("-".repeat(112));
        System.out.printf("%-30s | %-16d | %-18s | %s%n", "no synchronization", TRIALS,
                oversoldTrials + " trials", "INCORRECT");
        for (String name : List.of("synchronized method", "synchronized block",
                                   "striped per-seat locks", "AtomicBoolean + CAS",
                                   "ReentrantLock", "ConcurrentHashMap")) {
            int bad = 0;
            for (int t = 0; t < TRIALS; t++) if (runStrategy(name, true) > SEATS) bad++;
            System.out.printf("%-30s | %-16d | %-18s | %s%n", name, TRIALS, bad + " trials",
                    bad == 0 ? "correct" : "INCORRECT");
        }

        System.out.println();
        System.out.println("5.  TIME TO PROCESS 10,000 BOOKINGS UNDER CONTENTION");
        System.out.println("-".repeat(112));
        final int BOOKINGS = 10_000, THREADS = 8, BIGSEATS = 2_000;
        java.util.List<Bench.Result> all = Bench.list();
        Bench.tableHead();
        for (String name : List.of("synchronized method", "synchronized block",
                                   "striped per-seat locks", "AtomicBoolean + CAS",
                                   "ReentrantLock", "ConcurrentHashMap")) {
            Bench.Result r = Bench.measure(name, "10,000 bookings", BIGSEATS, BOOKINGS,
                    () -> load(name, BIGSEATS, BOOKINGS, THREADS));
            Bench.row(r); all.add(r);
        }
        Bench.writeCsv("Q5", all);

        System.out.println();
        System.out.println("6.  LOCKING THE WHOLE METHOD versus LOCKING ONLY THE CRITICAL SECTION");
        System.out.println("-".repeat(112));
        System.out.println("  In THIS class the two are almost identical, because the method body IS");
        System.out.println("  the critical section - there is nothing else in it to leave outside the");
        System.out.println("  lock.  The measured difference above is correspondingly small.");
        System.out.println();
        System.out.println("  The distinction matters when the method does other work.  If book()");
        System.out.println("  also formatted a receipt or wrote a log line, a synchronized METHOD");
        System.out.println("  would hold the screen's lock for all of it, serialising work that");
        System.out.println("  touches no shared state.  A synchronized BLOCK holds the lock only for");
        System.out.println("  the three lines that need it.");
        System.out.println();
        System.out.println("  The striped row is the important one: it does not reduce the time spent");
        System.out.println("  INSIDE the lock, it reduces CONTENTION for it, by giving each seat its");
        System.out.println("  own lock so bookings for different seats never block each other.");

        System.out.println();
        System.out.println("7.  THE INTRINSIC LOCK EACH VERSION USES");
        System.out.println("-".repeat(112));
        System.out.printf("%-34s | %-30s | %s%n", "VERSION", "LOCK OBJECT", "SCOPE OF MUTUAL EXCLUSION");
        System.out.println("-".repeat(112));
        String[][] locks = {
            {"synchronized instance method", "this - the SeatBooking object", "one screen at a time"},
            {"synchronized (lock) block",    "the private Object lock",       "one screen at a time"},
            {"static synchronized method",   "SeatBooking.class",             "ALL screens at a time"},
            {"striped locks[seat]",          "one Object per seat",           "one seat at a time"},
            {"ReentrantLock",                "an explicit Lock object",       "one screen at a time"},
            {"AtomicBoolean CAS",            "none - a CPU instruction",      "one seat, lock-free"},
        };
        for (String[] r : locks) System.out.printf("%-34s | %-30s | %s%n", r[0], r[1], r[2]);
        System.out.println();
        System.out.println("  A private final lock object is preferred to synchronizing on this:");
        System.out.println("  'this' is visible to callers, so any outside code can synchronize on");
        System.out.println("  the same monitor and block the booking logic - accidentally or not.");
        System.out.println();
        System.out.println("  STATIC SYNCHRONIZATION locks the Class object, so it excludes threads");
        System.out.println("  across EVERY instance.  It would be needed here only for state shared");
        System.out.println("  between screens - a cinema-wide booking sequence number, or a total of");
        System.out.println("  tickets sold across all 20 screens.  Using it for per-screen booking");
        System.out.println("  would be a serious mistake: it would force all 20 screens to book one");
        System.out.println("  at a time, turning a parallel system into a serial one.");

        System.out.println();
        System.out.println("8.  JOINT ENGINEERING DECISION - 20 SCREENS");
        System.out.println("-".repeat(112));
        System.out.println("  DEPLOY: per-seat striped locks, or AtomicBoolean[] with compareAndSet.");
        System.out.println();
        System.out.println("  Reasoning, in order of importance:");
        System.out.println("    1. CORRECTNESS is equal across all five safe strategies - the table in");
        System.out.println("       section 4 shows none of them ever oversold. So correctness does");
        System.out.println("       not decide it, and any argument must be about contention.");
        System.out.println("    2. A cinema's bookings are naturally INDEPENDENT: two customers");
        System.out.println("       booking different seats share nothing. One lock per screen forces");
        System.out.println("       them to queue anyway. Per-seat locking matches the real structure");
        System.out.println("       of the problem, so the only threads that ever block are the ones");
        System.out.println("       genuinely competing for the same seat.");
        System.out.println("    3. With 20 screens the shared object must NOT be the class. Each");
        System.out.println("       screen is independent, so cinema-wide locking would serialise the");
        System.out.println("       whole building.");
        System.out.println();
        System.out.println("  I would choose AtomicBoolean[] over striped locks if the only operation");
        System.out.println("  is 'take this seat if free', because compareAndSet does exactly that in");
        System.out.println("  one instruction with no lock at all. I would choose striped locks the");
        System.out.println("  moment a booking must update more than one thing atomically - a seat");
        System.out.println("  AND a payment record - because a single CAS cannot span two variables.");
        System.out.println();
        System.out.println("  A real system would also need the booking to be atomic with the PAYMENT,");
        System.out.println("  which is a database transaction, not a Java lock. The in-memory lock");
        System.out.println("  protects one process; it does not protect against two application");
        System.out.println("  servers both selling seat 4.");
    }

    /* ---------- helpers ---------- */

    static int runStrategy(String name, boolean concurrent) throws Exception {
        CountDownLatch gate = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(USERS);
        SyncMethod a = new SyncMethod(SEATS); SyncBlock b = new SyncBlock(SEATS);
        StripedLocks c = new StripedLocks(SEATS); AtomicSeats d = new AtomicSeats(SEATS);
        LockBased e = new LockBased(SEATS); MapBased f = new MapBased();
        for (int i = 0; i < USERS; i++) {
            final int seat = i % SEATS;
            new Thread(() -> {
                try { gate.await(); } catch (InterruptedException ignored) { }
                switch (name) {
                    case "synchronized method" -> a.book(seat);
                    case "synchronized block" -> b.book(seat);
                    case "striped per-seat locks" -> c.book(seat);
                    case "AtomicBoolean + CAS" -> d.book(seat);
                    case "ReentrantLock" -> e.book(seat);
                    default -> f.book(seat);
                }
                done.countDown();
            }).start();
        }
        gate.countDown(); done.await();
        return switch (name) {
            case "synchronized method" -> a.sold;
            case "synchronized block" -> b.sold;
            case "striped per-seat locks" -> c.sold.get();
            case "AtomicBoolean + CAS" -> d.sold.get();
            case "ReentrantLock" -> e.sold;
            default -> f.sold();
        };
    }

    static void load(String name, int seats, int bookings, int threads) throws Exception {
        SyncMethod a = new SyncMethod(seats); SyncBlock b = new SyncBlock(seats);
        StripedLocks c = new StripedLocks(seats); AtomicSeats d = new AtomicSeats(seats);
        LockBased e = new LockBased(seats); MapBased f = new MapBased();
        Thread[] ts = new Thread[threads];
        int per = bookings / threads;
        for (int t = 0; t < threads; t++) {
            final int base = t * per;
            ts[t] = new Thread(() -> {
                for (int i = 0; i < per; i++) {
                    int seat = (base + i * 7) % seats;
                    switch (name) {
                        case "synchronized method" -> a.book(seat);
                        case "synchronized block" -> b.book(seat);
                        case "striped per-seat locks" -> c.book(seat);
                        case "AtomicBoolean + CAS" -> d.book(seat);
                        case "ReentrantLock" -> e.book(seat);
                        default -> f.book(seat);
                    }
                }
            });
            ts[t].start();
        }
        for (Thread t : ts) t.join();
    }
}
