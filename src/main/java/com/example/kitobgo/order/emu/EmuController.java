package com.example.kitobgo.order.emu;

import com.example.kitobgo.order.dto.OrderResponseDto;
import com.example.kitobgo.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.List;

/** EMU bo'limi (admin panel): topshiriladigan pasilkalar ro'yxati va Excel eksport. */
@RestController
@RequestMapping("/api/orders/emu")
@RequiredArgsConstructor
public class EmuController {

    private static final MediaType XLSX =
            MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

    private final EmuService emuService;

    /**
     * EMU ga topshiriladigan buyurtmalar — admin shu ro'yxatni ko'zdan kechiradi.
     * Har birida {@code emu.parcelName} — avtomatik yasalgan pasilka nomi.
     */
    @GetMapping
    public ResponseEntity<List<OrderResponseDto>> pending(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(emuService.pending(principal.user()));
    }

    /**
     * Tasdiqlab Excel yuklab beradi va buyurtmalarni EMU ga topshirilgan deb belgilaydi.
     * Tanasi ixtiyoriy: {@code orderIds} berilsa faqat o'shalar, bo'lmasa ro'yxatdagi hammasi.
     */
    @PostMapping("/export")
    public ResponseEntity<byte[]> export(
            @RequestBody(required = false) EmuExportRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        ExcelFile file = emuService.export(
                request != null ? request.orderIds() : null, principal.user());

        return ResponseEntity.ok()
                .contentType(XLSX)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(file.fileName(), StandardCharsets.UTF_8)
                        .build()
                        .toString())
                .body(file.content());
    }
}
