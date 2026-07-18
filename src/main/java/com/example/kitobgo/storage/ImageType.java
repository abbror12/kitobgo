package com.example.kitobgo.storage;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;

/**
 * Qo'llab-quvvatlanadigan rasm turlari va ularni faylning bosh baytlaridan (magic bytes)
 * aniqlash. Klient yuborgan MIME turi va fayl nomiga ishonilmaydi — tur faqat mazmundan
 * aniqlanadi, kengaytma va Content-Type shu yerdan olinadi.
 */
public enum ImageType {

    JPEG(".jpg", "image/jpeg"),
    PNG(".png", "image/png"),
    GIF(".gif", "image/gif"),
    WEBP(".webp", "image/webp");

    private final String extension;
    private final String contentType;

    ImageType(String extension, String contentType) {
        this.extension = extension;
        this.contentType = contentType;
    }

    public String extension() {
        return extension;
    }

    public String contentType() {
        return contentType;
    }

    /**
     * Fayl bo'sh emasligini va mazmuni haqiqiy rasm ekanini tekshirib, turini qaytaradi.
     * Rasm bo'lmasa {@link IllegalArgumentException}.
     */
    public static ImageType requireImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Fayl bo'sh");
        }
        ImageType type = detect(file);
        if (type == null) {
            throw new IllegalArgumentException(
                    "Faqat rasm fayllari qabul qilinadi (jpeg, png, webp, gif) — "
                            + "fayl mazmuni rasm emas");
        }
        return type;
    }

    /** Turini bosh baytlardan aniqlaydi; qo'llanmaydigan tur uchun {@code null}. */
    private static ImageType detect(MultipartFile file) {
        byte[] head;
        try (InputStream in = file.getInputStream()) {
            head = in.readNBytes(12);
        } catch (IOException e) {
            throw new IllegalStateException("Faylni o'qishda xato", e);
        }

        if (startsWith(head, 0, 0xFF, 0xD8, 0xFF)) {
            return JPEG;
        }
        if (startsWith(head, 0, 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A)) {
            return PNG;
        }
        if (startsWith(head, 0, 'G', 'I', 'F', '8')) {
            return GIF;
        }
        if (startsWith(head, 0, 'R', 'I', 'F', 'F') && startsWith(head, 8, 'W', 'E', 'B', 'P')) {
            return WEBP;
        }
        return null;
    }

    private static boolean startsWith(byte[] data, int offset, int... expected) {
        if (data.length < offset + expected.length) {
            return false;
        }
        for (int i = 0; i < expected.length; i++) {
            if (data[offset + i] != (byte) expected[i]) {
                return false;
            }
        }
        return true;
    }
}
