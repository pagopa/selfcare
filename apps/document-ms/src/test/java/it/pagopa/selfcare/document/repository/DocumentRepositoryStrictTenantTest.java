package it.pagopa.selfcare.document.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;

import io.quarkus.test.InjectMock;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.mongodb.MongoTestResource;
import it.pagopa.selfcare.document.model.StorageOrigin;
import it.pagopa.selfcare.document.model.entity.Document;
import it.pagopa.selfcare.document.service.CurrentTenantProvider;
import it.pagopa.selfcare.onboarding.common.DocumentType;
import jakarta.inject.Inject;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Pins the post-backfill behaviour of the tenant filter, with {@code
 * selfcare.tenant.strict-data-isolation} turned on.
 *
 * <p>{@link DocumentRepositoryTenantTest} covers the migration phase, where an untagged document is
 * still readable. That leniency is what the backfill exists to remove, and it is deliberately the
 * default, so without this class the strict path would ship untested and would first be exercised
 * in whichever environment flipped the flag. Here the same queries are checked against the
 * behaviour that actually makes the isolation a boundary: an untagged document belongs to nobody
 * and is returned to nobody.
 */
@QuarkusTest
@TestProfile(DocumentRepositoryStrictTenantTest.StrictTenantIsolationProfile.class)
@QuarkusTestResource(value = MongoTestResource.class, restrictToAnnotatedClass = true)
class DocumentRepositoryStrictTenantTest {

  public static class StrictTenantIsolationProfile implements QuarkusTestProfile {
    @Override
    public Map<String, String> getConfigOverrides() {
      return Map.of("selfcare.tenant.strict-data-isolation", "true");
    }
  }

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

  private Document persist(String onboardingId, String tenantId) {
    return persist(onboardingId, tenantId, DocumentType.INSTITUTION, null);
  }

  @Test
  void findByOnboardingId_stillReturnsDocumentOfCurrentTenant() {
    persist("onb-1", "AR");

    assertNotNull(documentRepository.findByOnboardingId("onb-1").await().indefinitely());
  }

  @Test
  void findByOnboardingId_noLongerReturnsLegacyUntaggedDocument() {
    persist("onb-1", null);

    assertNull(documentRepository.findByOnboardingId("onb-1").await().indefinitely());
  }

  @Test
  void findDocumentById_noLongerReturnsLegacyUntaggedDocument() {
    Document document = persist("onb-1", null);

    assertNull(documentRepository.findDocumentById(document.getId()).await().indefinitely());
  }

  @Test
  void findAttachments_excludesLegacyUntaggedAttachments() {
    persist("onb-1", "AR", DocumentType.ATTACHMENT, "a.pdf");
    persist("onb-1", null, DocumentType.ATTACHMENT, "b.pdf");

    assertEquals(1, documentRepository.findAttachments("onb-1").await().indefinitely().size());
  }

  /** The native-syntax query has its own predicate, so it needs its own proof it went strict too. */
  @Test
  void countUserAttachments_noLongerCountsLegacyUntaggedAttachments() {
    persist("onb-1", null, DocumentType.ATTACHMENT, "doc-1");

    assertEquals(
        0,
        documentRepository
            .countUserAttachmentsByDocumentId("onb-1", "doc-1")
            .await()
            .indefinitely());
  }

  @Test
  void countUserAttachments_stillCountsCurrentTenantAttachments() {
    persist("onb-1", "AR", DocumentType.ATTACHMENT, "doc-1");

    assertEquals(
        1,
        documentRepository
            .countUserAttachmentsByDocumentId("onb-1", "doc-1")
            .await()
            .indefinitely());
  }

  @Test
  void updates_noLongerTouchLegacyUntaggedDocuments() {
    persist("onb-1", null);

    assertEquals(
        0L,
        documentRepository
            .updateContractFiles("onb-1", "signed/path", "contract.pdf")
            .await()
            .indefinitely());
  }

  @Test
  void deleteDocument_noLongerDeletesLegacyUntaggedDocument() {
    Document document = persist("onb-1", null);

    assertEquals(
        Boolean.FALSE, documentRepository.deleteDocument(document.getId()).await().indefinitely());
  }

  /**
   * Strictness applies to the tenant predicate, not to callers that have no tenant at all: consumers
   * and schedulers still run unscoped. Removing that concession is a separate decision from the
   * backfill, so the flag deliberately leaves it alone.
   */
  @Test
  void withoutAResolvableTenantQueriesAreStillLeftUnscoped() {
    persist("onb-1", null);
    actAs(null);

    assertNotNull(documentRepository.findByOnboardingId("onb-1").await().indefinitely());
  }
}
