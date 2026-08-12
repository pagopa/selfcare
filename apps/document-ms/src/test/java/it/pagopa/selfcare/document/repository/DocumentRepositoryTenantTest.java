package it.pagopa.selfcare.document.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import io.quarkus.test.InjectMock;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.mongodb.MongoTestResource;
import it.pagopa.selfcare.document.model.entity.Document;
import it.pagopa.selfcare.document.service.CurrentTenantProvider;
import it.pagopa.selfcare.onboarding.common.DocumentType;
import it.pagopa.selfcare.document.model.StorageOrigin;
import jakarta.inject.Inject;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Exercises the tenant-scoped queries against a real MongoDB, not a mock.
 *
 * <p>The service-level tests mock {@code DocumentRepository}, so they cannot catch a query that is
 * syntactically valid but matches nothing against real data - which is exactly how enforcing the
 * tenant filter on reads before any document carried a {@code tenantId} would have turned every
 * lookup into a 404. Since the filter is now appended inside the repository, a mistake in how the
 * predicate or its positional parameter is built would be invisible everywhere else.
 *
 * <p>Each query is pinned against three cases: same tenant (returned), other tenant (not returned),
 * and legacy untagged (still returned until the backfill runs).
 */
@QuarkusTest
@QuarkusTestResource(value = MongoTestResource.class, restrictToAnnotatedClass = true)
class DocumentRepositoryTenantTest {

  @Inject DocumentRepository documentRepository;

  @InjectMock CurrentTenantProvider currentTenantProvider;

  @BeforeEach
  void cleanUp() {
    documentRepository.deleteAll().await().indefinitely();
    actAs("AR");
  }

  private void actAs(String tenantId) {
    when(currentTenantProvider.currentTenantId()).thenReturn(Optional.ofNullable(tenantId));
  }

  private Document persist(String onboardingId, String tenantId) {
    return persist(onboardingId, tenantId, DocumentType.INSTITUTION, null);
  }

  private Document persist(
      String onboardingId, String tenantId, DocumentType type, String attachmentName) {
    Document document = new Document();
    document.setId(UUID.randomUUID().toString());
    document.setOnboardingId(onboardingId);
    document.setRootOnboardingId(onboardingId);
    document.setType(type);
    document.setTenantId(tenantId);
    document.setAttachmentName(attachmentName);
    document.setStorageOrigin(StorageOrigin.USER);
    document.setCreatedAt(LocalDateTime.now());
    document.setUpdatedAt(LocalDateTime.now());
    documentRepository.persist(document).await().indefinitely();
    return document;
  }

  // ---- findByOnboardingId ----

  @Test
  void findByOnboardingId_returnsDocumentOfCurrentTenant() {
    persist("onb-1", "AR");

    assertNotNull(documentRepository.findByOnboardingId("onb-1").await().indefinitely());
  }

  @Test
  void findByOnboardingId_doesNotReturnDocumentOfAnotherTenant() {
    persist("onb-1", "PNPG");

    assertNull(documentRepository.findByOnboardingId("onb-1").await().indefinitely());
  }

  @Test
  void findByOnboardingId_stillReturnsLegacyUntaggedDocument() {
    persist("onb-1", null);

    assertNotNull(documentRepository.findByOnboardingId("onb-1").await().indefinitely());
  }

  // ---- findDocumentById ----

  @Test
  void findDocumentById_returnsDocumentOfCurrentTenant() {
    Document document = persist("onb-1", "AR");

    assertNotNull(documentRepository.findDocumentById(document.getId()).await().indefinitely());
  }

  @Test
  void findDocumentById_doesNotReturnDocumentOfAnotherTenant() {
    Document document = persist("onb-1", "PNPG");

    assertNull(documentRepository.findDocumentById(document.getId()).await().indefinitely());
  }

  @Test
  void findDocumentById_stillReturnsLegacyUntaggedDocument() {
    Document document = persist("onb-1", null);

    assertNotNull(documentRepository.findDocumentById(document.getId()).await().indefinitely());
  }

  // ---- findAttachments / findAttachment ----

  @Test
  void findAttachments_returnsOnlyAttachmentsOfCurrentTenant() {
    persist("onb-1", "AR", DocumentType.ATTACHMENT, "a.pdf");
    persist("onb-1", "PNPG", DocumentType.ATTACHMENT, "b.pdf");

    assertEquals(1, documentRepository.findAttachments("onb-1").await().indefinitely().size());
  }

