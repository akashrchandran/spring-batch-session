package in.akashrchandran.salesdataloader.exception;

public class CrashException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public CrashException(String message) {
        super(message);
    }

    public CrashException(String message, Throwable cause) {
        super(message, cause);
    }

    public CrashException(Throwable cause) {
        super(cause);
    }
    
}
