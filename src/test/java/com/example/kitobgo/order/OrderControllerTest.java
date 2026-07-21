package com.example.kitobgo.order;

import com.example.kitobgo.order.dto.OrderItemRequest;
import com.example.kitobgo.order.dto.OrderRequestDto;
import com.example.kitobgo.order.emu.EmuService;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OrderControllerTest {

    @Test
    void firstRequestReturnsCreatedAndReplayReturnsOk() {
        OrderCreationService creationService = mock(OrderCreationService.class);
        OrderController controller = new OrderController(
                creationService,
                mock(OrderStatusService.class),
                mock(OrderAssignmentService.class),
                mock(OrderDeliveryService.class),
                mock(OrderQueryService.class),
                mock(EmuService.class));
        UUID key = UUID.randomUUID();
        OrderRequestDto request = new OrderRequestDto(
                List.of(new OrderItemRequest(42L, 1)),
                "Ali", "+998901112233", Region.ANDIJAN);

        when(creationService.create(request, key))
                .thenReturn(new OrderCreationResult(null, true))
                .thenReturn(new OrderCreationResult(null, false));

        var created = controller.create(key, request);
        var replayed = controller.create(key, request);

        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(created.getHeaders().getFirst("Idempotency-Replayed")).isEqualTo("false");
        assertThat(replayed.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(replayed.getHeaders().getFirst("Idempotency-Replayed")).isEqualTo("true");
    }
}
