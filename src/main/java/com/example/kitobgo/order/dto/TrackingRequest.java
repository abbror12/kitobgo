package com.example.kitobgo.order.dto;

import jakarta.validation.constraints.NotBlank;

public record TrackingRequest(
        @NotBlank(message = "Trek-raqam bo'sh bo'lishi mumkin emas")
        String trackingNumber
) {
}
