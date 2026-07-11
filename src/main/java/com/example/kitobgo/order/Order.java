package com.example.kitobgo.order;

import com.example.kitobgo.user.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "orders")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "operator_id")
    private User operator;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "courier_id")
    private User courier;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<OrderItem> items = new ArrayList<>();

    private String customerName;

    private String customerPhone;

    private String address;

    @Enumerated(EnumType.STRING)
    private OrderStatus status;

    /** Status o'zgargan vaqtlar — tegishli statusга o'tganda avtomatik to'ldiriladi. */
    private LocalDateTime confirmedAt;
    private LocalDateTime inDeliveryAt;
    private LocalDateTime deliveredAt;
    private LocalDateTime returnedAt;

    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.status == null) {
            this.status = OrderStatus.NEW;
        }
    }

    /** Buyurtma qatorini qo'shib, ikki tomonlama bog'lanishni o'rnatadi. */
    public void addItem(OrderItem item) {
        items.add(item);
        item.setOrder(this);
    }
}
