package net.maritimeconnectivity.serviceregistry.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import com.github.benmanes.caffeine.cache.Caffeine;

import java.time.Duration;
import java.util.concurrent.TimeUnit;


@Configuration
@EnableCaching
public class CacheConfig {
    public static final String CACHE_NAME = "search-results-by-transactionId";

    @Bean Caffeine<Object, Object> caffeine() {
        return Caffeine.newBuilder()
                .expireAfterWrite(30, TimeUnit.SECONDS)
                .maximumSize(10000);
    }

    @Bean
    CacheManager cacheManager(Caffeine<Object, Object> caffeine) {
        CaffeineCacheManager mgr = new CaffeineCacheManager(CACHE_NAME);
        mgr.setCaffeine(caffeine);
        return mgr;
    }

}
