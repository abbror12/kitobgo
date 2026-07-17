package com.example.kitobgo.order.dto;

import com.example.kitobgo.order.DeliveryMethod;
import com.example.kitobgo.order.Order;
import com.example.kitobgo.order.emu.EmuParcelName;
import com.example.kitobgo.order.emu.EmuShipment;

import java.time.LocalDateTime;

/**
 * Buyurtmaning EMU tomoni. COURIER buyurtmalarida umuman bo'lmaydi (null).
 * <p>
 * EMU buyurtmasi hali eksport qilinmagan bo'lsa ({@code exportedAt == null}) —
 * {@code parcelName} qatorlardan hisoblangan <b>taxminiy</b> nom: admin EMU bo'limida
 * shuni ko'radi. Eksportdan keyin esa u {@link EmuShipment} da muzlatilgan haqiqiy nom.
 */
public record EmuInfo(
        String parcelName,
        String trackingNumber,
        LocalDateTime exportedAt
) {
    /** COURIER buyurtmasi uchun null qaytaradi. */
    public static EmuInfo from(Order order) {
        if (order.getDeliveryMethod() != DeliveryMethod.EMU) {
            return null;
        }
        EmuShipment shipment = order.getEmuShipment();
        if (shipment == null) {
            return new EmuInfo(EmuParcelName.of(order), null, null);
        }
        return new EmuInfo(
                shipment.getParcelName(),
                shipment.getTrackingNumber(),
                shipment.getExportedAt());
    }
}
