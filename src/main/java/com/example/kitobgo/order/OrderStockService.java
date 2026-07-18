package com.example.kitobgo.order;

import com.example.kitobgo.common.ConflictException;
import com.example.kitobgo.common.NotFoundException;
import com.example.kitobgo.order.dto.OrderItemRequest;
import com.example.kitobgo.product.Product;
import com.example.kitobgo.product.ProductRepository;
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

            int quantity = itemReq.quantity() != null ? itemReq.quantity() : 1;
            if (quantity <= 0) {
                throw new IllegalArgumentException("Miqdor musbat bo'lishi kerak: " + product.getTitle());
            }

            Integer stock = product.getStockQuantity();
            int available = stock != null ? stock : 0;
            if (available < quantity) {
                throw new ConflictException("Yetarli zaxira yo'q: " + product.getTitle()
                        + " (mavjud: " + available + ", so'ralgan: " + quantity + ")");
            }
            product.setStockQuantity(available - quantity);

            OrderItem item = OrderItem.builder()
                    .product(product)
                    .quantity(quantity)
                    .priceAtPurchase(effectivePrice(product))
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
            Integer stock = product.getStockQuantity();
            product.setStockQuantity((stock != null ? stock : 0) + quantity);
        }
    }

    /** Mahsulotning haqiqiy narxi: chegirma bo'lsa chegirma narxi, aks holda asl narx. */
    private Integer effectivePrice(Product product) {
        Integer price = product.getPrice();
        Integer discount = product.getDiscountPrice();
        return (discount != null && price != null && discount < price) ? discount : price;
    }
}
