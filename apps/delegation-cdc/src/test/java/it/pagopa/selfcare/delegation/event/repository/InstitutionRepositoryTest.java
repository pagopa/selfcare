package it.pagopa.selfcare.delegation.event.repository;

import io.quarkus.panache.mock.PanacheMock;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.mongodb.MongoTestResource;
import io.quarkus.test.vertx.RunOnVertxContext;
import io.quarkus.test.vertx.UniAsserter;
import io.smallrye.mutiny.Uni;
import io.quarkus.mongodb.panache.reactive.ReactivePanacheQuery;
import it.pagopa.selfcare.delegation.event.entity.Institution;
import jakarta.inject.Inject;
import org.bson.Document;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@QuarkusTest
@QuarkusTestResource(MongoTestResource.class)
class InstitutionRepositoryTest {

    @Inject
    InstitutionRepository institutionRepository;

    @Test
    @RunOnVertxContext
    void testFindInstitutionByIdPresent(UniAsserter asserter) {
        String institutionId = "institutionId";
        Institution institution = new Institution();
        institution.setId(institutionId);

        PanacheMock.mock(Institution.class);
        when(Institution.findByIdOptional(anyString())).thenReturn(Uni.createFrom().item(Optional.of(institution)));

        Uni<Institution> result = institutionRepository.findInstitutionById(institutionId, null);

        asserter.execute(() -> {
            assertNotNull(result);
            result.subscribe().with(item -> {
                assertNotNull(item);
                assertEquals(institutionId, item.getId());
            });
            PanacheMock.verify(Institution.class, atLeastOnce()).findByIdOptional(anyString());
        });
    }

    @Test
    @RunOnVertxContext
    void testFindInstitutionByIdNotPresent(UniAsserter asserter) {
        String institutionId = "institutionId";

        PanacheMock.mock(Institution.class);
        when(Institution.findByIdOptional(anyString())).thenReturn(Uni.createFrom().item(Optional.empty()));

        Uni<Institution> result = institutionRepository.findInstitutionById(institutionId, null);

        asserter.execute(() -> {
            assertNotNull(result);
            result.subscribe().with(item -> {
                assertNull(item);
            });
            PanacheMock.verify(Institution.class, atLeastOnce()).findByIdOptional(anyString());
        });
    }

    @Test
    @RunOnVertxContext
    void testFindInstitutionByIdScopesQueryToSourceTenantDuringMigration(UniAsserter asserter) {
        String institutionId = "institutionId";
        String tenantId = "PNPG";
        Institution institution = new Institution();
        institution.setId(institutionId);
        institution.setTenantId(tenantId);

        PanacheMock.mock(Institution.class);
        ReactivePanacheQuery<Institution> query = mock(ReactivePanacheQuery.class);
        when(Institution.find(any(Document.class))).thenAnswer(invocation -> query);
        when(query.firstResultOptional()).thenReturn(Uni.createFrom().item(Optional.of(institution)));

        Uni<Institution> result = institutionRepository.findInstitutionById(institutionId, tenantId);

        asserter.execute(() -> {
            assertNotNull(result);
            result.subscribe().with(item -> assertEquals(tenantId, item.getTenantId()));
            ArgumentCaptor<Document> documentCaptor = ArgumentCaptor.forClass(Document.class);
            PanacheMock.verify(Institution.class, atLeastOnce()).find(documentCaptor.capture());
            Document queryDocument = documentCaptor.getValue();
            assertTrue(queryDocument.containsKey("$or"));
            assertEquals(tenantId, ((Document) queryDocument.getList("$or", Document.class).get(0)).get("tenantId"));
            assertNull(((Document) queryDocument.getList("$or", Document.class).get(1)).get("tenantId"));
        });
    }
}
