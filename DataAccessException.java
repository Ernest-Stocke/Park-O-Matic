/** Unchecked wrapper for database errors so service and UI code stays free of SQLException. */
public class DataAccessException extends RuntimeException {
    public DataAccessException(String message, Throwable cause) {
        super(message, cause);
    }
}
