package it.pagopa.selfcare.document.health;

import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.commons.health.AbstractMongoReadinessCheck;
import it.pagopa.selfcare.commons.health.HealthCheckConstants;
import it.pagopa.selfcare.tenant.TenantRegistry;
import it.pagopa.selfcare.tenant.mongodb.TenantMongoClientProducer;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.bson.Document;
import org.eclipse.microprofile.health.Readiness;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

/**
 * Pings the Mongo database of every supported tenant. The check is UP only when all tenants
 * answer; data lists {@code tenant=database} and {@code tenant=host} pairs, never connection
 * strings or credentials. Each ping has its own timeout so a slow tenant is named in the error;
 * the overall timeout is slightly longer to let the per-tenant one fire first.
 */
@Readiness
@ApplicationScoped
public class DocumentMongoReadinessCheck extends AbstractMongoReadinessCheck {

    static final Duration PING_TIMEOUT = HealthCheckConstants.DEFAULT_TIMEOUT;
    private static final Duration OVERALL_TIMEOUT_MARGIN = Duration.ofMillis(500);

    private final TenantMongoClientProducer tenantMongoClientProducer;
    private final Duration pingTimeout;
    private final List<TenantTarget> targets;
    private final String databaseName;
    private final String host;

    @Inject
    public DocumentMongoReadinessCheck(
            TenantRegistry tenantRegistry, TenantMongoClientProducer tenantMongoClientProducer) {
        this(tenantRegistry, tenantMongoClientProducer, PING_TIMEOUT);
    }

    DocumentMongoReadinessCheck(
            TenantRegistry tenantRegistry,
            TenantMongoClientProducer tenantMongoClientProducer,
            Duration pingTimeout) {
        this.tenantMongoClientProducer = tenantMongoClientProducer;
        this.pingTimeout = pingTimeout;
        this.targets = tenantRegistry.supportedTenantIds().stream()
                .sorted()
                .map(tenantId -> new TenantTarget(
                        tenantId,
                        tenantRegistry.resolve(tenantId).mongo().database(),
                        tenantRegistry.connectionString(tenantId)
                                .map(AbstractMongoReadinessCheck::hostFromConnectionString)
                                .orElse(HOST_NOT_AVAILABLE)))
                .toList();
        this.databaseName = targets.isEmpty() ? HOST_NOT_AVAILABLE : targets.stream()
                .map(t -> t.tenantId() + "=" + t.database())
                .collect(Collectors.joining(","));
        this.host = targets.isEmpty() ? HOST_NOT_AVAILABLE : targets.stream()
                .map(t -> t.tenantId() + "=" + t.host())
                .collect(Collectors.joining(","));
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
    protected Duration timeout() {
        return pingTimeout.plus(OVERALL_TIMEOUT_MARGIN);
    }

    @Override
    protected Uni<?> probe() {
        if (targets.isEmpty()) {
            return Uni.createFrom().failure(new IllegalStateException("No tenant configured"));
        }
        List<Uni<Document>> pings = targets.stream().map(this::ping).toList();
        return Uni.join().all(pings).andFailFast();
    }

    private Uni<Document> ping(TenantTarget target) {
        return Uni.createFrom().deferred(() -> tenantMongoClientProducer
                        .clientForTenant(target.tenantId())
                        .getDatabase(target.database())
                        .runCommand(new Document("ping", 1)))
                .ifNoItem().after(pingTimeout).failWith(() -> new TimeoutException(
                        "no reply after " + pingTimeout))
                .onFailure().transform(failure -> new IllegalStateException(
                        "Tenant " + target.tenantId() + " ping failed: "
                                + failure.getClass().getSimpleName() + ": " + failure.getMessage(),
                        failure));
    }

    private record TenantTarget(String tenantId, String database, String host) {
    }
}
