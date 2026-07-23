package com.erp.system.procurement.service;

import com.erp.system.procurement.dto.PurchaseOrderRequest;
import com.erp.system.procurement.dto.PurchaseOrderResponse;
import java.util.List;

public interface PurchaseOrderService {
    PurchaseOrderResponse createPurchaseOrder(PurchaseOrderRequest request);
    PurchaseOrderResponse getPurchaseOrderById(Long id);
    List<PurchaseOrderResponse> getPurchaseOrdersByVendor(Long vendorId);
    List<PurchaseOrderResponse> getAllPurchaseOrders();
    PurchaseOrderResponse updatePurchaseOrderStatus(Long id, String status);
}
