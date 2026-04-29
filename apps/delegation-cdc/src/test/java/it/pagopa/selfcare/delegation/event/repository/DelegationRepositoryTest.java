package it.pagopa.selfcare.delegation.event.repository;

import io.quarkus.mongodb.panache.reactive.ReactivePanacheQuery;
import io.quarkus.panache.mock.PanacheMock;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.mongodb.MongoTestResource;
import io.quarkus.test.vertx.RunOnVertxContext;
import io.quarkus.test.vertx.UniAsserter;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.delegation.event.constant.DelegationState;
import it.pagopa.selfcare.delegation.event.constant.DelegationType;
import it.pagopa.selfcare.delegation.event.entity.DelegationsEntity;
import it.pagopa.selfcare.delegation.event.constant.RelationshipState;
import it.pagopa.selfcare.delegation.event.entity.filter.DelegationsFilter;
import jakarta.inject.Inject;
import org.bson.Document;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@QuarkusTest
@QuarkusTestResource(MongoTestResource.class)
class DelegationRepositoryTest {

    @Inject
    DelegationRepository delegationRepository;

    @Test
    @RunOnVertxContext
    void testGetInstitutionsAlreadyPresent(UniAsserter asserter) {
        String institutionId = "institutionId";
        String productId = "productId";
        Map<String, Object> delegationFilters = DelegationsFilter.builder()
                .productId(productId)
                .to(institutionId)
                .type(DelegationType.PT)
                .status(RelationshipState.ACTIVE)
                .build().constructMap();

        DelegationsEntity delegationEntity = new DelegationsEntity();
        delegationEntity.setFrom("fromInstitution");

        PanacheMock.mock(DelegationsEntity.class);
        ReactivePanacheQuery<DelegationsEntity> query = mock(ReactivePanacheQuery.class);
        when(DelegationsEntity.find(any(Document.class))).thenAnswer(invocation -> query);
        when(query.stream()).thenReturn(Multi.createFrom().items(delegationEntity));

        Multi<String> result = delegationRepository.getInstitutionsAlreadyPresent(institutionId, productId);

        asserter.execute(() -> {
            assertNotNull(result);
            result.subscribe().with(item -> {
                assertEquals("fromInstitution", item);
            });
            PanacheMock.verify(DelegationsEntity.class, atLeastOnce()).find(new Document(delegationFilters));
        });
    }

    @Test
    @RunOnVertxContext
    void testGetDelegationsEA(UniAsserter asserter) {
        String institutionId = "institutionId";
        String productId = "productId";
        Map<String, Object> delegationFilters = DelegationsFilter.builder()
                .productId(productId)
                .to(institutionId)
                .type(DelegationType.EA)
                .status(RelationshipState.ACTIVE)
                .build().constructMap();

        DelegationsEntity delegationEntity = new DelegationsEntity();
        delegationEntity.setId("delegationId");

        PanacheMock.mock(DelegationsEntity.class);
        ReactivePanacheQuery<DelegationsEntity> query = mock(ReactivePanacheQuery.class);
        when(DelegationsEntity.find(any(Document.class))).thenAnswer(invocation -> query);
        when(query.stream()).thenReturn(Multi.createFrom().items(delegationEntity));

        Multi<DelegationsEntity> result = delegationRepository.getDelegationsEA(institutionId, productId);

        asserter.execute(() -> {
            assertNotNull(result);
            result.subscribe().with(item -> {
                assertEquals("delegationId", item.getId());
            });
            PanacheMock.verify(DelegationsEntity.class, atLeastOnce()).find(new Document(delegationFilters));
        });
    }

    @Test
    @RunOnVertxContext
    void testInsertDelegations(UniAsserter asserter) {
        DelegationsEntity delegation = new DelegationsEntity();
        delegation.setId("delegationId");
        delegation.setFrom("fromInstitution");
        delegation.setTo("toInstitution");
        delegation.setProductId("productId");
        delegation.setType(DelegationType.PT);
        delegation.setStatus(DelegationState.ACTIVE);

        Multi<DelegationsEntity> delegations = Multi.createFrom().item(delegation);

        PanacheMock.mock(DelegationsEntity.class);
        when(DelegationsEntity.persist(any(Iterable.class))).thenReturn(Uni.createFrom().voidItem());

        Uni<Void> result = delegationRepository.insertDelegations(delegations);

        asserter.execute(() -> {
            assertNotNull(result);
            result.subscribe().with(Assertions::assertNull);
        });
    }

}
