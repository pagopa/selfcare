package it.pagopa.selfcare.delegation.event.repository;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.delegation.event.constant.DelegationType;
import it.pagopa.selfcare.delegation.event.entity.DelegationsEntity;
import it.pagopa.selfcare.delegation.event.constant.RelationshipState;
import it.pagopa.selfcare.delegation.event.entity.filter.DelegationsFilter;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;

import java.util.Map;
import java.util.Objects;


@Slf4j
@RequiredArgsConstructor
@ApplicationScoped
public class DelegationRepository {

    private static final String DELEGATION_COLLECTION = "Delegations";
    private static final String TENANT_ID_FIELD = "tenantId";


    //This method retrieves the institutions ids of the institution that are already associated to the PT
    public Multi<String> getInstitutionsAlreadyPresent(String institutionId, String productId, String tenantId) {
        Map<String, Object> delegationFilters = DelegationsFilter.builder()
                .productId(productId)
                .to(institutionId)
                .type(DelegationType.PT)
                .status(RelationshipState.ACTIVE)
                .build().constructMap();

        return getDelegationsWithFilters(delegationFilters, tenantId)
                .onItem().transform(DelegationsEntity::getFrom)
                .filter(Objects::nonNull)
                .select().distinct();
    }

    //This method get all active delegations of type EA related to the aggregator, filtering by the one that already exists
    public Multi<DelegationsEntity> getDelegationsEA(String institutionId, String productId, String tenantId) {
        Map<String, Object> delegationFilters = DelegationsFilter.builder()
                .productId(productId)
                .to(institutionId)
                .type(DelegationType.EA)
                .status(RelationshipState.ACTIVE)
                .build().constructMap();
        return getDelegationsWithFilters(delegationFilters, tenantId);
    }

    public Multi<DelegationsEntity> getDelegationsWithFilters(Map<String, Object> queryParameter, String tenantId) {
        Document query = new Document(queryParameter);
        addTenantFilter(query, tenantId);
        return DelegationsEntity.find(query).stream();
    }

    private void addTenantFilter(Document query, String tenantId) {
        if (tenantId == null || tenantId.isBlank()) {
            // Migration phase: legacy CDC events may carry no tenant, so keep the pre-multitenant
            // unscoped lookup instead of creating an unsatisfiable query. This is not a security boundary.
            return;
        }
        // Migration phase: tenantId == null keeps pre-backfill delegations visible. Drop the null
        // branch once every source and derived delegation has been backfilled.
        query.append("$or", java.util.List.of(new Document(TENANT_ID_FIELD, tenantId), new Document(TENANT_ID_FIELD, null)));
    }



    //This method insert all delegations
    public Uni<Void> insertDelegations(Multi<DelegationsEntity> delegations) {
        return delegations
                .collect().asList()
                .flatMap(DelegationsEntity::persist)
                .replaceWithVoid();
    }

    
}
