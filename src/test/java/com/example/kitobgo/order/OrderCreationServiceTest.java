package com.example.kitobgo.order;

import com.example.kitobgo.common.ConflictException;
import com.example.kitobgo.notification.PushNotificationService;
import com.example.kitobgo.order.assignment.OperatorAssignmentStrategy;
import com.example.kitobgo.order.dto.OrderItemRequest;
import com.example.kitobgo.order.dto.OrderRequestDto;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class OrderCreationServiceTest {

    @Test
    void missingIdempotencyKeyIsRejected() {
        Fixture fixture = new Fixture();

        assertThatThrownBy(() -> fixture.service.create(
                request("Ali", List.of(new OrderItemRequest(42L, 1))), null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Idempotency-Key");

        verifyNoInteractions(fixture.repository, fixture.stockService, fixture.assignmentStrategy);
    }

    @Test
    void firstRequestCreatesOrderWithIdempotencyData() {
        Fixture fixture = new Fixture();
        UUID key = UUID.randomUUID();
        OrderRequestDto request = request("Ali", List.of(new OrderItemRequest(42L, 2)));
        when(fixture.repository.findByClientRequestId(key)).thenReturn(Optional.empty());
        when(fixture.repository.save(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            order.setId(UUID.randomUUID());
            return order;
        });

        OrderCreationResult result = fixture.service.create(request, key);

        assertThat(result.created()).isTrue();
        ArgumentCaptor<Order> captor = ArgumentCaptor.forClass(Order.class);
        verify(fixture.repository).save(captor.capture());
        assertThat(captor.getValue().getClientRequestId()).isEqualTo(key);
        assertThat(captor.getValue().getClientRequestHash())
                .isEqualTo(OrderRequestFingerprint.sha256(request));
        verify(fixture.stockService).populateItems(captor.getValue(), request.items());
    }

    @Test
    void retryReturnsExistingOrderWithoutDecreasingStockAgain() {
        Fixture fixture = new Fixture();
        UUID key = UUID.randomUUID();
        OrderRequestDto request = request("Ali", List.of(new OrderItemRequest(42L, 1)));
        Order existing = existingOrder(key, OrderRequestFingerprint.sha256(request));
        when(fixture.repository.findByClientRequestId(key)).thenReturn(Optional.of(existing));

        OrderCreationResult result = fixture.service.create(request, key);

        assertThat(result.created()).isFalse();
        assertThat(result.order().id()).isEqualTo(existing.getId());
        verify(fixture.idempotencyLock).acquire(key);
        verify(fixture.repository, never()).save(any());
        verifyNoInteractions(fixture.stockService, fixture.assignmentStrategy);
    }

    @Test
    void reusedKeyWithDifferentPayloadIsRejected() {
        Fixture fixture = new Fixture();
        UUID key = UUID.randomUUID();
        OrderRequestDto first = request("Ali", List.of(new OrderItemRequest(42L, 1)));
        OrderRequestDto changed = request("Vali", List.of(new OrderItemRequest(42L, 1)));
        when(fixture.repository.findByClientRequestId(key))
                .thenReturn(Optional.of(existingOrder(key, OrderRequestFingerprint.sha256(first))));

        assertThatThrownBy(() -> fixture.service.create(changed, key))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("boshqa checkout");

        verify(fixture.repository, never()).save(any());
        verifyNoInteractions(fixture.stockService, fixture.assignmentStrategy);
    }

    @Test
    void fingerprintDoesNotDependOnItemOrder() {
        OrderRequestDto first = request("Ali", List.of(
                new OrderItemRequest(42L, 1),
                new OrderItemRequest(7L, 2)));
        OrderRequestDto reordered = request("Ali", List.of(
                new OrderItemRequest(7L, 2),
                new OrderItemRequest(42L, 1)));

        assertThat(OrderRequestFingerprint.sha256(first))
                .isEqualTo(OrderRequestFingerprint.sha256(reordered));
    }

    private static OrderRequestDto request(String customerName, List<OrderItemRequest> items) {
        return new OrderRequestDto(items, customerName, "+998901112233", Region.ANDIJAN);
    }

    private static Order existingOrder(UUID key, String hash) {
        return Order.builder()
                .id(UUID.randomUUID())
                .clientRequestId(key)
                .clientRequestHash(hash)
                .customerName("Ali")
                .customerPhone("+998901112233")
                .region(Region.ANDIJAN)
                .source(OrderSource.WEBSITE)
                .status(OrderStatus.NEW)
                .build();
    }

    private static final class Fixture {
        private final OrderRepository repository = mock(OrderRepository.class);
        private final OperatorAssignmentStrategy assignmentStrategy = mock(OperatorAssignmentStrategy.class);
        private final OrderStockService stockService = mock(OrderStockService.class);
        private final OrderDeliveryService deliveryService = mock(OrderDeliveryService.class);
        private final PushNotificationService pushService = mock(PushNotificationService.class);
        private final OrderIdempotencyLock idempotencyLock = mock(OrderIdempotencyLock.class);
        private final OrderCreationService service = new OrderCreationService(
                repository,
                assignmentStrategy,
                stockService,
                deliveryService,
                pushService,
                idempotencyLock);
    }
}
