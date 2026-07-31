package com.erp.system.sales.dto;

import lombok.*;
import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductDto {
    private String sku;
    private String name;
    private BigDecimal price;
}
