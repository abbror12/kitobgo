package com.example.kitobgo.product;

import jakarta.persistence.*;

import java.util.UUID;

@Entity
@Table(name = "products")
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    private String name;
    private Integer price;
    private Integer stockQuantity;
}
