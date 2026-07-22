package com.example.kitobgo.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.List;

/**
 * Tez-tez o'qiladigan, kam o'zgaradigan ma'lumotlar uchun lokal cache.
 * Bir nechta app instance ishlatilganda yozish metodlaridagi eviction har bir
 * instancega tarqalmaydi; o'sha bosqichda shu nomlarni RedisCacheManager'ga
 * ko'chirish mumkin.
 */
@Configuration
public class CacheConfig {

    public static final String CATEGORIES = "categories";
    public static final String PRODUCT_DETAILS = "productDetails";
    public static final String PRODUCT_CATALOG = "productCatalog";
    public static final String USER_DETAILS = "userDetails";

    @Bean
    public CacheManager cacheManager() {
        SimpleCacheManager manager = new SimpleCacheManager();
        manager.setCaches(List.of(
                cache(CATEGORIES, 200, Duration.ofMinutes(30)),
                cache(PRODUCT_DETAILS, 10_000, Duration.ofMinutes(5)),
                cache(PRODUCT_CATALOG, 2_000, Duration.ofSeconds(60)),
                cache(USER_DETAILS, 10_000, Duration.ofSeconds(30))
        ));
        return manager;
    }

    private CaffeineCache cache(String name, long maximumSize, Duration ttl) {
        return new CaffeineCache(name, Caffeine.newBuilder()
                .maximumSize(maximumSize)
                .expireAfterWrite(ttl)
                .recordStats()
                .build());
    }
}
