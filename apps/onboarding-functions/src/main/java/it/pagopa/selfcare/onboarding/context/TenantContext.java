package it.pagopa.selfcare.onboarding.context;

import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.HttpRequestMessage;
import io.quarkus.arc.Arc;
import it.pagopa.selfcare.tenant.TenantRegistry;
import it.pagopa.selfcare.tenant.UnknownTenantException;

import java.util.Map;
import java.util.Objects;

/**
 * Tenant of the current function invocation, kept on a {@link ThreadLocal} because the request
 * scope (and so the SDK {@code TenantContext}) is not active in durable and queue triggers. The
 * supported tenants are the ones of the {@link TenantRegistry} ({@code TENANT_REGISTRY_JSON}
 * filtered by {@code tenant.supported-tenants}).
 */
public final class TenantContext {

    public static final String TENANT_HEADER = "X-Tenant-Id";
    public static final String TENANT_CLAIM = "tenant_id";

    private static final ThreadLocal<String> CURRENT_TENANT = new ThreadLocal<>();

    private TenantContext() {}

    /**
     * The Azure Functions host lowercases HTTP header names, so the lookup must be case-insensitive.
     */
    public static String tenantHeader(HttpRequestMessage<?> request) {
        Map<String, String> headers = request.getHeaders();
        if (headers == null) {
            return null;
        }
        return headers.entrySet().stream()
                .filter(entry -> TENANT_HEADER.equalsIgnoreCase(entry.getKey()))
                .map(Map.Entry::getValue)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }

    public static Scope open(HttpRequestMessage<?> request, ExecutionContext context) {
        String tenant = resolve(tenantHeader(request));
        context.getLogger().info(() -> "Function request tenant=" + tenant);

        return open(tenant);
    }

    public static Scope open(String tenant) {
        String resolvedTenant = resolve(tenant);
        String previousTenant = CURRENT_TENANT.get();
        CURRENT_TENANT.set(resolvedTenant);
        return new Scope(previousTenant);
    }

    public static String currentTenant() {
        return CURRENT_TENANT.get();
    }

    public static String requiredTenant() {
        String tenant = currentTenant();
        if (tenant == null || tenant.isBlank()) {
            throw new IllegalArgumentException("Tenant context is required");
        }
        return tenant;
    }

    /**
     * Normalizes {@code tenant} ({@code trim} + upper case) and checks it against the registry.
     *
     * @throws IllegalArgumentException if the tenant is blank or not supported by the registry
     */
    public static String resolve(String tenant) {
        if (tenant == null || tenant.isBlank()) {
            throw new IllegalArgumentException("Tenant context is required");
        }
        TenantRegistry registry = registry();
        String canonicalTenant = registry.normalizeTenantId(tenant);
        try {
            registry.resolve(canonicalTenant);
        } catch (UnknownTenantException exception) {
            throw new IllegalArgumentException("Unsupported tenant");
        }
        return canonicalTenant;
    }

    private static TenantRegistry registry() {
        return Arc.container().instance(TenantRegistry.class).get();
    }

    public static final class Scope implements AutoCloseable {
        private final String previousTenant;

        private Scope(String previousTenant) {
            this.previousTenant = previousTenant;
        }

        @Override
        public void close() {
            if (previousTenant == null) {
                CURRENT_TENANT.remove();
            } else {
                CURRENT_TENANT.set(previousTenant);
            }
        }
    }
}
