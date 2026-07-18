package com.example.kitobgo.storage;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.UUID;

/**
 * S3-mos bulut saqlash ({@code app.storage.type=s3}) — AWS S3, Cloudflare R2 va MinIO
 * bilan ishlaydi (S3 API umumiy). Fayl {@code products/<uuid>.<tur>} kaliti bilan
 * yuklanadi, DB'da esa to'liq ochiq URL saqlanadi ({@code app.storage.s3.public-url}
 * + kalit) — server almashsa ham rasmlar joyida qoladi.
 * <p>
 * R2/MinIO uchun {@code endpoint} beriladi va path-style ishlatiladi; AWS'ning o'zida
 * {@code endpoint} bo'sh qoldiriladi.
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "app.storage.type", havingValue = "s3")
public class S3FileStorageService implements FileStorageService {

    /** Rasm kalitlari shu prefiks ostida — bucket'da boshqa turdagi fayllardan ajralib turadi. */
    private static final String KEY_PREFIX = "products/";

    private final S3Client s3;
    private final String bucket;
    private final String publicUrlBase;

    public S3FileStorageService(
            @Value("${app.storage.s3.endpoint:}") String endpoint,
            @Value("${app.storage.s3.region:auto}") String region,
            @Value("${app.storage.s3.bucket}") String bucket,
            @Value("${app.storage.s3.access-key}") String accessKey,
            @Value("${app.storage.s3.secret-key}") String secretKey,
            @Value("${app.storage.s3.public-url:}") String publicUrl) {

        this.bucket = bucket;

        var builder = S3Client.builder()
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(accessKey, secretKey)));
        if (!endpoint.isBlank()) {
            // R2/MinIO — maxsus endpoint; path-style ikkalasida ham ishonchli ishlaydi.
            builder.endpointOverride(URI.create(endpoint)).forcePathStyle(true);
        }
        this.s3 = builder.build();

        // Ochiq URL berilmasa path-style manzilga tushamiz (masalan MinIO dev).
        String base = !publicUrl.isBlank() ? publicUrl
                : !endpoint.isBlank() ? endpoint + "/" + bucket
                : null;
        if (base == null) {
            throw new IllegalStateException(
                    "app.storage.s3.public-url yoki app.storage.s3.endpoint berilishi shart");
        }
        this.publicUrlBase = base.endsWith("/") ? base.substring(0, base.length() - 1) : base;

        if (!endpoint.isBlank()) {
            ensureBucket();
        }
    }

    /**
     * MinIO kabi o'zi boshqariladigan xizmatda bucket'ni tayyorlab qo'yadi: yo'q bo'lsa
     * yaratadi va rasmlarni anonim o'qishga ochadi (aks holda URL'lar 403 qaytaradi).
     * To'liq best-effort: xizmat hali ko'tarilmagan bo'lsa ham dastur yiqilmaydi —
     * faqat ogohlantirish log'i (birinchi yuklashgacha MinIO ishga tushgan bo'lishi kerak).
     * Faqat maxsus endpoint'da chaqiriladi — AWS'da bucket oldindan tayyorlanadi.
     */
    private void ensureBucket() {
        try {
            try {
                s3.headBucket(b -> b.bucket(bucket));
            } catch (NoSuchBucketException e) {
                s3.createBucket(b -> b.bucket(bucket));
                log.info("Bucket yaratildi: {}", bucket);
            }
            s3.putBucketPolicy(b -> b.bucket(bucket).policy("""
                    {
                      "Version": "2012-10-17",
                      "Statement": [{
                        "Effect": "Allow",
                        "Principal": "*",
                        "Action": "s3:GetObject",
                        "Resource": "arn:aws:s3:::%s/%s*"
                      }]
                    }
                    """.formatted(bucket, KEY_PREFIX)));
        } catch (Exception e) {
            log.warn("Bucket'ni tayyorlab bo'lmadi ({}): {} — xizmat ishga tushganda "
                    + "qo'lda tekshiring", bucket, e.getMessage());
        }
    }

    @Override
    public String store(MultipartFile file) {
        ImageType type = ImageType.requireImage(file);

        String key = KEY_PREFIX + UUID.randomUUID() + type.extension();
        try (InputStream in = file.getInputStream()) {
            s3.putObject(PutObjectRequest.builder()
                            .bucket(bucket)
                            .key(key)
                            .contentType(type.contentType())
                            // Nom UUID — mazmun hech qachon o'zgarmaydi, uzoq kesh xavfsiz.
                            .cacheControl("public, max-age=31536000, immutable")
                            .build(),
                    RequestBody.fromInputStream(in, file.getSize()));
        } catch (IOException e) {
            throw new IllegalStateException("Faylni bulutga yuklashda xato: " + key, e);
        }
        return publicUrlBase + "/" + key;
    }

    @Override
    public void delete(String url) {
        String prefix = publicUrlBase + "/" + KEY_PREFIX;
        if (url == null || !url.startsWith(prefix)) {
            return;   // tashqi yoki eski (lokal) URL — bizniki emas
        }
        String key = url.substring(publicUrlBase.length() + 1);
        try {
            s3.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(key).build());
        } catch (Exception e) {
            log.warn("Faylni bulutdan o'chirib bo'lmadi: {}", key, e);
        }
    }
}
