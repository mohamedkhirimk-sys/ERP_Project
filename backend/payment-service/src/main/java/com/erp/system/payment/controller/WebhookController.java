package com.erp.system.payment.controller;
import com.erp.system.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/webhooks")
@RequiredArgsConstructor
public class WebhookController {

    private final PaymentService paymentService;

    @PostMapping("/order-status")
    public ResponseEntity<String> handleOrderStatusWebhook(@RequestBody WebhookRequest request) {
        paymentService.handleOrderStatusUpdate(request.getOrderId(), request.getStatus());
        return ResponseEntity.ok("Webhook received");
    }


}