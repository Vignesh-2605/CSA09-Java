package parking.app;

import parking.model.Car;

/* Task 10 - evidence that the billing rule cannot be redefined from outside.
 *   javac -cp ../out -d /tmp/t10 compile_errors/BreakFinalRule.java
 */
class FreeParkingCar extends Car {
    FreeParkingCar(String r) { super(r); }
    @Override public long calculateFee(int minutes) { return 0; }   // calculateFee is final
}
