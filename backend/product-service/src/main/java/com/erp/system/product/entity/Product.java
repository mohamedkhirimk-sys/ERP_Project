package com.erp.system.product.entity;

import jakarta.persistence.*;
import lombok.*;
import com.erp.common.audit.Auditable;
import java.math.BigDecimal;

@Entity
@Table(name = "products")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product extends Auditable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    private String description;

    @Column(nullable = false)
    private BigDecimal price;

    @Column(unique = true, nullable = false)
    private String sku;

    private Integer stockQuantity;
}