package it.pagopa.selfcare.onboarding.context;

import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.HttpRequestMessage;
import org.eclipse.microprofile.config.ConfigProvider;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

public final class TenantContext {

    public static final String TENANT_HEADER = "X-Tenant-Id";
    public static final String TENANT_CLAIM = "tenant_id";
    public static final String DEFAULT_TENANT = "PNPG";

    private static final String SUPPORTED_TENANTS_PROPERTY =
            "onboarding-functions.tenant.supported-tenants";
    private static final String DEFAULT_TENANT_PROPERTY =
            "onboarding-functions.tenant.default-tenant";
    private static final String DEFAULT_SUPPORTED_TENANTS = "AR,PNPG";
    private static final ThreadLocal<String> CURRENT_TENANT = new ThreadLocal<>();

    private TenantContext() {}

    public static Scope open(HttpRequestMessage<?> request, ExecutionContext context) {
        String headerTenant = request.getHeaders() == null
                ? null
                : request.getHeaders().get(TENANT_HEADER);
        String tenant = resolve(headerTenant);
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

    public static String currentTenantOrDefault() {
        String tenant = currentTenant();
        return tenant == null ? defaultTenant() : tenant;
    }

    public static String resolve(String tenant) {
        if (tenant == null || tenant.isBlank()) {
            String configuredDefault = defaultTenant();
            if (!supportedTenants().contains(configuredDefault)) {
                throw new IllegalArgumentException("Default tenant is not supported");
            }
            return configuredDefault;
        }
        if (!supportedTenants().contains(tenant)) {
            throw new IllegalArgumentException("Unsupported tenant");
        }
        return tenant;
    }

    private static String defaultTenant() {
        return ConfigProvider.getConfig()
                .getOptionalValue(DEFAULT_TENANT_PROPERTY, String.class)
                .orElse(DEFAULT_TENANT);
    }

    private static Set<String> supportedTenants() {
        String configuredTenants = ConfigProvider.getConfig()
                .getOptionalValue(SUPPORTED_TENANTS_PROPERTY, String.class)
                .orElse(DEFAULT_SUPPORTED_TENANTS);
        return Arrays.stream(configuredTenants.split(","))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .collect(Collectors.toUnmodifiableSet());
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
