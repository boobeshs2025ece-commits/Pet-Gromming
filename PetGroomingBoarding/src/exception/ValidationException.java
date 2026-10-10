package exception;

/**
 * Thrown when user input, format checks, or authentication validations fail.
 */
public class ValidationException extends AppException {
    private static final long serialVersionUID = 1L;

    public ValidationException(String message) {
        super(message);
    }

    public ValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}
