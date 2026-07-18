package com.example.kitobgo.product.dto;

import jakarta.validation.constraints.Positive;

/**
 * Kitobga chegirma qo'yish/olib tashlash so'rovi.
 *
 * <p>{@code discountPrice} — chegirmali narx (so'mda). {@code null} berilsa
 * chegirma olib tashlanadi. Aks holda asl narxdan qat'iy past bo'lishi kerak.
 */
public record DiscountRequestDto(
        @Positive(message = "Chegirma narxi musbat bo'lishi kerak")
        Integer discountPrice
) {
}
