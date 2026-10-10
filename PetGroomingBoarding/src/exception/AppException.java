package exception;

/**
 * Base custom exception for the Pet Grooming and Boarding Management System.
 */
public class AppException extends Exception {
    private static final long serialVersionUID = 1L;

    public AppException(String message) {
        super(message);
    }

    public AppException(String message, Throwable cause) {
        super(message, cause);
    }
}
