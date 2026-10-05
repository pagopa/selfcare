package it.pagopa.selfcare.document.storage;

import it.pagopa.selfcare.tenant.TenantRegistry;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Optional;

/**
 * Builds a real {@link TenantRegistry} from a JSON document, running the same startup validation as the application.
 */
final class TenantRegistryFixtures {

    private TenantRegistryFixtures() {
    }

    static TenantRegistry registry(String json, String supportedTenants, String mandatoryStorageKeys) throws Exception {
        TenantRegistry registry = unvalidatedRegistry(json, supportedTenants, mandatoryStorageKeys);
        initialize(registry);
        return registry;
    }

    static TenantRegistry unvalidatedRegistry(String json, String supportedTenants, String mandatoryStorageKeys) throws Exception {
        TenantRegistry registry = new TenantRegistry();
        set(registry, "tenantRegistryJson", json);
        set(registry, "supportedTenants", supportedTenants);
        set(registry, "mongoMandatory", false);
        set(registry, "mandatoryStorageKeys", Optional.ofNullable(mandatoryStorageKeys));
        return registry;
    }

    static void initialize(TenantRegistry registry) throws Exception {
        Method initialize = TenantRegistry.class.getDeclaredMethod("initialize");
        initialize.setAccessible(true);
        try {
            initialize.invoke(registry);
        } catch (InvocationTargetException e) {
            throw (Exception) e.getCause();
        }
    }

    static String binding(String account, String container, String pathPrefix, String authentication) {
        return """
                {"account":"%s","container":"%s","pathPrefix":"%s","authentication":%s}
                """.formatted(account, container, pathPrefix, authentication).trim();
    }

    static String managedIdentity(String clientIdEnvVar) {
        if (clientIdEnvVar == null) {
            return "{\"type\":\"MANAGED_IDENTITY\"}";
        }
        return "{\"type\":\"MANAGED_IDENTITY\",\"managedIdentityClientIdEnvVar\":\"" + clientIdEnvVar + "\"}";
    }

    static String connectionString(String connectionStringEnvVar) {
        return "{\"type\":\"CONNECTION_STRING\",\"connectionStringEnvVar\":\"" + connectionStringEnvVar + "\"}";
    }

    static String tenant(String contractsBinding, String userAttachmentsBinding) {
        return "{\"storages\":{\"contracts\":" + contractsBinding
                + ",\"user-attachments\":" + userAttachmentsBinding + "}}";
    }

    private static void set(Object target, String field, Object value) throws Exception {
        Field f = TenantRegistry.class.getDeclaredField(field);
        f.setAccessible(true);
        f.set(target, value);
    }
}
