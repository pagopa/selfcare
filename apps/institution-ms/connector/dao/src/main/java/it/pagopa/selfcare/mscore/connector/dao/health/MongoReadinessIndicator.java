package it.pagopa.selfcare.mscore.connector.dao.health;

import it.pagopa.selfcare.commons.health.spring.AbstractReadinessIndicator;
import it.pagopa.selfcare.commons.health.spring.HealthIndicatorConstants;
import it.pagopa.selfcare.commons.tenant.TenantContext;
import it.pagopa.selfcare.commons.tenant.TenantRegistry;
import org.bson.Document;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Objects;

@Component("mongoReadiness")
public class MongoReadinessIndicator extends AbstractReadinessIndicator {

    private final MongoTemplate mongoTemplate;
    private final TenantRegistry tenantRegistry;
    private final TenantContext tenantContext;

    public MongoReadinessIndicator(
            MongoTemplate mongoTemplate,
            TenantRegistry tenantRegistry,
            TenantContext tenantContext) {
        this.mongoTemplate = mongoTemplate;
        this.tenantRegistry = Objects.requireNonNull(tenantRegistry);
        this.tenantContext = Objects.requireNonNull(tenantContext);
    }

    @Override
    protected String checkName() {
        return "mongodb-institution";
    }

    @Override
    protected Map<String, Object> data() {
        return Map.of(HealthIndicatorConstants.DETAIL_COMPONENT, "mongodb");
    }

    @Override
    protected void probe() {
        tenantRegistry.supportedTenantIds().forEach(tenantId -> {
            try {
                tenantContext.withTenant(tenantId, () -> {
                    mongoTemplate.executeCommand(new Document("ping", 1));
                    return null;
                });
            } catch (RuntimeException exception) {
                throw new IllegalStateException(
                        "MongoDB ping failed for tenant " + tenantId, exception);
            }
        });
    }
}
