package it.pagopa.selfcare.delegation.event.repository;

import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.delegation.event.entity.Institution;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.util.List;


@Slf4j
@RequiredArgsConstructor
@ApplicationScoped
public class InstitutionRepository {
    private static final String TENANT_ID_FIELD = "tenantId";

    /**
     * Whether untagged documents are still treated as belonging to the current tenant.
     *
     * <p>Configuration rather than a code constant because the backfill runs at a different time in
     * each environment, so the strict build must be promotable before every environment has been
     * migrated (Step_1/EPIC.md sub-tasks 2 and 10). Defaults to the lenient behaviour; both the flag
     * and the {@code tenantId == null} branch must be deleted once every environment runs strict.
     */
    @ConfigProperty(name = "selfcare.tenant.strict-data-isolation", defaultValue = "false")
    boolean strictTenantIsolation;

    public Uni<Institution> findInstitutionById(String institutionId, String tenantId) {
        if (tenantId == null || tenantId.isBlank()) {
            // Migration phase: legacy CDC events may carry no tenant, so keep the pre-multitenant
            // unscoped lookup instead of creating an unsatisfiable query. This is not a security boundary.
            return Institution.findByIdOptional(institutionId)
                    .flatMap(optionalEntity -> {
                        if (optionalEntity.isPresent()) {
                            Institution institution = (Institution) optionalEntity.get();
                            return Uni.createFrom().item(institution);
                        } else {
                            return Uni.createFrom().nullItem();
                        }
                    });
        }
        Document query = new Document("_id", institutionId);
        addTenantFilter(query, tenantId);
        return Institution.find(query).firstResultOptional()
                .flatMap(optionalEntity -> {
                    if (optionalEntity.isPresent()) {
                        Institution institution = (Institution) optionalEntity.get();
                        return Uni.createFrom().item(institution);
                    } else {
                        return Uni.createFrom().nullItem();
                    }
                });
    }

    private void addTenantFilter(Document query, String tenantId) {
        if (tenantId == null || tenantId.isBlank()) {
            // Migration phase: legacy CDC events may carry no tenant, so keep the pre-multitenant
            // unscoped lookup instead of creating an unsatisfiable query. This is not a security boundary.
            return;
        }
        if (strictTenantIsolation) {
            query.append(TENANT_ID_FIELD, tenantId);
            return;
        }
        // Migration phase: tenantId == null keeps pre-backfill institutions visible. The null branch
        // goes away when selfcare.tenant.strict-data-isolation is turned on after the backfill.
        query.append("$or", List.of(new Document(TENANT_ID_FIELD, tenantId), new Document(TENANT_ID_FIELD, null)));
    }

}
