package it.pagopa.selfcare.document.repository;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.mongodb.MongoTestResource;
import it.pagopa.selfcare.document.model.entity.Document;
import it.pagopa.selfcare.onboarding.common.DocumentType;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Exercises the Step_1 SELC-8 tenant-scoped queries against a real MongoDB, not a mock.
 *
 * <p>The service-level tests mock {@code DocumentRepository}, so they cannot catch a query that is
 * syntactically valid but matches nothing against real data — which is exactly how enforcing the
 * tenant filter on reads before any document carried a {@code tenantId} would have turned every
 * lookup into a 404. These tests pin down the migration-phase contract:
 * <ul>
 *   <li>documents tagged for the requested tenant are returned;</li>
 *   <li>documents tagged for a different tenant are NOT returned;</li>
 *   <li>legacy untagged documents remain visible until the backfill runs.</li>
 * </ul>
 */
@QuarkusTest
@QuarkusTestResource(value = MongoTestResource.class, restrictToAnnotatedClass = true)
class DocumentRepositoryTenantTest {

    @Inject
    DocumentRepository documentRepository;

    @BeforeEach
    void cleanUp() {
        documentRepository.deleteAll().await().indefinitely();
    }

    private Document persist(String onboardingId, String tenantId) {
        Document document = new Document();
        document.setId(UUID.randomUUID().toString());
        document.setOnboardingId(onboardingId);
        document.setRootOnboardingId(onboardingId);
        document.setType(DocumentType.INSTITUTION);
        document.setTenantId(tenantId);
        document.setCreatedAt(LocalDateTime.now());
        document.setUpdatedAt(LocalDateTime.now());
        documentRepository.persist(document).await().indefinitely();
        return document;
    }

    // ---- findByOnboardingIdForTenant ----

    @Test
    void findByOnboardingIdForTenant_shouldReturnDocumentTaggedForSameTenant() {
        persist("onb-1", "AR");

        Document result = documentRepository.findByOnboardingIdForTenant("onb-1", "AR")
                .await().indefinitely();

        assertNotNull(result);
        assertEquals("AR", result.getTenantId());
    }

    @Test
    void findByOnboardingIdForTenant_shouldNotReturnDocumentOfAnotherTenant() {
        persist("onb-1", "AR");

        Document result = documentRepository.findByOnboardingIdForTenant("onb-1", "PNPG")
                .await().indefinitely();

        assertNull(result);
    }

    @Test
    void findByOnboardingIdForTenant_shouldStillReturnLegacyUntaggedDocument() {
        // Regression guard: documents predating the tenantId discriminator must stay readable
        // during the migration phase, otherwise enabling the tenant filter 404s the whole service.
        persist("onb-legacy", null);

        Document result = documentRepository.findByOnboardingIdForTenant("onb-legacy", "AR")
                .await().indefinitely();

        assertNotNull(result);
        assertNull(result.getTenantId());
    }

    // ---- findByIdForTenant ----

    @Test
    void findByIdForTenant_shouldReturnDocumentTaggedForSameTenant() {
        Document persisted = persist("onb-2", "PNPG");

        Document result = documentRepository.findByIdForTenant(persisted.getId(), "PNPG")
                .await().indefinitely();

        assertNotNull(result);
        assertEquals(persisted.getId(), result.getId());
    }

    @Test
    void findByIdForTenant_shouldNotReturnDocumentOfAnotherTenant() {
        Document persisted = persist("onb-2", "PNPG");

        Document result = documentRepository.findByIdForTenant(persisted.getId(), "AR")
                .await().indefinitely();

        assertNull(result);
    }

    @Test
    void findByIdForTenant_shouldStillReturnLegacyUntaggedDocument() {
        Document persisted = persist("onb-legacy-2", null);

        Document result = documentRepository.findByIdForTenant(persisted.getId(), "AR")
                .await().indefinitely();

        assertNotNull(result);
        assertNull(result.getTenantId());
    }
}
