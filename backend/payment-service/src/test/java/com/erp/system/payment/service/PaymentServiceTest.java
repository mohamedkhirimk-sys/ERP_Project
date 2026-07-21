package com.erp.system.payment.service;

import com.erp.common.event.PaymentCompletedEvent;
import com.erp.system.payment.dto.PaymentRequest;
import com.erp.system.payment.entity.PaymentEntity;
import com.erp.system.payment.exception.PaymentProcessingException;
import com.erp.system.payment.repository.PaymentRepository;

import lombok.Data;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.mockito.InjectMocks;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@Data
public class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private PaymentService paymentService;

    private PaymentRequest validRequest;

    @BeforeEach
    void setUp() {
        validRequest = PaymentRequest.builder()
                .orderId("ORD-123")       
                .amount(new BigDecimal("100.00"))
                .paymentMethod("CREDIT_CARD")
                .build();
    }

    @Test
    void processPayment_Success() {
        // Arrange
        when(paymentRepository.save(any(PaymentEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        PaymentEntity result = paymentService.processPayment(validRequest);

        // Assert
        assertNotNull(result);
        assertEquals("COMPLETED", result.getStatus());
        assertEquals("ORD-123", result.getOrderId());
        verify(eventPublisher, times(1)).publishEvent(any(PaymentCompletedEvent.class));
        verify(paymentRepository, times(1)).save(any(PaymentEntity.class));
    }

    @Test
    void processPayment_InvalidAmount_ThrowsException() {
        // Arrange
        validRequest.setAmount(new BigDecimal("10.00"));

        // Act & Assert
        PaymentProcessingException exception = assertThrows(PaymentProcessingException.class, () -> {
            paymentService.processPayment(validRequest);
        });

        assertTrue(exception.getMessage().contains("Invalid amount"));
        verify(paymentRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void processPayment_InvalidMethod_ThrowsException() {
        // Arrange
        validRequest.setPaymentMethod("INVALID");

        // Act & Assert
        PaymentProcessingException exception = assertThrows(PaymentProcessingException.class, () -> {
            paymentService.processPayment(validRequest);
        });

        assertTrue(exception.getMessage().contains("Invalid payment method"));
        verify(paymentRepository, never()).save(any());
    }
}
