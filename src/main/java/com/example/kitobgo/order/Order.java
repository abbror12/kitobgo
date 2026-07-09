package com.example.kitobgo.order;

import com.example.kitobgo.product.Product;
import com.example.kitobgo.user.User;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "orders")
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @ManyToOne
    private User operator;
    @ManyToOne
    private User courier;
    @ManyToOne
    private Product product;
    private String customerName;
    private String customerPhone;
    private String address;
    private OrderStatus status;
    private LocalDateTime createdAt;
}
