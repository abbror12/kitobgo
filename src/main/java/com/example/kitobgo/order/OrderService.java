package com.example.kitobgo.order;

import com.example.kitobgo.common.NotFoundException;
import com.example.kitobgo.order.assignment.OperatorAssignmentStrategy;
import com.example.kitobgo.order.dto.OrderRequestDto;
import com.example.kitobgo.order.dto.OrderResponseDto;
import com.example.kitobgo.product.Product;
import com.example.kitobgo.product.ProductRepository;
import com.example.kitobgo.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final OperatorAssignmentStrategy operatorAssignmentStrategy;

    @Transactional
    public OrderResponseDto create(OrderRequestDto dto) {
        Product product = productRepository.findById(dto.productId())
                .orElseThrow(() -> new NotFoundException("Mahsulot topilmadi: " + dto.productId()));

        User operator = operatorAssignmentStrategy.assignOperator();

        Order order = Order.builder()
                .operator(operator)
                .product(product)
                .customerName(dto.customerName())
                .customerPhone(dto.customerPhone())
                .address(dto.address())
                .status(OrderStatus.NEW)
                .build();

        Order saved = orderRepository.save(order);
        return OrderResponseDto.from(saved);
    }

    @Transactional(readOnly = true)
    public OrderResponseDto getById(UUID id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Buyurtma topilmadi: " + id));
        return OrderResponseDto.from(order);
    }

    @Transactional(readOnly = true)
    public List<OrderResponseDto> getAll() {
        return orderRepository.findAll().stream()
                .map(OrderResponseDto::from)
                .toList();
    }
}
