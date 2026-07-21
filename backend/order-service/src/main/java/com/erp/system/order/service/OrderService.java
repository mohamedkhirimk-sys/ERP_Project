package com.erp.system.order.service;

import com.erp.system.order.dto.OrderRequest;
import com.erp.system.order.dto.OrderResponse;
import com.erp.system.order.entity.OrderEntity;
import com.erp.system.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;

    public OrderResponse createOrder(OrderRequest request) {
        OrderEntity entity = OrderEntity.builder()
                .orderNumber("ORD-" + System.currentTimeMillis())
                .customerName(request.customerName())
                .totalAmount(request.totalAmount())
                .status(request.status())
                .build();

        OrderEntity saved = orderRepository.save(entity);
        return toResponse(saved);
    }

    public List<OrderResponse> getAllOrders() {
        return orderRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    private OrderResponse toResponse(OrderEntity entity) {
        return new OrderResponse(
                entity.getId(),
                entity.getOrderNumber(),
                entity.getCustomerName(),
                entity.getTotalAmount(),
                entity.getStatus()
        );
    }
}
