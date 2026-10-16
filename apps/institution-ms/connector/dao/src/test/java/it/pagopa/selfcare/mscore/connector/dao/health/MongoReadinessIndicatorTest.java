package it.pagopa.selfcare.mscore.connector.dao.health;

import it.pagopa.selfcare.commons.tenant.TenantContext;
import it.pagopa.selfcare.commons.tenant.TenantRegistry;
import org.bson.Document;
import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;
import org.springframework.data.mongodb.core.MongoTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MongoReadinessIndicatorTest {

    @Test
    void probesEverySupportedTenantAndRestoresAnEmptyContext() {
        MongoTemplate mongoTemplate = mock(MongoTemplate.class);
        TenantRegistry tenantRegistry = mock(TenantRegistry.class);
        TenantContext tenantContext = new TenantContext();
        when(tenantRegistry.supportedTenantIds()).thenReturn(Set.of("AR", "PNPG"));
        List<String> probedTenants = new ArrayList<>();
        doAnswer(invocation -> {
            probedTenants.add(tenantContext.requiredTenantId());
            return new Document("ok", 1);
        }).when(mongoTemplate).executeCommand(new Document("ping", 1));
        MongoReadinessIndicator indicator =
                indicator(mongoTemplate, tenantRegistry, tenantContext);

        Health health = indicator.health();

        assertEquals(Status.UP, health.getStatus());
        assertEquals(Set.of("AR", "PNPG"), Set.copyOf(probedTenants));
        assertTrue(tenantContext.tenantId().isEmpty());
        assertEquals("mongodb-institution", indicator.checkName());
        assertEquals("mongodb", health.getDetails().get("component"));
        assertEquals("selcMsCore", health.getDetails().get("database"));
        assertEquals(
                "mongo-primary:27017,mongo-secondary:27017",
                health.getDetails().get("host"));
        assertTrue(health.getDetails().containsKey("latencyMs"));
        assertFalse(health.getDetails().containsKey("error"));
    }

    @Test
    void onlyProbesSupportedTenants() {
        MongoTemplate mongoTemplate = mock(MongoTemplate.class);
        TenantRegistry tenantRegistry = mock(TenantRegistry.class);
        TenantContext tenantContext = new TenantContext();
        when(tenantRegistry.supportedTenantIds()).thenReturn(Set.of("AR"));
        List<String> probedTenants = new ArrayList<>();
        doAnswer(invocation -> {
            probedTenants.add(tenantContext.requiredTenantId());
            return new Document("ok", 1);
        }).when(mongoTemplate).executeCommand(new Document("ping", 1));
        MongoReadinessIndicator indicator =
                indicator(mongoTemplate, tenantRegistry, tenantContext);

        Health health = indicator.health();

        assertEquals(Status.UP, health.getStatus());
        assertEquals(List.of("AR"), probedTenants);
    }

    @Test
    void returnsDownWithSanitizedErrorAndRestoresPreviousContextOnFailure() {
        MongoTemplate mongoTemplate = mock(MongoTemplate.class);
        TenantRegistry tenantRegistry = mock(TenantRegistry.class);
        TenantContext tenantContext = new TenantContext();
        tenantContext.setTenantId("AR");
        when(tenantRegistry.supportedTenantIds()).thenReturn(Set.of("PNPG", "AR"));
        doAnswer(invocation -> {
            if ("PNPG".equals(tenantContext.requiredTenantId())) {
                throw new IllegalStateException("mongodb://user:password@mongo-host");
            }
            return new Document("ok", 1);
        }).when(mongoTemplate).executeCommand(new Document("ping", 1));
        MongoReadinessIndicator indicator =
                indicator(mongoTemplate, tenantRegistry, tenantContext);

        Health health = indicator.health();

        assertEquals(Status.DOWN, health.getStatus());
        assertEquals("AR", tenantContext.requiredTenantId());
        String error = (String) health.getDetails().get("error");
        assertTrue(error.contains("PNPG"));
        assertFalse(error.contains("password"));
        assertFalse(error.contains("mongo-host"));
    }

    @Test
    void restoresAnEmptyContextAfterFailure() {
        MongoTemplate mongoTemplate = mock(MongoTemplate.class);
        TenantRegistry tenantRegistry = mock(TenantRegistry.class);
        TenantContext tenantContext = new TenantContext();
        when(tenantRegistry.supportedTenantIds()).thenReturn(Set.of("AR"));
        when(mongoTemplate.executeCommand(new Document("ping", 1)))
                .thenThrow(new IllegalStateException("ping failed"));
        MongoReadinessIndicator indicator =
                indicator(mongoTemplate, tenantRegistry, tenantContext);

        Health health = indicator.health();

        assertEquals(Status.DOWN, health.getStatus());
        assertTrue(tenantContext.tenantId().isEmpty());
    }

    private static MongoReadinessIndicator indicator(
            MongoTemplate mongoTemplate,
            TenantRegistry tenantRegistry,
            TenantContext tenantContext) {
        return new MongoReadinessIndicator(
                mongoTemplate,
                "selcMsCore",
                "mongodb://mongo-primary:27017,mongo-secondary:27017/selcMsCore",
                tenantRegistry,
                tenantContext) {
            @Override
            protected Executor executor() {
                return Runnable::run;
            }
        };
    }
}
