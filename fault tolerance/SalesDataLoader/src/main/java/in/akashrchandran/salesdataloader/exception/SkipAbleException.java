package in.akashrchandran.salesdataloader.exception;

public class SkipAbleException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public SkipAbleException(String message) {
        super(message);
    }

    public SkipAbleException(String message, Throwable cause) {
        super(message, cause);
    }

    public SkipAbleException(Throwable cause) {
        super(cause);
    }
    
}
