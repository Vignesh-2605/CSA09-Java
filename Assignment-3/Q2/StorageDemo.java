/*
 * Programming in Java  -  Assignment 3 (CO3)
 * Q2  -  User-Defined Exceptions and Propagation  ("Complete the Code")
 *
 * Name            : Vignesh S
 * Register Number : 192325002
 * Primary responsibility for this file: Student A (hierarchy and propagation)
 *
 * The given skeleton is unchanged apart from TODO 1.
 */
import java.io.IOException;

class ExamException extends Exception {
    ExamException(String message) { super(message); }
    ExamException(String message, Throwable cause) { super(message, cause); }
}

class SubmissionStorageException extends ExamException {
    SubmissionStorageException(String message, Throwable cause) {
        super(message, cause);
    }
}

class Storage {
    void save(String fileName, long sizeKb) throws SubmissionStorageException {
        try {
            if (sizeKb > 500) throw new IOException("Disk quota exceeded");
            System.out.println("Saved " + fileName);
        } catch (IOException e) {
            // TODO 1: Wrap the IOException in a SubmissionStorageException, keeping the
            //   IOException as its cause
            //
            // The second constructor argument is what preserves the cause.  Passing
            // only the message would discard the IOException entirely, and
            // e.getCause() in main() would then return null.
            throw new SubmissionStorageException("Could not store " + fileName, e);
        }
    }
}

public class StorageDemo {
    public static void main(String[] args) {
        Storage storage = new Storage();
        try {
            storage.save("answer01.pdf", 120);
            storage.save("answer02.pdf", 800);
        } catch (ExamException e) {
            System.out.println("Error: " + e.getMessage());
            System.out.println("Cause: " + e.getCause().getMessage());
        }
    }
}
