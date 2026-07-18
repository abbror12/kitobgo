package com.example.kitobgo.storage;

import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

/**
 * Rasm fayllarini saqlash. Implementatsiya {@code app.storage.type} bilan tanlanadi:
 * {@code local} (default) — {@link LocalFileStorageService}, {@code s3} —
 * {@link S3FileStorageService} (AWS S3 / Cloudflare R2 / MinIO).
 * <p>
 * Har ikkala implementatsiya ham klient yuborgan MIME/kengaytmaga ishonmaydi — tur
 * {@link ImageType#requireImage} orqali mazmundan aniqlanadi va nom/kengaytmani
 * server o'zi belgilaydi.
 */
public interface FileStorageService {

    /** Rasm faylini saqlaydi va uni ko'rsatish uchun URL qaytaradi. */
    String store(MultipartFile file);

    /**
     * Saqlangan faylni darhol o'chiradi (best-effort: xato bo'lsa faqat log).
     * Bu servisga tegishli bo'lmagan URL'lar e'tiborsiz qoldiriladi.
     */
    void delete(String url);

    /**
     * Faylni joriy transaksiya muvaffaqiyatli yakunlangach o'chiradi — rollback bo'lsa
     * fayl joyida qoladi va DB yozuvi unga ishora qilaveradi. Transaksiya bo'lmasa
     * darhol o'chiradi.
     */
    default void deleteAfterCommit(String url) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    delete(url);
                }
            });
        } else {
            delete(url);
        }
    }
}
