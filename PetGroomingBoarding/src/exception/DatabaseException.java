package exception;

/**
 * Thrown when database connection, initialization, or SQL operations fail.
 */
public class DatabaseException extends AppException {
    private static final long serialVersionUID = 1L;

    public DatabaseException(String message) {
        super(message);
    }

    public DatabaseException(String message, Throwable cause) {
        super(message, cause);
    }
}
