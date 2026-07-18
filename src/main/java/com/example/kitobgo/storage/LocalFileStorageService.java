package com.example.kitobgo.storage;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

/**
 * Lokal disk saqlash ({@code app.storage.type=local}, default) — fayllar
 * {@code app.upload.dir} papkasida, {@code /uploads/**} orqali xizmat qilinadi
 * ({@code WebConfig}). Dev va bitta serverli o'rnatish uchun.
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "app.storage.type", havingValue = "local", matchIfMissing = true)
public class LocalFileStorageService implements FileStorageService {

    /** Saqlangan fayllar shu prefiks bilan xizmat qilinadi; tashqi URL'lar bizniki emas. */
    private static final String URL_PREFIX = "/uploads/";

    private final Path root;

    public LocalFileStorageService(@Value("${app.upload.dir:uploads}") String uploadDir) {
        this.root = Paths.get(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(root);
        } catch (IOException e) {
            throw new IllegalStateException("Yuklash papkasini yaratib bo'lmadi: " + root, e);
        }
    }

    @Override
    public String store(MultipartFile file) {
        ImageType type = ImageType.requireImage(file);

        // Nom UUID — path traversal xavfi yo'q.
        String filename = UUID.randomUUID() + type.extension();
        try {
            file.transferTo(root.resolve(filename));
        } catch (IOException e) {
            throw new IllegalStateException("Faylni saqlashda xato: " + filename, e);
        }
        return URL_PREFIX + filename;
    }

    @Override
    public void delete(String url) {
        if (url == null || !url.startsWith(URL_PREFIX)) {
            return;
        }
        Path target = root.resolve(url.substring(URL_PREFIX.length())).normalize();
        // Faqat root papkaning bevosita ichidagi fayl — traversal himoyasi.
        if (!root.equals(target.getParent())) {
            log.warn("Shubhali fayl yo'li o'chirilmadi: {}", url);
            return;
        }
        try {
            Files.deleteIfExists(target);
        } catch (IOException e) {
            log.warn("Faylni diskdan o'chirib bo'lmadi: {}", target, e);
        }
    }
}
