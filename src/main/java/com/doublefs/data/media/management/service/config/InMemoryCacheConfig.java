package com.doublefs.data.media.management.service.config;

import com.doublefs.data.media.management.common.constants.Constants;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;

import java.util.concurrent.TimeUnit;

@EnableCaching
@Configuration
public class InMemoryCacheConfig {
    @Bean
    public Caffeine cacheBuilder() {
        Caffeine cacheBuilder = Caffeine.newBuilder();

        cacheBuilder.expireAfterWrite(Constants.IN_MEMORY_CACHE_MINUTES, TimeUnit.MINUTES)
                .maximumSize(Constants.IN_MEMORY_CACHE_SIZE)
                .build();

        return cacheBuilder;
    }

    /**
     * expireAfterAccess: 当缓存项在指定的时间段内没有被读或写就会被回收。
     * expireAfterWrite：当缓存项在指定的时间段内没有更新就会被回收,如果我们认为缓存数据在一段时间后数据不再可用，那么可以使用该种策略。
     * refreshAfterWrite：当缓存项上一次更新操作之后的多久会被刷新。
     *
     * @return
     */
    @DependsOn({"cacheBuilder"})
    @Bean
    public CacheManager cacheManager(Caffeine caffeine) {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();

        cacheManager.setCaffeine(caffeine);

        return cacheManager;
    }
}
