package in.akashrchandran.salesdataloader.exception;

public class RetryAbleException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public RetryAbleException(String message) {
        super(message);
    }

    public RetryAbleException(String message, Throwable cause) {
        super(message, cause);
    }

    public RetryAbleException(Throwable cause) {
        super(cause);
    }
    
}
