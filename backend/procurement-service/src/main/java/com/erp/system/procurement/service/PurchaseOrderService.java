package com.erp.system.procurement.service;

import com.erp.system.procurement.dto.PurchaseOrderRequest;
import com.erp.system.procurement.dto.PurchaseOrderResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface PurchaseOrderService {
    PurchaseOrderResponse createPurchaseOrder(PurchaseOrderRequest request);
    PurchaseOrderResponse getPurchaseOrderById(Long id);
    Page<PurchaseOrderResponse> getPurchaseOrdersByVendor(Long vendorId, Pageable pageable);
    Page<PurchaseOrderResponse> getAllPurchaseOrders(Pageable pageable);
    PurchaseOrderResponse updatePurchaseOrderStatus(Long id, String status);
}
