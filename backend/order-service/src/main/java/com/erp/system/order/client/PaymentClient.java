package com.erp.system.order.client;

import com.erp.system.order.dto.PaymentRequest;
import com.erp.system.order.dto.PaymentResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "payment-service", path = "/api/payments")
public interface PaymentClient {

    @PostMapping
    PaymentResponse processPayment(@RequestBody PaymentRequest request);
}
