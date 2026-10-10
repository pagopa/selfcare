package it.pagopa.selfcare.party.registry_proxy.connector.rest.cache;

import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.jedis.JedisClientConfiguration;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RedisConfigTest {

    @Test
    void jedisConnectionFactoryUsesPoolingSslAndConfiguredTimeouts() {
        RedisConfig redisConfig = new RedisConfig();
        ReflectionTestUtils.setField(redisConfig, "redisHostName", "localhost");
        ReflectionTestUtils.setField(redisConfig, "redisPort", 6380);
        ReflectionTestUtils.setField(redisConfig, "redisPassword", "password");
        ReflectionTestUtils.setField(redisConfig, "connectTimeout", Duration.ofSeconds(5));
        ReflectionTestUtils.setField(redisConfig, "readTimeout", Duration.ofSeconds(4));

        JedisConnectionFactory factory = redisConfig.jedisConnectionFactory();
        JedisClientConfiguration clientConfiguration = factory.getClientConfiguration();

        assertTrue(clientConfiguration.isUsePooling());
        assertTrue(clientConfiguration.getPoolConfig().isPresent());
        assertTrue(clientConfiguration.isUseSsl());
        assertEquals(Duration.ofSeconds(5), clientConfiguration.getConnectTimeout());
        assertEquals(Duration.ofSeconds(4), clientConfiguration.getReadTimeout());
        assertEquals("localhost", factory.getHostName());
        assertEquals(6380, factory.getPort());
    }
}
