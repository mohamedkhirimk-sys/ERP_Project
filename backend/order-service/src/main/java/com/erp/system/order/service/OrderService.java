package com.erp.system.order.service;

import com.erp.system.order.client.InventoryClient;
import com.erp.system.order.client.PaymentClient;
import com.erp.system.order.client.SalesClient;
import com.erp.system.order.dto.*;
import com.erp.system.order.entity.OrderEntity;
import com.erp.system.order.entity.OrderItem;
import com.erp.system.order.repository.OrderRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;
    private final InventoryClient inventoryClient;
    private final PaymentClient paymentClient;
    private final SalesClient salesClient;

    @Transactional
    public OrderResponse createOrder(OrderRequest request) {
        if (request.getIdempotencyKey() != null
                && orderRepository.findByIdempotencyKey(request.getIdempotencyKey()).isPresent()) {
            throw new IllegalArgumentException("Duplicate order - idempotency key already exists");
        }

        OrderEntity entity = OrderEntity.builder()
                .orderNumber("ORD-" + System.currentTimeMillis())
                .idempotencyKey(request.getIdempotencyKey())
                .customerName(request.getCustomerName())
                .totalAmount(request.getTotalAmount())
                .status("PENDING")
                .build();

        List<OrderItem> orderItems = new ArrayList<>();
        try {
            for (var itemReq : request.getItems()) {
                inventoryClient.deductStock(itemReq.getProductSku(), -itemReq.getQuantity());
                OrderItem orderItem = OrderItem.builder()
                        .productSku(itemReq.getProductSku())
                        .quantity(itemReq.getQuantity())
                        .order(entity)
                        .build();
                orderItems.add(orderItem);
            }

            entity.setItems(orderItems);
            OrderEntity saved = orderRepository.save(entity);

            PaymentRequest paymentReq = PaymentRequest.builder()
                    .orderId(entity.getOrderNumber())
                    .amount(request.getTotalAmount())
                    .paymentMethod(request.getPaymentMethod())
                    .build();
            paymentClient.processPayment(paymentReq);

            try {
                OrderInvoiceRequest invoiceReq = OrderInvoiceRequest.builder()
                        .orderNumber(saved.getOrderNumber())
                        .customerName(saved.getCustomerName())
                        .totalAmount(saved.getTotalAmount())
                        .items(request.getItems())
                        .build();
                salesClient.createInvoiceFromOrder(invoiceReq);
            } catch (Exception e) {
                log.error("Failed to create invoice for order {}, will retry later: {}", saved.getOrderNumber(), e.getMessage());
            }

            saved.setStatus("CONFIRMED");
            return toResponse(orderRepository.save(saved));
        } catch (Exception e) {
            for (var itemReq : request.getItems()) {
                inventoryClient.deductStock(itemReq.getProductSku(), itemReq.getQuantity());
            }
            entity.setItems(orderItems);
            entity.setStatus("FAILED");
            orderRepository.save(entity);
            throw new RuntimeException("Order creation failed, stock restored", e);
        }
    }

    public Page<OrderResponse> getAllOrders(Pageable pageable) {
        return orderRepository.findAll(pageable)
                .map(this::toResponse);
    }

    private OrderResponse toResponse(OrderEntity entity) {
        List<ItemResponse> itemResponses = entity.getItems().stream()
                .map(item -> new ItemResponse(item.getId(), item.getProductSku(), item.getQuantity()))
                .toList();

        return OrderResponse.builder()
                .id(entity.getId())
                .orderNumber(entity.getOrderNumber())
                .customerName(entity.getCustomerName())
                .totalAmount(entity.getTotalAmount())
                .status(entity.getStatus())
                .items(itemResponses)
                .build();
    }
}
