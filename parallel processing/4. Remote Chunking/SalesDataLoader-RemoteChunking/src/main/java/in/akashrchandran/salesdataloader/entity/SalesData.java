package in.akashrchandran.salesdataloader.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SalesData implements Serializable {
    private Long index;
    private String invoiceNo;
    private String stockCode;
    private String description;
    private Integer quantity;
    private LocalDateTime invoiceDate;
    private BigDecimal unitPrice;
    private String customerId;
    private String country;
}
