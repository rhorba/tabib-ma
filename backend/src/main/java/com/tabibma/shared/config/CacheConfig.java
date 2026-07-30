package com.tabibma.shared.config;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Configuration;

/**
 * Enables Spring's cache abstraction; TTL and the Redis backend come from
 * spring.cache.redis.time-to-live / spring.cache.type in application.yml (60s TTL per
 * docs/system-design-tabib-ma.md Section 5's search data flow).
 */
@Configuration
@EnableCaching
public class CacheConfig {
}
