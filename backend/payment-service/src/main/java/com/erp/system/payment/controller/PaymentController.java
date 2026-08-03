package com.erp.system.payment.controller;

import com.erp.system.payment.dto.PaymentRequest;
import com.erp.system.payment.dto.PaymentResponse;
import com.erp.system.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping
    public ResponseEntity<PaymentResponse> processPayment(@RequestBody PaymentRequest request) {
        return ResponseEntity.ok(paymentService.processPayment(request));
    }

    @PostMapping("/accounting-backfill")
    public ResponseEntity<Map<String, Object>> backfillAccounting() {
        return ResponseEntity.ok(paymentService.backfillAccounting());
    }
}