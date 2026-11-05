/* Task 9 - evidence that an abstract class cannot be instantiated and that an
 * interface cannot hold state or a constructor.
 *   javac -cp .. InstantiateAbstract.java
 */
class InstantiateAbstract {
    void tryIt() {
        Vehicle v = new Vehicle("TN01AB1111", 4);   // Vehicle is abstract
        System.out.println(v);
    }
}
