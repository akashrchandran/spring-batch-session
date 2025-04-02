package in.akashrchandran.salesdataloader.processor;

import in.akashrchandran.salesdataloader.dto.SalesDataDto;
import in.akashrchandran.salesdataloader.entity.SalesData;
import in.akashrchandran.salesdataloader.exception.CrashException;
import in.akashrchandran.salesdataloader.exception.RetryAbleException;
import in.akashrchandran.salesdataloader.exception.SkipAbleException;
import in.akashrchandran.salesdataloader.mapper.SalesDataMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.batch.item.ItemProcessor;

@RequiredArgsConstructor
@Slf4j
public class SalesDataItemProcessor implements ItemProcessor<SalesDataDto, SalesData> {

    private static boolean isRetryed = false;

    @Override
    public SalesData process(SalesDataDto item) throws Exception {
        log.info("Processing item index: {}", item.getIndex());
        if (item.getIndex() == 100 && !isRetryed) {
            isRetryed = true;
            throw new RetryAbleException("retry this item");
        }
        if (item.getIndex() == 200) {
            throw new SkipAbleException("Just to skip");
        }
        if (item.getIndex() == 351) {
            throw new CrashException("Job failed, please restart the job");
        }
        return SalesDataMapper.INSTANCE.toEntity(item);
    }
}
