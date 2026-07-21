package com.erp.system.payment.service;

import com.erp.system.payment.client.OrderClient;
import com.erp.system.payment.dto.PaymentRequest;
import com.erp.system.payment.dto.PaymentResponse;
import com.erp.system.payment.entity.PaymentEntity;
import com.erp.system.payment.entity.PaymentStatus;
import com.erp.system.payment.repository.PaymentRepository;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@SpringBootTest
public class PaymentServiceIntegrationTest {

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private PaymentRepository paymentRepository;

    @MockBean
    private OrderClient orderClient;

    @Test
    void testPaymentFlowAndWebhookUpdate() {
        String orderId = "123";
        PaymentRequest request = new PaymentRequest();
        request.setOrderId(orderId);
        request.setAmount(new java.math.BigDecimal("100.0"));
        request.setPaymentMethod("CREDIT_CARD");

        // Mock OrderClient to return a PENDING order
        OrderClient.OrderResponse mockOrder = new OrderClient.OrderResponse();
        mockOrder.setStatus("PENDING"); 
        when(orderClient.getOrderById(anyString())).thenReturn(mockOrder);

        // 1. Process Payment
        PaymentEntity response = paymentService.processPayment(request);
        assertNotNull(response);
        assertEquals("COMPLETED", response.getStatus());

        // 2. Simulate Webhook: Order is CANCELLED
        paymentService.handleOrderStatusUpdate(orderId, "CANCELLED");

        // 3. Verify status in DB is now FAILED
        Optional<com.erp.system.payment.entity.PaymentEntity> updatedPayment = paymentRepository.findByOrderId(orderId);
        assertTrue(updatedPayment.isPresent());
        assertEquals("FAILED", updatedPayment.get().getStatus().toString());
    }
}