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
import org.springframework.mock.env.MockEnvironment;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.doAnswer;

@SpringBootTest(
        classes = TenantReadinessEndpointTest.TestApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
            "management.endpoints.web.exposure.include=health",
            "management.endpoint.health.show-details=always",
            "management.endpoint.health.show-components=always",
            "management.endpoint.health.group.readiness.include=readinessState,mongoReadiness,fileBlobStorageReadiness,productBlobStorageReadiness",
            "management.health.mongo.enabled=false"
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
    void readinessEndpointUsesRegistryOnlyConfigurationAndKeepsBlobComponents() throws Exception {
        ResponseEntity<String> response =
                restTemplate.getForEntity("/actuator/health/readiness", String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        JsonNode body = objectMapper.readTree(response.getBody());
        assertEquals("UP", body.path("status").asText());
        assertEquals("UP", body.path("components").path("mongoReadiness").path("status").asText());
        JsonNode mongoDetails = body.path("components").path("mongoReadiness").path("details");
        assertEquals("mongodb", mongoDetails.path("component").asText());
        assertFalse(mongoDetails.has("host"));
        assertFalse(mongoDetails.has("database"));
        assertTrue(mongoDetails.has("latencyMs"));
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
        TenantRegistry tenantRegistry(ObjectMapper objectMapper) {
            return new TenantRegistry(
                    objectMapper,
                    new MockEnvironment()
                            .withProperty("MONGO_AR", "mongodb://mongo-ar:27017")
                            .withProperty("MONGO_PNPG", "mongodb://mongo-pnpg:27017"),
                    """
                    {
                      "AR":{"mongo":{"account":"ar","database":"institution-ar","connectionStringEnvVar":"MONGO_AR"}},
                      "PNPG":{"mongo":{"account":"pnpg","database":"institution-pnpg","connectionStringEnvVar":"MONGO_PNPG"}}
                    }
                    """,
                    "AR,PNPG",
                    "");
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
