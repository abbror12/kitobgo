package com.example.kitobgo.order;

import com.example.kitobgo.common.ConflictException;
import com.example.kitobgo.order.dto.OrderItemRequest;
import com.example.kitobgo.product.Product;
import com.example.kitobgo.product.ProductRepository;
import com.example.kitobgo.product.ProductStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OrderStockServiceTest {

    @ParameterizedTest
    @EnumSource(value = ProductStatus.class, names = {"DRAFT", "ARCHIVED"})
    void nonActiveProductCannotBeOrdered(ProductStatus status) {
        ProductRepository repository = mock(ProductRepository.class);
        Product product = Product.builder()
                .id(42L)
                .title("O'tkan kunlar")
                .status(status)
                .stockQuantity(10)
                .build();
        when(repository.findById(42L)).thenReturn(Optional.of(product));

        OrderStockService service = new OrderStockService(repository);

        assertThatThrownBy(() -> service.populateItems(
                Order.builder().build(),
                List.of(new OrderItemRequest(42L, 1))))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Mahsulot sotuvda emas");
    }

    @Test
    void activeProductUsesDomainPriceAndStockRules() {
        ProductRepository repository = mock(ProductRepository.class);
        Product product = Product.builder()
                .id(42L)
                .title("O'tkan kunlar")
                .status(ProductStatus.ACTIVE)
                .price(100_000)
                .discountPrice(80_000)
                .stockQuantity(5)
                .build();
        when(repository.findById(42L)).thenReturn(Optional.of(product));
        Order order = Order.builder().build();

        OrderStockService service = new OrderStockService(repository);
        service.populateItems(order, List.of(new OrderItemRequest(42L, 2)));

        assertThat(product.getStockQuantity()).isEqualTo(3);
        assertThat(order.getItems()).singleElement()
                .satisfies(item -> assertThat(item.getPriceAtPurchase()).isEqualTo(80_000));

        service.restoreStock(order);
        assertThat(product.getStockQuantity()).isEqualTo(5);
    }
}
