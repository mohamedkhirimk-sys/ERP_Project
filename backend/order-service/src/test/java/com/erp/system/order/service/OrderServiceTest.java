package com.erp.system.order.service;

import com.erp.system.order.client.AccountingClient;
import com.erp.system.order.client.InventoryClient;
import com.erp.system.order.client.PaymentClient;
import com.erp.system.order.client.SalesClient;
import com.erp.system.order.dto.AccountingPostingRequest;
import com.erp.system.order.dto.ItemRequest;
import com.erp.system.order.dto.OrderRequest;
import com.erp.system.order.dto.OrderResponse;
import com.erp.system.order.entity.OrderEntity;
import com.erp.system.order.repository.OrderRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;
    @Mock
    private InventoryClient inventoryClient;
    @Mock
    private PaymentClient paymentClient;
    @Mock
    private SalesClient salesClient;
    @Mock
    private AccountingClient accountingClient;

    @Test
    void postsAccountingEntryAfterOrderConfirmed() {
        when(orderRepository.findByIdempotencyKey(any())).thenReturn(Optional.empty());
        when(orderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        OrderService service = new OrderService(orderRepository, inventoryClient, paymentClient, salesClient, accountingClient);
        OrderResponse response = service.createOrder(request());

        ArgumentCaptor<AccountingPostingRequest> captor = ArgumentCaptor.forClass(AccountingPostingRequest.class);
        verify(accountingClient).post(captor.capture());
        AccountingPostingRequest posting = captor.getValue();

        assertThat(posting.getSourceType()).isEqualTo("ORDER_CONFIRMED");
        assertThat(posting.getSourceId()).isEqualTo(response.getOrderNumber());
        assertThat(posting.getCustomerName()).isEqualTo("Test Co");
        assertThat(posting.getTotalAmount()).isEqualByComparingTo("100.00");
        assertThat(response.getStatus()).isEqualTo("CONFIRMED");
    }

    @Test
    void keepsOrderConfirmedWhenAccountingPostingFails() {
        when(orderRepository.findByIdempotencyKey(any())).thenReturn(Optional.empty());
        when(orderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        doThrow(new RuntimeException("finance-service down")).when(accountingClient).post(any());

        OrderService service = new OrderService(orderRepository, inventoryClient, paymentClient, salesClient, accountingClient);
        OrderResponse[] holder = new OrderResponse[1];
        assertThatCode(() -> holder[0] = service.createOrder(request())).doesNotThrowAnyException();

        assertThat(holder[0].getStatus()).isEqualTo("CONFIRMED");
        verify(accountingClient).post(any());
    }

    @Test
    void backfillsAccountingEntriesForConfirmedOrders() {
        OrderEntity confirmed = OrderEntity.builder()
                .orderNumber("ORD-1")
                .customerName("Acme")
                .totalAmount(new BigDecimal("100.00"))
                .status("CONFIRMED")
                .build();
        OrderEntity pending = OrderEntity.builder()
                .orderNumber("ORD-2")
                .customerName("Globex")
                .totalAmount(new BigDecimal("50.00"))
                .status("PENDING")
                .build();
        when(orderRepository.findAll()).thenReturn(List.of(confirmed, pending));

        OrderService service = new OrderService(orderRepository, inventoryClient, paymentClient, salesClient, accountingClient);
        Map<String, Object> result = service.backfillAccounting();

        ArgumentCaptor<AccountingPostingRequest> captor = ArgumentCaptor.forClass(AccountingPostingRequest.class);
        verify(accountingClient).post(captor.capture());
        AccountingPostingRequest posting = captor.getValue();

        assertThat(posting.getSourceType()).isEqualTo("ORDER_CONFIRMED");
        assertThat(posting.getSourceId()).isEqualTo("ORD-1");
        assertThat(posting.getCustomerName()).isEqualTo("Acme");
        assertThat(posting.getTotalAmount()).isEqualByComparingTo("100.00");
        assertThat(result).containsEntry("processed", 1).containsEntry("created", 1);
    }

    @Test
    void doesNotAbortBackfillWhenPostingFails() {
        OrderEntity confirmed = OrderEntity.builder()
                .orderNumber("ORD-1")
                .customerName("Acme")
                .totalAmount(new BigDecimal("100.00"))
                .status("CONFIRMED")
                .build();
        when(orderRepository.findAll()).thenReturn(List.of(confirmed));
        doThrow(new RuntimeException("finance-service down")).when(accountingClient).post(any());

        OrderService service = new OrderService(orderRepository, inventoryClient, paymentClient, salesClient, accountingClient);
        Map<String, Object> result = service.backfillAccounting();

        assertThat(result).containsEntry("processed", 1).containsEntry("created", 0);
        verify(accountingClient).post(any());
    }

    private OrderRequest request() {
        return OrderRequest.builder()
                .customerName("Test Co")
                .totalAmount(new BigDecimal("100.00"))
                .status("PENDING")
                .paymentMethod("CARD")
                .idempotencyKey("KEY-1")
                .items(List.of(ItemRequest.builder().productSku("SKU-1").quantity(1).build()))
                .build();
    }
}
