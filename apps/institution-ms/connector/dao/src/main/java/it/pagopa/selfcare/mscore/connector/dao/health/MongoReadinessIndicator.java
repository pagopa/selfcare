package it.pagopa.selfcare.mscore.connector.dao.health;

import it.pagopa.selfcare.commons.health.spring.AbstractMongoReadinessIndicator;
import it.pagopa.selfcare.commons.tenant.TenantContext;
import it.pagopa.selfcare.commons.tenant.TenantRegistry;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component("mongoReadiness")
public class MongoReadinessIndicator extends AbstractMongoReadinessIndicator {

    private final MongoTemplate mongoTemplate;
    private final String databaseName;
    private final String host;
    private final TenantRegistry tenantRegistry;
    private final TenantContext tenantContext;

    public MongoReadinessIndicator(
            MongoTemplate mongoTemplate,
            @Value("${spring.data.mongodb.database}") String databaseName,
            @Value("${spring.data.mongodb.uri}") String connectionString,
            TenantRegistry tenantRegistry,
            TenantContext tenantContext) {
        this.mongoTemplate = mongoTemplate;
        this.databaseName = databaseName;
        this.host = hostFromConnectionString(connectionString);
        this.tenantRegistry = Objects.requireNonNull(tenantRegistry);
        this.tenantContext = Objects.requireNonNull(tenantContext);
    }

    @Override
    protected String checkName() {
        return "mongodb-institution";
    }

    @Override
    protected String databaseName() {
        return databaseName;
    }

    @Override
    protected String host() {
        return host;
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
