package com.example.kitobgo.common;

import org.springframework.data.domain.Page;

import java.util.List;

/**
 * Sahifalangan javob uchun barqaror (stabil) ko'rinish.
 * Spring'ning ichki {@code Page} obyektini to'g'ridan-to'g'ri qaytarish o'rniga ishlatiladi.
 */
public record PagedResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean last
) {
    public static <T> PagedResponse<T> from(Page<T> page) {
        return new PagedResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isLast()
        );
    }
}
