package parking.app;

import parking.model.Slot;
import parking.model.Car;
import parking.service.Facility;

/* Task 10 - evidence that the access modifiers really do seal the model.
 * Every line in tryIt() is intended to FAIL to compile.
 *   javac -cp ../out -d /tmp/t10 compile_errors/BreakEncapsulation.java
 */
class BreakEncapsulation {
    void tryIt(Slot s, Car car) {
        s.free();                            // free() is package-private to parking.model
        Slot invented = new Slot(0, 0);      // the constructor is package-private too
        double raw = car.ratePerHour();      // protected: not visible outside the hierarchy
        System.out.println(invented + " " + raw);
    }
}
