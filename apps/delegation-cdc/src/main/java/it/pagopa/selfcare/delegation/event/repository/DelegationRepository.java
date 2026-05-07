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


    //This method retrieves the institutions ids of the institution that are already associated to the PT
    public Multi<String> getInstitutionsAlreadyPresent(String institutionId, String productId) {
        Map<String, Object> delegationFilters = DelegationsFilter.builder()
                .productId(productId)
                .to(institutionId)
                .type(DelegationType.PT)
                .status(RelationshipState.ACTIVE)
                .build().constructMap();

        return getDelegationsWithFilters(delegationFilters)
                .onItem().transform(DelegationsEntity::getFrom)
                .filter(Objects::nonNull)
                .select().distinct();
    }

    //This method get all active delegations of type EA related to the aggregator, filtering by the one that already exists
    public Multi<DelegationsEntity> getDelegationsEA(String institutionId, String productId) {
        Map<String, Object> delegationFilters = DelegationsFilter.builder()
                .productId(productId)
                .to(institutionId)
                .type(DelegationType.EA)
                .status(RelationshipState.ACTIVE)
                .build().constructMap();
        return getDelegationsWithFilters(delegationFilters);
    }

    public Multi<DelegationsEntity> getDelegationsWithFilters(Map<String, Object> queryParameter) {
        Document query = new Document(queryParameter);
        return DelegationsEntity.find(query).stream();
    }



    //This method insert all delegations
    public Uni<Void> insertDelegations(Multi<DelegationsEntity> delegations) {
        return delegations
                .collect().asList()
                .flatMap(DelegationsEntity::persist)
                .replaceWithVoid();
    }

    
}

