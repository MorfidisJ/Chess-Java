package chess;

/**
 * Exception thrown when a search exceeds its time limit.
 */
public class SearchTimeoutException extends Exception {
    public SearchTimeoutException(String message) {
        super(message);
    }
}
