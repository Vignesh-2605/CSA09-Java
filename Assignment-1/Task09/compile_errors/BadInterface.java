/* Task 9 - evidence that an interface cannot declare instance state or a
 * constructor, which is why Vehicle had to be an abstract class.
 *   javac BadInterface.java
 */
interface BadVehicle {
    private String regNo;                 // an interface field is implicitly static final
    BadVehicle(String regNo);             // an interface cannot declare a constructor
}
