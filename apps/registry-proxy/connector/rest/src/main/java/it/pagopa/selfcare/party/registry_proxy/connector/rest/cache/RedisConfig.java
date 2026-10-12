package it.pagopa.selfcare.party.registry_proxy.connector.rest.cache;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.cache.RedisCacheWriter;
import org.springframework.data.redis.connection.RedisPassword;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.jedis.JedisClientConfiguration;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import redis.clients.jedis.JedisPoolConfig;

import java.time.Duration;

@Configuration
@Slf4j
@PropertySource("classpath:config/redis-cache.properties")
public class RedisConfig {

    @Value("${redis.url}")
    private String redisHostName;
    @Value("${redis.port}")
    private int redisPort;
    @Value("${redis.password}")
    private String redisPassword;
    @Value("${redis.connect-timeout}")
    private Duration connectTimeout;
    @Value("${redis.read-timeout}")
    private Duration readTimeout;

    @Bean
    JedisConnectionFactory jedisConnectionFactory() {
        // Reuse TLS connections instead of opening a new one for every cache access and health check
        JedisClientConfiguration jedisClientConfiguration = JedisClientConfiguration.builder()
                .connectTimeout(connectTimeout)
                .readTimeout(readTimeout)
                .useSsl()
                .and()
                .usePooling()
                .poolConfig(new JedisPoolConfig())
                .build();
        return new JedisConnectionFactory(getRedisStandaloneConfiguration(), jedisClientConfiguration);
    }

    @Bean
    public RedisStandaloneConfiguration getRedisStandaloneConfiguration() {
        RedisStandaloneConfiguration redisStandaloneConfiguration = new RedisStandaloneConfiguration();
        redisStandaloneConfiguration.setHostName(redisHostName);
        redisStandaloneConfiguration.setPort(redisPort);
        redisStandaloneConfiguration.setPassword(RedisPassword.of(redisPassword));

        return redisStandaloneConfiguration;
    }

    @Bean(name = "redisCacheManager")
    public RedisCacheManager redisCacheManager(JedisConnectionFactory jedisConnectionFactory) {
        RedisCacheConfiguration cfg = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofHours(1))
                .disableCachingNullValues()
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()));

        return RedisCacheManager.builder(RedisCacheWriter.nonLockingRedisCacheWriter(jedisConnectionFactory))
                .cacheDefaults(cfg)
                .withCacheConfiguration("pdndInfocamere", cfg)
                .disableCreateOnMissingCache()
                .build();
    }



}