  @Test
  void findAttachment_doesNotReturnAttachmentOfAnotherTenant() {
    persist("onb-1", "PNPG", DocumentType.ATTACHMENT, "a.pdf");

    assertNull(
        documentRepository
            .findAttachment("onb-1", DocumentType.ATTACHMENT.name(), "a.pdf")
            .await()
            .indefinitely());
  }

  // ---- countUserAttachmentsByDocumentId (native query) ----

  @Test
  void countUserAttachments_ignoresAttachmentsOfAnotherTenant() {
    persist("onb-1", "AR", DocumentType.ATTACHMENT, "doc-1");
    persist("onb-1", "PNPG", DocumentType.ATTACHMENT, "doc-1_2");

    assertEquals(
        1,
        documentRepository
            .countUserAttachmentsByDocumentId("onb-1", "doc-1")
            .await()
            .indefinitely());
  }

  @Test
  void countUserAttachments_stillCountsLegacyUntaggedAttachments() {
    persist("onb-1", null, DocumentType.ATTACHMENT, "doc-1");

    assertEquals(
        1,
        documentRepository
            .countUserAttachmentsByDocumentId("onb-1", "doc-1")
            .await()
            .indefinitely());
  }

  // ---- updates ----

  @Test
  void updateContractFiles_doesNotTouchAnotherTenantDocument() {
    persist("onb-1", "PNPG");

    assertEquals(
        0L,
        documentRepository
            .updateContractFiles("onb-1", "signed/path", "contract.pdf")
            .await()
            .indefinitely());
  }

  @Test
  void updateContractFiles_updatesCurrentTenantDocument() {
    persist("onb-1", "AR");

    assertEquals(
        1L,
        documentRepository
            .updateContractFiles("onb-1", "signed/path", "contract.pdf")
            .await()
            .indefinitely());
  }

  @Test
  void updateAttachmentPathById_doesNotTouchAnotherTenantDocument() {
    Document document = persist("onb-1", "PNPG");

    assertEquals(
        0L,
        documentRepository
            .updateAttachmentPathById(document.getId(), "new/path")
            .await()
            .indefinitely());
  }

  @Test
  void updateContractSignedByOnboardingId_doesNotTouchAnotherTenantDocument() {
    persist("onb-1", "PNPG");

    assertEquals(
        0L,
        documentRepository
            .updateContractSignedByOnboardingId("onb-1", "signed/path")
            .await()
            .indefinitely());
  }

  @Test
  void updateUpdatedAt_doesNotTouchAnotherTenantDocument() {
    persist("onb-1", "PNPG");

    assertEquals(
        0L,
        documentRepository
            .updateUpdatedAt("onb-1", LocalDateTime.now())
            .await()
            .indefinitely());
  }

  @Test
  void touchUpdatedAtById_doesNotTouchAnotherTenantDocument() {
    Document document = persist("onb-1", "PNPG");

    assertEquals(
        0L, documentRepository.touchUpdatedAtById(document.getId()).await().indefinitely());
  }

  @Test
  void updateContractFilesById_doesNotTouchAnotherTenantDocument() {
    Document document = persist("onb-1", "PNPG");

    assertEquals(
        0L,
        documentRepository
            .updateContractFilesById(document.getId(), "signed", "contract.pdf", 1)
            .await()
            .indefinitely());
  }

  // ---- delete ----

  /**
   * A delete that reported success would tell the caller the document exists, which is the same
   * cross-tenant existence leak the read paths are careful to avoid.
   */
  @Test
  void deleteDocument_doesNotDeleteAnotherTenantDocument() {
    Document document = persist("onb-1", "PNPG");

    assertFalse(documentRepository.deleteDocument(document.getId()).await().indefinitely());

    actAs("PNPG");
    assertNotNull(documentRepository.findDocumentById(document.getId()).await().indefinitely());
  }

  @Test
  void deleteDocument_deletesCurrentTenantDocument() {
    Document document = persist("onb-1", "AR");

    assertTrue(documentRepository.deleteDocument(document.getId()).await().indefinitely());
    assertNull(documentRepository.findDocumentById(document.getId()).await().indefinitely());
  }

  // ---- no tenant resolvable ----

  /**
   * Callers running outside a request keep working unscoped during the migration phase; pinning it
   * makes the concession visible, so that removing it later is a deliberate change.
   */
  @Test
  void withoutAResolvableTenantQueriesAreLeftUnscoped() {
    persist("onb-1", "PNPG");
    actAs(null);

    assertNotNull(documentRepository.findByOnboardingId("onb-1").await().indefinitely());
  }
}
