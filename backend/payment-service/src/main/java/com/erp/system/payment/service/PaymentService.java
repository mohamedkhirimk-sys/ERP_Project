package com.erp.system.payment.service;

import com.erp.common.event.PaymentCompletedEvent;
import com.erp.system.payment.client.AccountingClient;
import com.erp.system.payment.dto.AccountingPostingRequest;
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
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final AccountingClient accountingClient;

    @Transactional
    public PaymentResponse processPayment(PaymentRequest request) {
        if (request.getIdempotencyKey() != null) {
            Optional<PaymentEntity> existing = paymentRepository.findByIdempotencyKey(request.getIdempotencyKey());
            if (existing.isPresent()) {
                return toResponse(existing.get());
            }
        }
        log.info("Processing payment for order: {}", request.getOrderId());
        validateRequest(request);
        String status = determinePaymentStatus(request);
        PaymentEntity payment = PaymentEntity.builder()
                .orderId(request.getOrderId())
                .idempotencyKey(request.getIdempotencyKey())
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
        if ("COMPLETED".equals(status)) {
            try {
                accountingClient.post(AccountingPostingRequest.builder()
                        .sourceType("PAYMENT_COMPLETED")
                        .sourceId(saved.getOrderId())
                        .totalAmount(saved.getAmount())
                        .build());
            } catch (Exception e) {
                log.error("Failed to post accounting entry for payment (order {}, amount {}): {}",
                        saved.getOrderId(), saved.getAmount(), e.getMessage());
            }
        }
        return toResponse(saved);
    }

    public Map<String, Object> backfillAccounting() {
        List<PaymentEntity> completed = paymentRepository.findAll().stream()
                .filter(p -> p.getStatus() == PaymentStatus.COMPLETED)
                .toList();
        int created = 0;
        for (PaymentEntity p : completed) {
            try {
                accountingClient.post(AccountingPostingRequest.builder()
                        .sourceType("PAYMENT_COMPLETED")
                        .sourceId(p.getOrderId())
                        .totalAmount(p.getAmount())
                        .build());
                created++;
            } catch (Exception e) {
                log.error("Failed to post accounting entry for payment (order {}, amount {}): {}",
                        p.getOrderId(), p.getAmount(), e.getMessage());
            }
        }
        return Map.of("processed", completed.size(), "created", created);
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
