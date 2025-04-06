package in.akashrchandran.salesdataloader.processor;

import in.akashrchandran.salesdataloader.dto.SalesDataDto;
import in.akashrchandran.salesdataloader.entity.SalesData;
import in.akashrchandran.salesdataloader.mapper.SalesDataMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemProcessor;

@RequiredArgsConstructor
@Slf4j
public class SalesDataItemProcessor implements ItemProcessor<SalesDataDto, SalesData> {

    @Override
    public SalesData process(SalesDataDto item) throws Exception {
//        log.info("Processing item: {}", item.getIndex());
        return SalesDataMapper.INSTANCE.toEntity(item);
    }
}
