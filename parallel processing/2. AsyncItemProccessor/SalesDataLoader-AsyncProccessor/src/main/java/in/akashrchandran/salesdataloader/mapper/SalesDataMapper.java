package in.akashrchandran.salesdataloader.mapper;

import in.akashrchandran.salesdataloader.dto.SalesDataDto;
import in.akashrchandran.salesdataloader.entity.SalesData;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Mapper(componentModel = "spring")
public interface SalesDataMapper {

    SalesDataMapper INSTANCE = Mappers.getMapper(SalesDataMapper.class);


    @Mapping(target = "invoiceDate", expression = "java(convertStringToLocalDateTime(dto.getInvoiceDate()))")
    SalesData toEntity(SalesDataDto dto);


    default LocalDateTime convertStringToLocalDateTime(String date) {
        if (date == null || date.isEmpty()) {
            return null;
        }
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("M/d/yyyy H:mm");
        return LocalDateTime.parse(date, formatter);
    }

}
