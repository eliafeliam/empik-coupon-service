package org.example.config.cache;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;
@Configuration
@EnableCaching(proxyTargetClass = true)
@SuppressWarnings("unused")
public class CacheConfig {
    public static final String GEOIP_CACHE_NAME = "geoIpCache";

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager manager = new CaffeineCacheManager(GEOIP_CACHE_NAME);
        manager.setCaffeine(
            Caffeine.newBuilder()
                    .expireAfterWrite(10, TimeUnit.MINUTES)
                    .maximumSize(10000)
        );
        return manager;
    }
}
