package com.example.kitobgo.order;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OrderQueryServiceTest {

    @Test
    void searchUsesSpecificationAndKeepsPagination() {
        OrderRepository repository = mock(OrderRepository.class);
        PageRequest pageable = PageRequest.of(1, 20);
        UUID operatorId = UUID.randomUUID();
        when(repository.findAll(anySpecification(), eq(pageable))).thenReturn(Page.empty(pageable));
        OrderQueryService service = new OrderQueryService(
                repository, mock(OrderAuthorizationPolicy.class));

        var response = service.search("+99890", operatorId, OrderStatus.NEW, pageable);

        assertThat(response.page()).isEqualTo(1);
        assertThat(response.size()).isEqualTo(20);
        verify(repository).findAll(anySpecification(), eq(pageable));
    }

    private Specification<Order> anySpecification() {
        return any();
    }
}
