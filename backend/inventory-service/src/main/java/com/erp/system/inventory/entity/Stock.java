package com.erp.system.inventory.entity;

import jakarta.persistence.*;
import lombok.*;
import com.erp.common.audit.Auditable;

@Entity
@Table(name = "stocks")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Stock extends Auditable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String productSku;

    @Column(nullable = false)
    private Integer quantity;

    private String warehouseLocation;
}