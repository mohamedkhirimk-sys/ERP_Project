package com.erp.system.procurement.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "grn_line_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GRNLineItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "grn_id", nullable = false)
    private GoodsReceivedNote goodsReceivedNote;

    @Column(nullable = false)
    private String productSku;

    @Column(nullable = false)
    private Integer quantity;
}
