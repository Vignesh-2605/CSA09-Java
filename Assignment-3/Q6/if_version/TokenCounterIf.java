/*
 * Programming in Java  -  Assignment 3 (CO3)
 * Q6  -  the SAME program with while replaced by if, as the brief asks.
 *
 * Name            : Vignesh S   Register Number: 192325002
 *
 * A guard is added so the failure is REPORTED rather than silently producing a
 * wrong total: without it the symptom is simply a wrong sum, which does not
 * show WHERE the design broke.
 */
public class TokenCounterIf {
    private final int[] buffer = new int[5];
    private int count = 0, in = 0, out = 0;
    static volatile String firstFault = null;

    public synchronized void put(int token) throws InterruptedException {
        if (count == buffer.length) {            // if, not while
            wait();
        }
        if (count == buffer.length && firstFault == null)
            firstFault = "put() proceeded while the counter was FULL - a token is overwritten";
        buffer[in] = token;
        in = (in + 1) % buffer.length;
        count++;
        notifyAll();
    }

    public synchronized int take() throws InterruptedException {
        if (count == 0) {                        // if, not while
            wait();
        }
        if (count == 0 && firstFault == null)
            firstFault = "take() proceeded while the counter was EMPTY - a stale slot is read";
        int token = buffer[out];
        out = (out + 1) % buffer.length;
        count--;                                 // can drive count negative
        notifyAll();
        return token;
    }

    public static void main(String[] args) throws InterruptedException {
        TokenCounterIf counter = new TokenCounterIf();
        int[] collected = new int[2];
        Thread cook = new Thread(() -> {
            try { for (int t = 1; t <= 20; t++) counter.put(t); }
            catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        });
        Thread[] students = new Thread[2];
        for (int s = 0; s < 2; s++) {
            final int id = s;
            students[s] = new Thread(() -> {
                try { for (int k = 0; k < 10; k++) collected[id] += counter.take(); }
                catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            });
        }
        cook.start(); for (Thread t : students) t.start();
        cook.join(); for (Thread t : students) t.join();
        int sum = collected[0] + collected[1];
        System.out.println("Sum of tokens collected: " + sum + " (expected 210)"
                + (sum == 210 ? "" : "   <- WRONG")
                + (firstFault == null ? "" : "   [" + firstFault + "]")
                + "   final count=" + counter.count);
    }
}
