package exception;

/**
 * Thrown when a booking or cancellation breaks business rules 
 * (e.g., slot overlap, room unavailability, past dates, payment failures).
 */
public class BookingException extends AppException {
    private static final long serialVersionUID = 1L;

    public BookingException(String message) {
        super(message);
    }

    public BookingException(String message, Throwable cause) {
        super(message, cause);
    }
}
