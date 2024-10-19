package pl.dawidgorecki.clamav.exception;

public class UnknownCommandException extends RuntimeException {
    public UnknownCommandException(String message) {
        super(message);
    }
}
