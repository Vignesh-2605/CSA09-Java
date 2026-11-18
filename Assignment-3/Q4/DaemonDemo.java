/*
 * Programming in Java  -  Assignment 3 (CO3)
 * Q4, Student B  -  one station converted into a daemon thread
 *
 * Run as:   java DaemonDemo daemon     (station 4 is a daemon)
 *           java DaemonDemo user       (station 4 is an ordinary thread)
 *
 * Name: Vignesh S   Register Number: 192325002
 */
public class DaemonDemo {
    public static void main(String[] args) throws Exception {
        boolean asDaemon = args.length > 0 && args[0].equals("daemon");
        System.out.printf("  station 4 is a %s thread%n", asDaemon ? "DAEMON" : "USER (ordinary)");

        for (int i = 1; i <= 3; i++) {
            final int id = i;
            Thread t = new Thread(() -> {
                for (int s = 0; s < 10; s++) {
                    try { Thread.sleep(2); } catch (InterruptedException e) { return; }
                }
                System.out.printf("      Station-%d finished its 10 scripts%n", id);
            }, "Station-" + i);
            t.start();
            t.join();                      // keep the output deterministic for stations 1-3
        }

        // Station 4 has a LONG batch that outlives main()
        Thread slow = new Thread(() -> {
            for (int s = 1; s <= 200; s++) {
                try { Thread.sleep(5); } catch (InterruptedException e) { return; }
                if (s % 50 == 0) System.out.printf("      Station-4 has scanned %d scripts%n", s);
            }
            System.out.println("      Station-4 finished all 200 scripts");
        }, "Station-4");
        slow.setDaemon(asDaemon);
        slow.start();

        Thread.sleep(120);                 // let station 4 get going
        System.out.printf("  main() is about to end.  Station-4 isAlive()=%s isDaemon()=%s%n",
                slow.isAlive(), slow.isDaemon());
        System.out.println("  main() ends here.");
    }
}
