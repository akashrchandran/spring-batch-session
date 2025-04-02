package in.akashrchandran.salesdataloader.listener;

import in.akashrchandran.salesdataloader.dto.SalesDataDto;
import in.akashrchandran.salesdataloader.entity.SalesData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.SkipListener;
import org.springframework.stereotype.Component;

@Component
public class StepSkipListener implements SkipListener<SalesDataDto, SalesData> {

    private static final Logger log = LoggerFactory.getLogger(StepSkipListener.class);

    @Override
    public void onSkipInRead(Throwable t) {
        log.error("Error while reading record: {}", t.getMessage());
    }

    @Override
    public void onSkipInProcess(SalesDataDto item, Throwable t) {
        log.error("Error while processing record: {} - Error: {}", item, t.getMessage());
    }

    @Override
    public void onSkipInWrite(SalesData item, Throwable t) {
        log.error("Error while writing record: {} - Error: {}", item, t.getMessage());
    }
}
