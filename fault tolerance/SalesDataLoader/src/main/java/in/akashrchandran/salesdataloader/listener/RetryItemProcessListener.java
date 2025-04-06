package in.akashrchandran.salesdataloader.listener;

import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.RetryCallback;
import org.springframework.retry.RetryContext;
import org.springframework.retry.RetryListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class RetryItemProcessListener implements RetryListener {

    @Override
    public <T, E extends Throwable> boolean open(RetryContext context, RetryCallback<T, E> callback) {
        if (context.getRetryCount() != 0) {
            log.info("Retry attempt started for item: {}", context.getAttribute("item"));
        }
        return true;
    }

    @Override
    public <T, E extends Throwable> void close(RetryContext context, RetryCallback<T, E> callback, Throwable throwable) {
        if (context.getRetryCount() != 0) {
            log.info("Retry attempt completed. Retry count: {}", context.getRetryCount());
        }
    }

    @Override
    public <T, E extends Throwable> void onError(RetryContext context, RetryCallback<T, E> callback, Throwable throwable) {
        log.error("Error in retry attempt {}. Error: {}", context.getRetryCount(), throwable.getMessage());
        log.debug("Full stack trace:", throwable);
    }
}
