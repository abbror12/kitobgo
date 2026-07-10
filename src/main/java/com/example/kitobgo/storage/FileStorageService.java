package com.example.kitobgo.storage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import java.util.UUID;

/**
 * Yuklangan fayllarni lokal diskka saqlaydigan servis.
 *
 * <p>Fayl {@code app.upload.dir} papkasiga tasodifiy nom bilan saqlanadi va uni
 * ochish uchun nisbiy URL (masalan {@code /uploads/<uuid>.jpg}) qaytariladi.
 *
 * <p>Production/marketplace uchun faqat shu klass ichini MinIO/S3 ga almashtirish
 * kifoya — qolgan kod (service, controller) o'zgarmaydi.
 */
@Service
public class FileStorageService {

    /** Ruxsat etilgan rasm turlari (MIME). */
    private static final Set<String> ALLOWED_IMAGE_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/webp",
            "image/gif"
    );

    private final Path root;

    public FileStorageService(@Value("${app.upload.dir:uploads}") String uploadDir) {
        this.root = Paths.get(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(root);
        } catch (IOException e) {
            throw new IllegalStateException("Yuklash papkasini yaratib bo'lmadi: " + root, e);
        }
    }

    /**
     * Faylni saqlaydi va uni ochish uchun nisbiy URL qaytaradi.
     *
     * @return masalan {@code /uploads/3f2a...c1.jpg}
     */
    public String store(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Fayl bo'sh");
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_IMAGE_TYPES.contains(contentType.toLowerCase())) {
            throw new IllegalArgumentException(
                    "Faqat rasm fayllari qabul qilinadi (jpeg, png, webp, gif). Yuborilgan tur: " + contentType);
        }

        // Fayl nomini o'zimiz UUID bilan yasaymiz — path traversal xavfi yo'q.
        String filename = UUID.randomUUID() + extensionOf(file.getOriginalFilename());
        try {
            file.transferTo(root.resolve(filename));
        } catch (IOException e) {
            throw new IllegalStateException("Faylni saqlashda xato: " + filename, e);
        }
        return "/uploads/" + filename;
    }

    private String extensionOf(String originalName) {
        if (originalName == null) {
            return "";
        }
        int dot = originalName.lastIndexOf('.');
        return dot >= 0 ? originalName.substring(dot) : "";
    }
}
