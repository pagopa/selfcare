package it.pagopa.selfcare.document.health;

import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.commons.health.AbstractMongoReadinessCheck;
import it.pagopa.selfcare.tenant.TenantRegistry;
import it.pagopa.selfcare.tenant.mongodb.TenantMongoClientProducer;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.bson.Document;
import org.eclipse.microprofile.health.Readiness;

@Readiness
@ApplicationScoped
public class DocumentMongoReadinessCheck extends AbstractMongoReadinessCheck {

    private final TenantMongoClientProducer tenantMongoClientProducer;
    private final String tenantId;
    private final String databaseName;
    private final String host;

    @Inject
    public DocumentMongoReadinessCheck(
            TenantRegistry tenantRegistry, TenantMongoClientProducer tenantMongoClientProducer) {
        this.tenantMongoClientProducer = tenantMongoClientProducer;
        this.tenantId = tenantRegistry.supportedTenantIds().iterator().next();
        this.databaseName = tenantRegistry.resolve(tenantId).mongo().database();
        this.host = tenantRegistry.connectionString(tenantId)
                .map(AbstractMongoReadinessCheck::hostFromConnectionString)
                .orElse(HOST_NOT_AVAILABLE);
    }

    @Override
    protected String checkName() {
        return "mongodb-document";
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
    protected Uni<?> probe() {
        return tenantMongoClientProducer.clientForTenant(tenantId)
                .getDatabase(databaseName)
                .runCommand(new Document("ping", 1));
    }
}
