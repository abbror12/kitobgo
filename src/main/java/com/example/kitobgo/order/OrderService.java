package com.example.kitobgo.order;

import com.example.kitobgo.common.NotFoundException;
import com.example.kitobgo.order.assignment.OperatorAssignmentStrategy;
import com.example.kitobgo.order.dto.OrderItemRequest;
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
        if (dto.items() == null || dto.items().isEmpty()) {
            throw new IllegalArgumentException("Buyurtmada kamida bitta mahsulot bo'lishi kerak");
        }

        User operator = operatorAssignmentStrategy.assignOperator();

        Order order = Order.builder()
                .operator(operator)
                .customerName(dto.customerName())
                .customerPhone(dto.customerPhone())
                .address(dto.address())
                .status(OrderStatus.NEW)
                .build();

        for (OrderItemRequest itemReq : dto.items()) {
            Product product = productRepository.findById(itemReq.productId())
                    .orElseThrow(() -> new NotFoundException("Mahsulot topilmadi: " + itemReq.productId()));

            int quantity = itemReq.quantity() != null ? itemReq.quantity() : 1;
            if (quantity <= 0) {
                throw new IllegalArgumentException("Miqdor musbat bo'lishi kerak: " + product.getTitle());
            }

            OrderItem item = OrderItem.builder()
                    .product(product)
                    .quantity(quantity)
                    .priceAtPurchase(effectivePrice(product))
                    .build();
            order.addItem(item);
        }

        Order saved = orderRepository.save(order);
        return OrderResponseDto.from(saved);
    }

    /** Mahsulotning haqiqiy narxi: chegirma bo'lsa chegirma narxi, aks holda asl narx. */
    private Integer effectivePrice(Product product) {
        Integer price = product.getPrice();
        Integer discount = product.getDiscountPrice();
        return (discount != null && price != null && discount < price) ? discount : price;
    }

    @Transactional(readOnly = true)
    public OrderResponseDto getById(UUID id) {
        Order order = orderRepository.findWithItemsById(id)
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
