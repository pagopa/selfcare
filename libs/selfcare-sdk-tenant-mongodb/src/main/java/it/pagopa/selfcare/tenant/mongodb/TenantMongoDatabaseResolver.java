package it.pagopa.selfcare.tenant.mongodb;

import io.quarkus.mongodb.panache.common.MongoDatabaseResolver;
import it.pagopa.selfcare.tenant.TenantContext;
import it.pagopa.selfcare.tenant.TenantRegistry;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Selects the Mongo database name from the tenant registry for the current
 * {@link TenantContext}. Used by Panache when {@code @MongoEntity.database} is empty.
 */
@ApplicationScoped
public class TenantMongoDatabaseResolver implements MongoDatabaseResolver {

    private final TenantRegistry tenantRegistry;
    private final TenantContext tenantContext;

    @Inject
    public TenantMongoDatabaseResolver(TenantRegistry tenantRegistry, TenantContext tenantContext) {
        this.tenantRegistry = tenantRegistry;
        this.tenantContext = tenantContext;
    }

    @Override
    public String resolve() {
        return tenantRegistry.resolve(tenantContext.requiredTenantId()).mongo().database();
    }
}
