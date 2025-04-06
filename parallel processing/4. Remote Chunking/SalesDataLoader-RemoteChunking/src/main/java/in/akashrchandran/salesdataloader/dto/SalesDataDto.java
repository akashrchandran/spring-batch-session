package in.akashrchandran.salesdataloader.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SalesDataDto implements Serializable {
    private Long index;
    private String invoiceNo;
    private String stockCode;
    private String description;
    private Integer quantity;
    private String invoiceDate;
    private BigDecimal unitPrice;
    private String customerId;
    private String country;
}
