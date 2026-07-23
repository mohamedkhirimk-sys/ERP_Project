package com.erp.system.order.service;

import com.erp.system.order.client.InventoryClient;
import com.erp.system.order.client.PaymentClient;
import com.erp.system.order.dto.*;
import com.erp.system.order.entity.OrderEntity;
import com.erp.system.order.entity.OrderItem;
import com.erp.system.order.repository.OrderRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final InventoryClient inventoryClient;
    private final PaymentClient paymentClient;

    @Transactional
    public OrderResponse createOrder(OrderRequest request) {
        OrderEntity entity = OrderEntity.builder()
                .orderNumber("ORD-" + System.currentTimeMillis())
                .customerName(request.getCustomerName())
                .totalAmount(request.getTotalAmount())
                .status(request.getStatus())
                .build();

        List<OrderItem> orderItems = request.getItems().stream().map(itemReq -> {
            inventoryClient.deductStock(itemReq.getProductSku(), -itemReq.getQuantity());
            OrderItem orderItem = OrderItem.builder()
                    .productSku(itemReq.getProductSku())
                    .quantity(itemReq.getQuantity())
                    .order(entity)
                    .build();
            return orderItem;
        }).toList();

        entity.setItems(orderItems);
        OrderEntity saved = orderRepository.save(entity);

        PaymentRequest paymentReq = PaymentRequest.builder()
                .orderId(entity.getOrderNumber())
                .amount(request.getTotalAmount())
                .paymentMethod(request.getPaymentMethod())
                .build();
        paymentClient.processPayment(paymentReq);

        return toResponse(saved);
    }

    public List<OrderResponse> getAllOrders() {
        return orderRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
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
