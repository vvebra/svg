package lt.uhealth.alpi.svg.exception;

public class AppRuntimeException extends RuntimeException {

    public AppRuntimeException(String message) {
        super(message);
    }

    public AppRuntimeException(Throwable cause) {
        super(cause);
    }
}
