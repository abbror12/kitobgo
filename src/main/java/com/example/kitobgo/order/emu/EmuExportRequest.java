package com.example.kitobgo.order.emu;

import java.util.List;
import java.util.UUID;

/**
 * EMU eksport so'rovi. {@code orderIds} — admin ro'yxatdan tanlagan buyurtmalar;
 * bo'sh yoki berilmasa ro'yxatdagi hammasi eksport qilinadi.
 */
public record EmuExportRequest(List<UUID> orderIds) {
}
