package com.smichelotti.mtg.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;

import java.time.Duration;
import java.util.Map;

@Configuration
public class RedisConfig {

        // Scoped to our own DTOs (+ the JDK container/boxed types Jackson needs to wrap
        // them) instead of allowIfSubType(Object.class), which accepted any class on the
        // classpath as a polymorphic deserialization target. See Fase 6 security review.
        private static final String DTO_PACKAGE = "com.smichelotti.mtg.dto";

        @Bean
        public RedisCacheManager cacheManager(
                        RedisConnectionFactory connectionFactory) {

                ObjectMapper objectMapper = new ObjectMapper();

                objectMapper.activateDefaultTyping(
                                BasicPolymorphicTypeValidator.builder()
                                                .allowIfSubType(DTO_PACKAGE)
                                                .allowIfSubType("java.util.")
                                                .allowIfSubType("java.lang.")
                                                .build(),
                                ObjectMapper.DefaultTyping.NON_FINAL,
                                JsonTypeInfo.As.PROPERTY);

                GenericJackson2JsonRedisSerializer serializer = new GenericJackson2JsonRedisSerializer(objectMapper);

                RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                                .entryTtl(Duration.ofMinutes(10))
                                .serializeValuesWith(
                                                RedisSerializationContext.SerializationPair
                                                                .fromSerializer(serializer));

                Map<String, RedisCacheConfiguration> perCacheTtl = Map.of(
                                "cards", defaultConfig.entryTtl(Duration.ofMinutes(15)),
                                "cardsByEdition", defaultConfig.entryTtl(Duration.ofMinutes(15)),
                                "scryfallSets", defaultConfig.entryTtl(Duration.ofHours(24)),
                                "mtgstocksSetsIndex", defaultConfig.entryTtl(Duration.ofHours(24)),
                                "mtgstocksSetDetail", defaultConfig.entryTtl(Duration.ofHours(6)));

                return RedisCacheManager.builder(connectionFactory)
                                .cacheDefaults(defaultConfig)
                                .withInitialCacheConfigurations(perCacheTtl)
                                .build();
        }
}
