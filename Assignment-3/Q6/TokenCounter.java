/*
 * Programming in Java  -  Assignment 3 (CO3)
 * Q6  -  Inter-Thread Communication  ("Complete the Code")
 *
 * Name            : Vignesh S
 * Register Number : 192325002
 * Primary responsibility for this file: Student A (producer-consumer design)
 *
 * The given skeleton is unchanged apart from TODO 1 and TODO 2.
 * Q6/if_version/TokenCounterIf.java is the SAME program with while replaced by
 * if, kept so the difference the brief asks about can be demonstrated.
 */
public class TokenCounter {
    private final int[] buffer = new int[5];      // the counter holds at most 5 tokens
    private int count = 0, in = 0, out = 0;

    public synchronized void put(int token) throws InterruptedException {
        // TODO 1: Wait while the counter is full, add the token at position in, then
        //   wake waiting threads
        //
        // while, not if: wait() can return without the condition being true -
        // a notifyAll() wakes EVERY waiter, and only one of them can proceed,
        // so each must re-test before acting.  The loop is also the defence
        // against a spurious wakeup, which the JLS explicitly permits.
        while (count == buffer.length) {
            wait();                               // releases the lock while waiting
        }
        buffer[in] = token;
        in = (in + 1) % buffer.length;            // circular buffer
        count++;
        notifyAll();                              // a consumer may be waiting on empty
    }

    public synchronized int take() throws InterruptedException {
        // TODO 2: Wait while the counter is empty, remove the token at position out,
        //   wake waiting threads and return the token
        while (count == 0) {
            wait();
        }
        int token = buffer[out];
        out = (out + 1) % buffer.length;
        count--;
        notifyAll();                              // the producer may be waiting on full
        return token;
    }

    public static void main(String[] args) throws InterruptedException {
        TokenCounter counter = new TokenCounter();
        int[] collected = new int[2];
        Thread cook = new Thread(() -> {
            try {
                for (int t = 1; t <= 20; t++) counter.put(t);
            } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        });
        Thread[] students = new Thread[2];
        for (int s = 0; s < 2; s++) {
            final int id = s;
            students[s] = new Thread(() -> {
                try {
                    for (int k = 0; k < 10; k++) collected[id] += counter.take();
                } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            });
        }
        cook.start(); for (Thread t : students) t.start();
        cook.join(); for (Thread t : students) t.join();
        System.out.println("Sum of tokens collected: " + (collected[0] + collected[1])
                + " (expected 210)");
    }
}
