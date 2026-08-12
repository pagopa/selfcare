package it.pagopa.selfcare.delegation.event.repository;

import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.delegation.event.entity.Institution;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;

import java.util.List;


@Slf4j
@RequiredArgsConstructor
@ApplicationScoped
public class InstitutionRepository {
    private static final String TENANT_ID_FIELD = "tenantId";

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
        // Migration phase: tenantId == null keeps pre-backfill institutions visible. Drop the null
        // branch once every institution document has been backfilled.
        query.append("$or", List.of(new Document(TENANT_ID_FIELD, tenantId), new Document(TENANT_ID_FIELD, null)));
    }

}
