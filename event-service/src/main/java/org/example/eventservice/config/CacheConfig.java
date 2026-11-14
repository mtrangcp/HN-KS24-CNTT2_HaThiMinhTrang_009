//package org.example.eventservice.config;
//
//import org.springframework.boot.cache.autoconfigure.RedisCacheManagerBuilderCustomizer;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.data.redis.cache.RedisCacheWriter;
//import org.springframework.data.redis.connection.RedisConnectionFactory;
//
//@Configuration
//public class CacheConfig {
//    @Bean
//    RedisCacheManagerBuilderCustomizer cacheCustomizer(
//            RedisConnectionFactory connectionFactory) {
//
//        return builder -> builder.cacheWriter(
//                RedisCacheWriter.lockingRedisCacheWriter(
//                        connectionFactory
//                )
//        );
//    }
//}
