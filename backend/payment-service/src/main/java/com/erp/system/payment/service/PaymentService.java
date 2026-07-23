package com.erp.system.payment.service;

import com.erp.common.event.PaymentCompletedEvent;
import com.erp.system.payment.dto.PaymentRequest;
import com.erp.system.payment.dto.PaymentResponse;
import com.erp.system.payment.entity.PaymentEntity;
import com.erp.system.payment.entity.PaymentStatus;
import com.erp.system.payment.repository.PaymentRepository;
import com.erp.system.payment.exception.PaymentProcessingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public PaymentResponse processPayment(PaymentRequest request) {
        log.info("Processing payment for order: {}", request.getOrderId());
        validateRequest(request);
        String status = determinePaymentStatus(request);
        PaymentEntity payment = PaymentEntity.builder()
                .orderId(request.getOrderId())
                .amount(request.getAmount())
                .paymentMethod(request.getPaymentMethod())
                .status(PaymentStatus.valueOf(status))
                .build();
        if ("FAILED".equals(status)) {
            log.error("Payment failed for order: {}", request.getOrderId());
        } else {
            eventPublisher.publishEvent(PaymentCompletedEvent.builder()
                    .orderNumber(request.getOrderId())
                    .amount(request.getAmount())
                    .paymentStatus("COMPLETED")
                    .build());
            log.info("Payment successful for order: {}", request.getOrderId());
        }
        PaymentEntity saved = paymentRepository.save(payment);
        return toResponse(saved);
    }

    private void validateRequest(PaymentRequest request) {
        if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new PaymentProcessingException("Invalid amount for order: " + request.getOrderId());
        }
        if ("INVALID".equalsIgnoreCase(request.getPaymentMethod())) {
            throw new PaymentProcessingException("Invalid payment method for order: " + request.getOrderId());
        }
    }

    private String determinePaymentStatus(PaymentRequest request) {
        return "COMPLETED";
    }

    private PaymentResponse toResponse(PaymentEntity entity) {
        return PaymentResponse.builder()
                .id(entity.getId())
                .orderId(entity.getOrderId())
                .amount(entity.getAmount())
                .status(entity.getStatus().name())
                .paymentMethod(entity.getPaymentMethod())
                .transactionId(entity.getTransactionId())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    public void handleOrderStatusUpdate(String orderId, String status) {
        log.info("Handling order status update for order: {}", orderId);
    }
}
