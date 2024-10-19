package pl.dawidgorecki.clamav.exception;

public class ClamAVException extends RuntimeException {
    public ClamAVException(String message) {
        super(message);
    }

    public ClamAVException(String message, Throwable cause) {
        super(message, cause);
    }
}
