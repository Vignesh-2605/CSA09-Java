package parking.app;

/* Task 10 - evidence that the storage layout is invisible outside parking.service.
 *   javac -cp ../out -d /tmp/t10 compile_errors/BreakServiceInternals.java
 */
class BreakServiceInternals {
    void tryIt() {
        parking.service.SlotGrid g = new parking.service.SlotGrid(new int[]{2});  // not public
        System.out.println(g);
    }
}
