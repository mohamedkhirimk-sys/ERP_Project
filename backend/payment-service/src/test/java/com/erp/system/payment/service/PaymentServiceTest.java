package com.erp.system.payment.service;

import com.erp.system.payment.client.AccountingClient;
import com.erp.system.payment.dto.AccountingPostingRequest;
import com.erp.system.payment.dto.PaymentRequest;
import com.erp.system.payment.dto.PaymentResponse;
import com.erp.system.payment.entity.PaymentEntity;
import com.erp.system.payment.entity.PaymentStatus;
import com.erp.system.payment.repository.PaymentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private ApplicationEventPublisher eventPublisher;
    @Mock
    private AccountingClient accountingClient;

    @Test
    void postsAccountingEntryAfterCompletedPayment() {
        when(paymentRepository.findByIdempotencyKey("KEY-1")).thenReturn(Optional.empty());
        when(paymentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        PaymentService service = new PaymentService(paymentRepository, eventPublisher, accountingClient);
        PaymentResponse response = service.processPayment(request());

        ArgumentCaptor<AccountingPostingRequest> captor = ArgumentCaptor.forClass(AccountingPostingRequest.class);
        verify(accountingClient).post(captor.capture());
        AccountingPostingRequest posting = captor.getValue();

        assertThat(posting.getSourceType()).isEqualTo("PAYMENT_COMPLETED");
        assertThat(posting.getSourceId()).isEqualTo("ORD-1");
        assertThat(posting.getTotalAmount()).isEqualByComparingTo("100.00");
        assertThat(response.getStatus()).isEqualTo("COMPLETED");
    }

    @Test
    void keepsPaymentCompletedWhenAccountingPostingFails() {
        when(paymentRepository.findByIdempotencyKey("KEY-1")).thenReturn(Optional.empty());
        when(paymentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        doThrow(new RuntimeException("finance-service down")).when(accountingClient).post(any());

        PaymentService service = new PaymentService(paymentRepository, eventPublisher, accountingClient);
        PaymentResponse[] holder = new PaymentResponse[1];
        assertThatCode(() -> holder[0] = service.processPayment(request())).doesNotThrowAnyException();

        assertThat(holder[0].getStatus()).isEqualTo("COMPLETED");
        verify(accountingClient).post(any());
    }

    @Test
    void doesNotPostTwiceForReplayedPayment() {
        when(paymentRepository.findByIdempotencyKey("KEY-1"))
                .thenReturn(Optional.of(PaymentEntity.builder()
                        .id(7L)
                        .orderId("ORD-1")
                        .status(PaymentStatus.COMPLETED)
                        .build()));

        PaymentService service = new PaymentService(paymentRepository, eventPublisher, accountingClient);
        service.processPayment(request());

        verify(accountingClient, never()).post(any());
    }

    private PaymentRequest request() {
        return PaymentRequest.builder()
                .orderId("ORD-1")
                .amount(new BigDecimal("100.00"))
                .paymentMethod("CARD")
                .idempotencyKey("KEY-1")
                .build();
    }

    @Test
    void backfillsAccountingEntriesForCompletedPayments() {
        PaymentEntity completed = PaymentEntity.builder()
                .id(1L)
                .orderId("ORD-1")
                .amount(new BigDecimal("100.00"))
                .status(PaymentStatus.COMPLETED)
                .build();
        PaymentEntity failed = PaymentEntity.builder()
                .id(2L)
                .orderId("ORD-2")
                .amount(new BigDecimal("50.00"))
                .status(PaymentStatus.FAILED)
                .build();
        when(paymentRepository.findAll()).thenReturn(List.of(completed, failed));

        PaymentService service = new PaymentService(paymentRepository, eventPublisher, accountingClient);
        Map<String, Object> result = service.backfillAccounting();

        ArgumentCaptor<AccountingPostingRequest> captor = ArgumentCaptor.forClass(AccountingPostingRequest.class);
        verify(accountingClient).post(captor.capture());
        AccountingPostingRequest posting = captor.getValue();

        assertThat(posting.getSourceType()).isEqualTo("PAYMENT_COMPLETED");
        assertThat(posting.getSourceId()).isEqualTo("ORD-1");
        assertThat(posting.getTotalAmount()).isEqualByComparingTo("100.00");
        assertThat(result).containsEntry("processed", 1).containsEntry("created", 1);
    }

    @Test
    void doesNotAbortPaymentBackfillWhenPostingFails() {
        PaymentEntity completed = PaymentEntity.builder()
                .id(1L)
                .orderId("ORD-1")
                .amount(new BigDecimal("100.00"))
                .status(PaymentStatus.COMPLETED)
                .build();
        when(paymentRepository.findAll()).thenReturn(List.of(completed));
        doThrow(new RuntimeException("finance-service down")).when(accountingClient).post(any());

        PaymentService service = new PaymentService(paymentRepository, eventPublisher, accountingClient);
        Map<String, Object> result = service.backfillAccounting();

        assertThat(result).containsEntry("processed", 1).containsEntry("created", 0);
        verify(accountingClient).post(any());
    }
}
