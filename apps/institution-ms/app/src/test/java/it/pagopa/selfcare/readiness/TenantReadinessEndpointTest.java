package it.pagopa.selfcare.readiness;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.selfcare.commons.tenant.TenantContext;
import it.pagopa.selfcare.commons.tenant.TenantRegistry;
import it.pagopa.selfcare.mscore.connector.dao.health.MongoReadinessIndicator;
import org.bson.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doAnswer;

@SpringBootTest(
        classes = TenantReadinessEndpointTest.TestApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
            "management.endpoints.web.exposure.include=health",
            "management.endpoint.health.show-details=always",
            "management.endpoint.health.show-components=always",
            "management.endpoint.health.group.readiness.include=readinessState,mongoReadiness,fileBlobStorageReadiness,productBlobStorageReadiness",
            "management.health.mongo.enabled=false",
            "spring.data.mongodb.database=selcMsCore",
            "spring.data.mongodb.uri=mongodb://mongo-primary:27017/selcMsCore"
        })
class TenantReadinessEndpointTest {

    @Autowired private TestRestTemplate restTemplate;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private AtomicBoolean failPnpgPing;
    @Autowired private TenantContext tenantContext;

    @BeforeEach
    void resetProbe() {
        failPnpgPing.set(false);
        tenantContext.clear();
    }

    @Test
    void readinessEndpointNeedsNoTenantHeaderAndKeepsBlobComponents() throws Exception {
        ResponseEntity<String> response =
                restTemplate.getForEntity("/actuator/health/readiness", String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        JsonNode body = objectMapper.readTree(response.getBody());
        assertEquals("UP", body.path("status").asText());
        assertEquals("UP", body.path("components").path("mongoReadiness").path("status").asText());
        assertEquals("mongodb", body.path("components").path("mongoReadiness")
                .path("details").path("component").asText());
        assertEquals("UP", body.path("components").path("fileBlobStorageReadiness").path("status").asText());
        assertEquals("UP", body.path("components").path("productBlobStorageReadiness").path("status").asText());
        assertTrue(tenantContext.tenantId().isEmpty());
    }

    @Test
    void readinessEndpointIsDownForTenantFailureWithoutExposingConnectionDetails() throws Exception {
        failPnpgPing.set(true);

        ResponseEntity<String> response =
                restTemplate.getForEntity("/actuator/health/readiness", String.class);

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
        JsonNode body = objectMapper.readTree(response.getBody());
        assertEquals("DOWN", body.path("components").path("mongoReadiness").path("status").asText());
        assertEquals("UP", body.path("components").path("fileBlobStorageReadiness").path("status").asText());
        assertEquals("UP", body.path("components").path("productBlobStorageReadiness").path("status").asText());
        String diagnostics = body.path("components").path("mongoReadiness").toString();
        assertTrue(diagnostics.contains("PNPG"));
        assertFalse(diagnostics.contains("password"));
        assertFalse(diagnostics.contains("mongo-secret-host"));
        assertTrue(tenantContext.tenantId().isEmpty());
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration(exclude = MongoAutoConfiguration.class)
    @Import(MongoReadinessIndicator.class)
    static class TestApplication {

        @Bean
        TenantContext tenantContext() {
            return new TenantContext();
        }

        @Bean
        TenantRegistry tenantRegistry() {
            TenantRegistry registry = mock(TenantRegistry.class);
            when(registry.supportedTenantIds()).thenReturn(Set.of("AR", "PNPG"));
            return registry;
        }

        @Bean
        AtomicBoolean failPnpgPing() {
            return new AtomicBoolean();
        }

        @Bean
        MongoTemplate mongoTemplate(TenantContext tenantContext, AtomicBoolean failPnpgPing) {
            MongoTemplate mongoTemplate = mock(MongoTemplate.class);
            doAnswer(invocation -> {
                if (failPnpgPing.get() && "PNPG".equals(tenantContext.requiredTenantId())) {
                    throw new IllegalStateException("connection refused at mongo-secret-host");
                }
                return new Document("ok", 1);
            }).when(mongoTemplate).executeCommand(new Document("ping", 1));
            return mongoTemplate;
        }

        @Bean("fileBlobStorageReadiness")
        HealthIndicator fileBlobStorageReadiness() {
            return () -> Health.up().build();
        }

        @Bean("productBlobStorageReadiness")
        HealthIndicator productBlobStorageReadiness() {
            return () -> Health.up().build();
        }
    }
}
