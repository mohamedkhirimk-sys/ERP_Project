package com.erp.system.payment.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "order-service", path = "/api/orders")
public interface OrderClient {

    @GetMapping("/{id}")
    OrderResponse getOrderById(@PathVariable("id") Long id);
}
