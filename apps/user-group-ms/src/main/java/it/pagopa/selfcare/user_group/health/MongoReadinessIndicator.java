package it.pagopa.selfcare.user_group.health;

import it.pagopa.selfcare.commons.health.spring.AbstractMongoReadinessIndicator;
import it.pagopa.selfcare.commons.tenant.TenantContext;
import it.pagopa.selfcare.commons.tenant.TenantRegistry;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

@Component("mongoReadiness")
public class MongoReadinessIndicator extends AbstractMongoReadinessIndicator {

    private final MongoTemplate mongoTemplate;
    private final String databaseName;
    private final String host;
    private final TenantRegistry tenantRegistry;
    private final TenantContext tenantContext;

    @Autowired
    public MongoReadinessIndicator(
            MongoTemplate mongoTemplate,
            @Value("${spring.data.mongodb.database}") String databaseName,
            @Value("${spring.data.mongodb.uri}") String connectionString,
            TenantRegistry tenantRegistry,
            TenantContext tenantContext) {
        this.mongoTemplate = mongoTemplate;
        this.databaseName = databaseName;
        this.host = hostFromConnectionString(connectionString);
        this.tenantRegistry = tenantRegistry;
        this.tenantContext = tenantContext;
    }

    MongoReadinessIndicator(
            MongoTemplate mongoTemplate, String databaseName, String connectionString) {
        this.mongoTemplate = mongoTemplate;
        this.databaseName = databaseName;
        this.host = hostFromConnectionString(connectionString);
        this.tenantRegistry = null;
        this.tenantContext = null;
    }

    @Override
    protected String checkName() {
        return "mongodb-user-group";
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
        if (tenantRegistry == null || tenantContext == null || !tenantRegistry.isConfigured()) {
            mongoTemplate.executeCommand(new Document("ping", 1));
            return;
        }
        tenantRegistry.supportedTenantIds().forEach(tenantId ->
                tenantContext.withTenant(tenantId, () -> {
                    mongoTemplate.executeCommand(new Document("ping", 1));
                    return null;
                }));
    }
}
