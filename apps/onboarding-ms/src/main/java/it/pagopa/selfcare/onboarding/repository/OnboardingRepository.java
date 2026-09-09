package it.pagopa.selfcare.onboarding.repository;

import io.quarkus.mongodb.panache.reactive.ReactivePanacheQuery;
import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.onboarding.entity.Onboarding;
import it.pagopa.selfcare.tenant.TenantContext;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.List;
import java.util.Optional;
import org.bson.Document;

@ApplicationScoped
public class OnboardingRepository {

    private static final String TENANT_ID = "tenantId";

    @Inject
    TenantContext tenantContext;

    /**
     * Panache's ID helpers do not accept an additional predicate, so the
     * discriminator is checked before the entity is returned to the service.
     */
    public Uni<Optional<Onboarding>> findByIdOptional(String onboardingId) {
        String tenantId = tenantId();
        return Onboarding.findByIdOptional(onboardingId)
                .map(optional -> optional
                        .map(Onboarding.class::cast)
                        .filter(onboarding -> tenantId.equalsIgnoreCase(onboarding.getTenantId())));
    }

    public Uni<Onboarding> findById(String onboardingId) {
        String tenantId = tenantId();
        return Onboarding.findById(onboardingId)
                .map(entity -> {
                    Onboarding onboarding = (Onboarding) entity;
                    return tenantId.equalsIgnoreCase(onboarding.getTenantId()) ? onboarding : null;
                });
    }

    public ReactivePanacheQuery<Onboarding> find(Document query) {
        return Onboarding.find(scope(query));
    }

    public ReactivePanacheQuery<Onboarding> find(Document query, Document sort) {
        return Onboarding.find(scope(query), sort);
    }

    public Uni<Long> update(Document update, String onboardingId) {
        return Onboarding.update(update)
                .where(TENANT_ID + " = ?1 and _id = ?2", tenantId(), onboardingId);
    }

    public void validateTenant(Onboarding onboarding) {
        String currentTenant = tenantId();
        if (onboarding.getTenantId() != null
                && !currentTenant.equalsIgnoreCase(onboarding.getTenantId())) {
            throw new IllegalStateException("Onboarding tenant does not match the current tenant");
        }
        onboarding.setTenantId(currentTenant);
    }

    public Uni<Onboarding> persist(Onboarding onboarding) {
        validateTenant(onboarding);
        return Onboarding.persist(onboarding).replaceWith(onboarding);
    }

    public Uni<Onboarding> persistOrUpdate(Onboarding onboarding) {
        validateTenant(onboarding);
        return Onboarding.persistOrUpdate(List.of(onboarding)).replaceWith(onboarding);
    }

    private Document scope(Document query) {
        Document scopedQuery = new Document(TENANT_ID, tenantId());
        if (query != null && !query.isEmpty()) {
            return new Document("$and", List.of(scopedQuery, query));
        }
        return scopedQuery;
    }

    private String tenantId() {
        return tenantContext.requiredTenantId();
    }
}
