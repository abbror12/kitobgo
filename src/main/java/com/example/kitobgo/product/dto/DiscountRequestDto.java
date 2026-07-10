package com.example.kitobgo.product.dto;

/**
 * Kitobga chegirma qo'yish/olib tashlash so'rovi.
 *
 * <p>{@code discountPrice} — chegirmali narx (so'mda). {@code null} berilsa
 * chegirma olib tashlanadi. Aks holda asl narxdan qat'iy past bo'lishi kerak.
 */
public record DiscountRequestDto(
        Integer discountPrice
) {
}
