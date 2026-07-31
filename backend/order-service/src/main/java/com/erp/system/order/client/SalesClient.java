package com.erp.system.order.client;

import com.erp.system.order.dto.OrderInvoiceRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "sales-service", path = "/api/invoices")
public interface SalesClient {

    @PostMapping("/from-order")
    void createInvoiceFromOrder(@RequestBody OrderInvoiceRequest request);
}
