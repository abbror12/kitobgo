package com.example.kitobgo.order;

import com.example.kitobgo.common.ConflictException;
import com.example.kitobgo.common.NotFoundException;
import com.example.kitobgo.order.dto.OrderItemRequest;
import com.example.kitobgo.product.Product;
import com.example.kitobgo.product.ProductRepository;
import com.example.kitobgo.product.ProductStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Buyurtma bilan zaxira (stock) hisobining bog'lanish nuqtasi: qatorlarni yaratishda
 * kamaytirish va bekor qilishda qaytarish — bir juft teskari amal bitta joyda.
 * <p>
 * Metodlar chaqiruvchining transaksiyasi ichida ishlashi shart: mahsulotlar managed
 * obyekt bo'lgani uchun o'zgarishlar transaksiya yakunida avtomatik saqlanadi
 * (dirty checking).
 */
@Component
@RequiredArgsConstructor
public class OrderStockService {

    private final ProductRepository productRepository;

    /** Buyurtma qatorlarini yaratadi: zaxirani tekshiradi, kamaytiradi va narxni muzlatadi. */
    public void populateItems(Order order, List<OrderItemRequest> items) {
        for (OrderItemRequest itemReq : items) {
            Product product = productRepository.findById(itemReq.productId())
                    .orElseThrow(() -> new NotFoundException("Mahsulot topilmadi: " + itemReq.productId()));

            if (product.getStatus() != ProductStatus.ACTIVE) {
                throw new ConflictException("Mahsulot sotuvda emas: " + product.getTitle());
            }

            int quantity = itemReq.quantity() != null ? itemReq.quantity() : 1;
            product.decreaseStock(quantity);

            OrderItem item = OrderItem.builder()
                    .product(product)
                    .quantity(quantity)
                    .priceAtPurchase(product.effectivePrice())
                    .build();
            order.addItem(item);
        }
    }

    /**
     * Buyurtmadagi kitoblarni zaxiraga qaytaradi — {@link #populateItems} dagi
     * kamaytirishning teskarisi.
     */
    public void restoreStock(Order order) {
        for (OrderItem item : order.getItems()) {
            Product product = item.getProduct();
            Integer quantity = item.getQuantity();
            if (product == null || quantity == null) {
                continue;
            }
            product.increaseStock(quantity);
        }
    }
}
