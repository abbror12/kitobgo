package com.example.kitobgo.product;

import com.example.kitobgo.common.ConflictException;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProductTest {

    @Test
    void createAuditFieldsAndDefaultStatus() {
        Product product = Product.builder().build();

        product.onCreate();

        assertThat(product.getStatus()).isEqualTo(ProductStatus.ACTIVE);
        assertThat(product.getCreatedAt()).isNotNull();
        assertThat(product.getUpdatedAt()).isNotNull();
        assertThat(product.getCategories()).isEmpty();
    }

    @Test
    void updateRefreshesOnlyUpdatedAt() {
        Product product = Product.builder().build();
        product.onCreate();
        LocalDateTime createdAt = product.getCreatedAt();
        LocalDateTime firstUpdatedAt = product.getUpdatedAt();

        product.onUpdate();

        assertThat(product.getCreatedAt()).isEqualTo(createdAt);
        assertThat(product.getUpdatedAt()).isAfterOrEqualTo(firstUpdatedAt);
    }

    @Test
    void archiveChangesStatusWithoutRemovingProductData() {
        Product product = Product.builder()
                .title("O'tkan kunlar")
                .status(ProductStatus.ACTIVE)
                .build();

        product.archive();

        assertThat(product.getStatus()).isEqualTo(ProductStatus.ARCHIVED);
        assertThat(product.getTitle()).isEqualTo("O'tkan kunlar");

        product.changeStatus(ProductStatus.ACTIVE);
        assertThat(product.getStatus()).isEqualTo(ProductStatus.ACTIVE);
    }

    @Test
    void calculatesEffectivePriceAndDiscount() {
        Product product = Product.builder()
                .price(100_000)
                .discountPrice(80_000)
                .build();

        assertThat(product.hasDiscount()).isTrue();
        assertThat(product.effectivePrice()).isEqualTo(80_000);

        product.setDiscountPrice(110_000);

        assertThat(product.hasDiscount()).isFalse();
        assertThat(product.effectivePrice()).isEqualTo(100_000);
    }

    @Test
    void decreasesAndRestoresStock() {
        Product product = Product.builder()
                .title("O'tkan kunlar")
                .stockQuantity(5)
                .build();

        product.decreaseStock(2);
        assertThat(product.getStockQuantity()).isEqualTo(3);
        assertThat(product.canFulfill(3)).isTrue();
        assertThat(product.isInStock()).isTrue();

        product.increaseStock(2);
        assertThat(product.getStockQuantity()).isEqualTo(5);

        assertThatThrownBy(() -> product.decreaseStock(6))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Yetarli zaxira yo'q");
    }
}
