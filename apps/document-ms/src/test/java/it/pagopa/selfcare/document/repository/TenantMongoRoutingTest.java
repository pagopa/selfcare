package it.pagopa.selfcare.document.repository;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ManagedContext;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import it.pagopa.selfcare.document.model.entity.Document;
import it.pagopa.selfcare.tenant.TenantContext;
import it.pagopa.selfcare.tenant.TenantRegistry;
import it.pagopa.selfcare.tenant.UnresolvedTenantException;
import it.pagopa.selfcare.tenant.mongodb.TenantMongoClientProducer;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Verifies that Panache entities resolve the Mongo client and database of the tenant bound to
 * the current request, and that misconfigured or missing tenants fail closed.
 */
@QuarkusTest
@TestProfile(TenantMongoRoutingTest.MultiTenantMongoProfile.class)
class TenantMongoRoutingTest {

    @Inject TenantContext tenantContext;
    @Inject TenantMongoClientProducer tenantMongoClientProducer;

    private <T> T inRequest(String tenantId, Supplier<T> action) {
        ManagedContext requestContext = Arc.container().requestContext();
        requestContext.activate();
        try {
            if (tenantId != null) {
                tenantContext.setTenantId(tenantId);
            }
            return action.get();
        } finally {
            requestContext.terminate();
        }
    }

    @Test
    void entityUsesDatabaseOfCurrentTenant() {
        assertThat(inRequest("AR", () -> Document.mongoDatabase().getName())).isEqualTo("selcDocument");
        assertThat(inRequest("PNPG", () -> Document.mongoDatabase().getName())).isEqualTo("selcDocumentPnpg");
        assertThat(inRequest("AR", () -> Document.mongoCollection().getNamespace().getFullName()))
                .isEqualTo("selcDocument.documents");
    }

    @Test
    void eachTenantHasItsOwnClient() {
        assertThat(tenantMongoClientProducer.clientForTenant("AR"))
                .isNotSameAs(tenantMongoClientProducer.clientForTenant("PNPG"));
    }

    @Test
    void failsClosedWhenNoTenantIsBound() {
        assertThatThrownBy(() -> inRequest(null, () -> Document.mongoDatabase().getName()))
                .isInstanceOf(UnresolvedTenantException.class);
    }

    @Test
    void rejectsUnsupportedTenantClient() {
        assertThatThrownBy(() -> tenantMongoClientProducer.clientForTenant("UNKNOWN"))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void registryRejectsSupportedTenantWithoutMongo() throws Exception {
        TenantRegistry registry = new TenantRegistry();
        set(registry, "tenantRegistryJson",
                "{\"AR\":{\"mongo\":{\"account\":\"cosmos-ar\",\"database\":\"selcDocument\","
                        + "\"connectionStringEnvVar\":\"MONGODB_CONNECTION_STRING_AR\"}},"
                        + "\"PNPG\":{\"jwt\":{\"publicKeyEnvVar\":\"JWT_PUBLIC_KEY_PNPG\"}}}");
        set(registry, "supportedTenants", "AR,PNPG");
        Method initialize = TenantRegistry.class.getDeclaredMethod("initialize");
        initialize.setAccessible(true);

        assertThatThrownBy(() -> {
            try {
                initialize.invoke(registry);
            } catch (java.lang.reflect.InvocationTargetException e) {
                throw e.getCause();
            }
        })
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Missing Mongo configuration for tenant PNPG");
    }

    private static void set(Object target, String field, Object value) throws Exception {
        Field f = TenantRegistry.class.getDeclaredField(field);
        f.setAccessible(true);
        f.set(target, value);
    }

    public static class MultiTenantMongoProfile implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of(
                    "tenant.registry.json",
                    "{\"AR\":{\"mongo\":{\"account\":\"cosmos-ar\",\"database\":\"selcDocument\","
                            + "\"connectionStringEnvVar\":\"MONGODB_CONNECTION_STRING_AR\"}},"
                            + "\"PNPG\":{\"mongo\":{\"account\":\"cosmos-pnpg\",\"database\":\"selcDocumentPnpg\","
                            + "\"connectionStringEnvVar\":\"MONGODB_CONNECTION_STRING_PNPG\"}}}",
                    "tenant.supported-tenants", "AR,PNPG",
                    "tenant.storage.mandatory-keys", "",
                    "MONGODB_CONNECTION_STRING_PNPG", "mongodb://localhost:27018");
        }
    }
}
