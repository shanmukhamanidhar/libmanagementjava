package library.operations;

/**
 * 24CS202 Unit III Concept: Custom Checked Exception Subclass
 * 
 * Member 2 Responsibility: Library Operations Exception Handling
 * 
 * A checked exception subclassing java.lang.Exception.
 * Represents conditions where a requested library book cannot be issued,
 * such as when it is already on loan, non-circulating, or missing.
 * 
 * Because it extends Exception (rather than RuntimeException), the Java compiler
 * enforces that methods declaring it must specify 'throws BookUnavailableException',
 * and calling code must handle it using 'try-catch' blocks.
 */
public class BookUnavailableException extends Exception {

    private final String bookId;
    private final String reason;

    /**
     * Parameterized Constructor with contextual details.
     * 
     * @param bookId Identifier of the problematic book
     * @param reason Detailed cause of unavailability
     */
    public BookUnavailableException(String bookId, String reason) {
        super(String.format("Book [%s] cannot be issued: %s", bookId, reason));
        this.bookId = bookId;
        this.reason = reason;
    }

    /**
     * Constructor accepting just a general message.
     * 
     * @param message Descriptive error message
     */
    public BookUnavailableException(String message) {
        super(message);
        this.bookId = "UNKNOWN";
        this.reason = message;
    }

    /**
     * Constructor supporting exception chaining.
     * 
     * @param message Error message
     * @param cause Underlying Throwable root cause
     */
    public BookUnavailableException(String message, Throwable cause) {
        super(message, cause);
        this.bookId = "UNKNOWN";
        this.reason = message;
    }

    public String getBookId() {
        return bookId;
    }

    public String getReason() {
        return reason;
    }
}
