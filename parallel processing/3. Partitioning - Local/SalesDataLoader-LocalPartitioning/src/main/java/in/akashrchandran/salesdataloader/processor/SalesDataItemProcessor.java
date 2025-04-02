package in.akashrchandran.salesdataloader.processor;

import in.akashrchandran.salesdataloader.dto.SalesDataDto;
import in.akashrchandran.salesdataloader.entity.SalesData;
import in.akashrchandran.salesdataloader.mapper.SalesDataMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.item.ItemProcessor;

@RequiredArgsConstructor
public class SalesDataItemProcessor implements ItemProcessor<SalesDataDto, SalesData> {

    @Override
    public SalesData process(SalesDataDto item) throws Exception {
        return SalesDataMapper.INSTANCE.toEntity(item);
    }
}
