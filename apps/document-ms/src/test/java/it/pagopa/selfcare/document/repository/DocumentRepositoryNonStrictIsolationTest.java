package it.pagopa.selfcare.document.repository;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ManagedContext;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import io.quarkus.test.mongodb.MongoTestResource;
import it.pagopa.selfcare.document.model.StorageOrigin;
import it.pagopa.selfcare.document.model.entity.Document;
import it.pagopa.selfcare.onboarding.common.DocumentType;
import it.pagopa.selfcare.tenant.TenantContext;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
@QuarkusTestResource(value = MongoTestResource.class, restrictToAnnotatedClass = true)
@TestProfile(DocumentRepositoryNonStrictIsolationTest.NonStrictSharedMongoProfile.class)
class DocumentRepositoryNonStrictIsolationTest {

    @Inject
    DocumentRepository documentRepository;

    @Inject
    TenantContext tenantContext;

    @BeforeEach
    void seedDocuments() {
        inRequest("AR", () -> {
            Document.mongoCollection().deleteMany(new org.bson.Document()).await().indefinitely();
            Document.mongoCollection().insertOne(contract("ar-contract", "shared-onboarding", "AR")).await().indefinitely();
            Document.mongoCollection().insertOne(contract("pnpg-contract", "shared-onboarding", "PNPG")).await().indefinitely();
            Document.mongoCollection().insertOne(contract("legacy-contract", "legacy-onboarding", null)).await().indefinitely();
            Document.mongoCollection().insertOne(attachment("legacy-attachment", "legacy-onboarding", null, "legacy-doc")).await().indefinitely();
            return null;
        });
    }

    @Test
    void readsIncludeLegacyRecordsWhenStrictIsolationIsDisabled() {
        assertThat(inRequest("AR", () -> documentRepository.findDocumentById("legacy-contract").await().indefinitely()))
                .extracting(Document::getTenantId)
                .isNull();
        assertThat(inRequest("AR", () -> documentRepository.findByOnboardingId("legacy-onboarding").await().indefinitely()))
                .extracting(Document::getId)
                .isEqualTo("legacy-contract");
        assertThat(inRequest("AR", () -> documentRepository.findCurrentSignedContract("legacy-onboarding").await().indefinitely()))
                .extracting(Document::getId)
                .isEqualTo("legacy-contract");
        assertThat(inRequest("AR", () -> documentRepository.findDocumentById("pnpg-contract").await().indefinitely()))
                .isNull();
    }

    @Test
    void writesReachLegacyRecordsUntilStrictIsolationIsEnabled() {
        Long updated = inRequest("AR", () -> documentRepository
                .updateContractFilesById("legacy-contract", "ar-write.pdf", "ar-contract.pdf", 1)
                .await().indefinitely());

        assertThat(updated).isEqualTo(1);
        assertThat(inRequest("AR", () -> documentRepository.findDocumentById("legacy-contract").await().indefinitely()))
                .satisfies(document -> {
                    assertThat(document.getTenantId()).isNull();
                    assertThat(document.getContractSigned()).isEqualTo("ar-write.pdf");
                });

        Boolean deleted = inRequest("AR", () -> documentRepository.deleteDocument("legacy-attachment").await().indefinitely());

        assertThat(deleted).isTrue();
    }

    @Test
    void writesDoNotModifyOtherTenantRecords() {
        Long updated = inRequest("AR", () -> documentRepository
                .updateContractFilesById("pnpg-contract", "ar-write.pdf", "ar-contract.pdf", 1)
                .await().indefinitely());
        Boolean deleted = inRequest("AR", () -> documentRepository.deleteDocument("pnpg-contract").await().indefinitely());

        assertThat(updated).isZero();
        assertThat(deleted).isFalse();
        assertThat(inRequest("PNPG", () -> documentRepository.findDocumentById("pnpg-contract").await().indefinitely()))
                .extracting(Document::getContractSigned)
                .isEqualTo("signed-pnpg-contract.pdf");
    }

    @Test
    void scopedCountsIncludeReadableLegacyRecordsOnly() {
        assertThat(inRequest("AR", () -> documentRepository.countUserAttachmentsByDocumentId("legacy-onboarding", "legacy-doc").await().indefinitely()))
                .isEqualTo(1);
        assertThat(inRequest("PNPG", () -> documentRepository.countUserAttachmentsByDocumentId("legacy-onboarding", "legacy-doc").await().indefinitely()))
                .isEqualTo(1);
    }

    @Test
    void scopedListsIncludeLegacyRecords() {
        assertThat(inRequest("AR", () -> documentRepository.findAttachments("legacy-onboarding").await().indefinitely()))
                .extracting(Document::getId)
                .containsExactly("legacy-attachment");
        assertThat(inRequest("AR", () -> documentRepository.findAttachments("shared-onboarding").await().indefinitely()))
                .isEmpty();
    }

    @Test
    void findByOnboardingIdOrRootOnboardingIdReturnsWholeDossierForRootAndOnlyOwnDocumentsForChild() {
        inRequest("AR", () -> {
            Document userContract = contract("user-contract", "user-onboarding", "AR");
            userContract.setType(DocumentType.USER);
            userContract.setRootOnboardingId("dossier-onboarding");
            userContract.setCreatedAt(LocalDateTime.now().plusMinutes(2));
            Document rootContract = contract("root-contract", "dossier-onboarding", "AR");
            rootContract.setCreatedAt(LocalDateTime.now().minusMinutes(1));
            Document rootAttachment = attachment("root-attachment", "dossier-onboarding", "AR", "Allegato");
            rootAttachment.setCreatedAt(LocalDateTime.now().plusMinutes(1));
            Document otherTenant = contract("pnpg-dossier-contract", "dossier-onboarding", "PNPG");
            Document.mongoCollection().insertOne(userContract).await().indefinitely();
            Document.mongoCollection().insertOne(rootAttachment).await().indefinitely();
            Document.mongoCollection().insertOne(rootContract).await().indefinitely();
            Document.mongoCollection().insertOne(otherTenant).await().indefinitely();
            return null;
        });

        assertThat(inRequest("AR", () -> documentRepository.findByOnboardingIdOrRootOnboardingId("dossier-onboarding").await().indefinitely()))
                .extracting(Document::getId)
                .containsExactly("root-contract", "root-attachment", "user-contract");
        assertThat(inRequest("AR", () -> documentRepository.findByOnboardingIdOrRootOnboardingId("user-onboarding").await().indefinitely()))
                .extracting(Document::getId)
                .containsExactly("user-contract");
        assertThat(inRequest("AR", () -> documentRepository.findByOnboardingIdOrRootOnboardingId("missing-onboarding").await().indefinitely()))
                .isEmpty();
    }

    @Test
    void findCurrentSignedContractPrefersLatestActivatedUpdateAndIgnoresUnactivatedOrBlankUpdates() {
        inRequest("AR", () -> {
            LocalDateTime base = LocalDateTime.of(2026, 1, 1, 10, 0);
            Document root = contract("current-root", "current-root", "AR");
            root.setCreatedAt(base);
            Document activated = contract("current-update-active", "current-update-active", "AR");
            activated.setRootOnboardingId("current-root");
            activated.setCreatedAt(base.plusDays(1));
            activated.setActivatedAt(base.plusDays(1));
            Document latestActivated = contract("current-update-latest", "current-update-latest", "AR");
            latestActivated.setRootOnboardingId("current-root");
            latestActivated.setCreatedAt(base.plusDays(4));
            latestActivated.setActivatedAt(base.plusDays(4));
            Document pending = contract("current-update-pending", "current-update-pending", "AR");
            pending.setRootOnboardingId("current-root");
            pending.setCreatedAt(base.plusDays(2));
            pending.setActivatedAt(null);
            Document blank = contract("current-update-blank", "current-update-blank", "AR");
            blank.setRootOnboardingId("current-root");
            blank.setContractSigned("   ");
            blank.setCreatedAt(base.plusDays(3));
            Document user = contract("current-user", "current-user", "AR");
            user.setType(DocumentType.USER);
            user.setRootOnboardingId("current-root");
            user.setCreatedAt(base.plusDays(5));
            Document attachment = attachment("current-attachment", "current-attachment", "AR", "required-doc");
            attachment.setRootOnboardingId("current-root");
            attachment.setCreatedAt(base.plusDays(6));
            Document.mongoCollection().insertMany(java.util.List.of(root, activated, latestActivated, pending, blank, user, attachment))
                    .await().indefinitely();
            return null;
        });

        assertThat(inRequest("AR", () -> documentRepository.findCurrentSignedContract("current-root").await().indefinitely()))
                .extracting(Document::getId)
                .isEqualTo("current-update-latest");
    }

    @Test
    void findCurrentSignedContractPreservesOwnLookupSemanticsAndReturnsNullWhenMissing() {
        inRequest("AR", () -> {
            Document unsignedRoot = contract("own-unsigned-root", "own-unsigned-root", "AR");
            unsignedRoot.setContractSigned(null);
            Document pendingUpdate = contract("own-pending-update", "own-pending-update", "AR");
            pendingUpdate.setRootOnboardingId("own-unsigned-root");
            pendingUpdate.setActivatedAt(null);
            Document.mongoCollection().insertMany(java.util.List.of(unsignedRoot, pendingUpdate)).await().indefinitely();
            return null;
        });

        assertThat(inRequest("AR", () -> documentRepository.findCurrentSignedContract("own-unsigned-root").await().indefinitely()))
                .extracting(Document::getId)
                .isEqualTo("own-unsigned-root");
        assertThat(inRequest("AR", () -> documentRepository.findCurrentSignedContract("own-pending-update").await().indefinitely()))
                .extracting(Document::getId)
                .isEqualTo("own-pending-update");
        assertThat(inRequest("AR", () -> documentRepository.findCurrentSignedContract("not-found").await().indefinitely()))
                .isNull();
    }

    @Test
    void activateContractSetsTimestampOnceAndRejectsMissingOrBlankSignedFiles() {
        LocalDateTime originalUpdatedAt = LocalDateTime.of(2026, 1, 1, 10, 0);
        inRequest("AR", () -> {
            Document signed = contract("activate-signed", "activate-onboarding", "AR");
            signed.setUpdatedAt(originalUpdatedAt);
            Document blank = contract("activate-blank", "activate-onboarding", "AR");
            blank.setContractSigned("  ");
            Document unsigned = baseDocument("activate-unsigned", "activate-onboarding", "AR", DocumentType.INSTITUTION);
            Document attachment = attachment("activate-attachment", "activate-onboarding", "AR", "required-doc");
            Document.mongoCollection().insertMany(java.util.List.of(signed, blank, unsigned, attachment)).await().indefinitely();
            return null;
        });

        assertThat(inRequest("AR", () -> documentRepository.activateContract("activate-onboarding").await().indefinitely()))
                .isEqualTo(1L);
        LocalDateTime activatedAt = inRequest("AR", () -> documentRepository.findDocumentById("activate-signed").await().indefinitely().getActivatedAt());
        assertThat(activatedAt).isNotNull();
        assertThat(inRequest("AR", () -> documentRepository.activateContract("activate-onboarding").await().indefinitely()))
                .isZero();
        assertThat(inRequest("AR", () -> documentRepository.findDocumentById("activate-signed").await().indefinitely().getActivatedAt()))
                .isEqualTo(activatedAt);
        assertThat(inRequest("AR", () -> documentRepository.findDocumentById("activate-signed").await().indefinitely().getUpdatedAt()))
                .isEqualTo(originalUpdatedAt);
        assertThat(inRequest("AR", () -> documentRepository.findDocumentById("activate-attachment").await().indefinitely().getActivatedAt()))
                .isNull();
        assertThat(inRequest("AR", () -> documentRepository.existsSignedContract("activate-onboarding").await().indefinitely()))
                .isTrue();
        assertThat(inRequest("AR", () -> documentRepository.existsSignedContract("missing-contract").await().indefinitely()))
                .isFalse();
    }

    @Test
    void activateContractSupportsSignedUserContracts() {
        inRequest("AR", () -> {
            Document userContract = contract("activate-user-contract", "activate-user-onboarding", "AR");
            userContract.setType(DocumentType.USER);
            userContract.setRootOnboardingId("activate-root-onboarding");
            Document.mongoCollection().insertOne(userContract).await().indefinitely();
            return null;
        });

        assertThat(inRequest("AR", () -> documentRepository.activateContract("activate-user-onboarding").await().indefinitely()))
                .isEqualTo(1L);
        assertThat(inRequest("AR", () -> documentRepository.findDocumentById("activate-user-contract").await().indefinitely()))
                .extracting(Document::getActivatedAt)
                .isNotNull();
    }

    private <T> T inRequest(String tenantId, Supplier<T> action) {
        ManagedContext requestContext = Arc.container().requestContext();
        requestContext.activate();
        try {
            tenantContext.setTenantId(tenantId);
            return action.get();
        } finally {
            requestContext.terminate();
        }
    }

    private Document contract(String id, String onboardingId, String tenantId) {
        Document document = baseDocument(id, onboardingId, tenantId, DocumentType.INSTITUTION);
        document.setContractSigned("signed-" + id + ".pdf");
        document.setContractFilename(id + ".pdf");
        document.setStorageOrigin(StorageOrigin.SYSTEM);
        return document;
    }

    private Document attachment(String id, String onboardingId, String tenantId, String attachmentName) {
        Document document = baseDocument(id, onboardingId, tenantId, DocumentType.ATTACHMENT);
        document.setAttachmentName(attachmentName);
        document.setStorageOrigin(StorageOrigin.USER);
        return document;
    }

    private Document baseDocument(String id, String onboardingId, String tenantId, DocumentType type) {
        Document document = new Document();
        document.setId(id);
        document.setTenantId(tenantId);
        document.setOnboardingId(onboardingId);
        document.setRootOnboardingId(onboardingId);
        document.setType(type);
        document.setProductId("prod-io");
        document.setCreatedAt(LocalDateTime.now());
        document.setUpdatedAt(LocalDateTime.now());
        return document;
    }

    public static class NonStrictSharedMongoProfile implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of(
                    "tenant.registry.json",
                    "{\"AR\":{\"mongo\":{\"account\":\"cosmos-ar\",\"database\":\"selcDocument\","
                            + "\"connectionStringEnvVar\":\"MONGODB_CONNECTION_STRING_AR\"}},"
                            + "\"PNPG\":{\"mongo\":{\"account\":\"cosmos-pnpg\",\"database\":\"selcDocument\","
                            + "\"connectionStringEnvVar\":\"MONGODB_CONNECTION_STRING_PNPG\"}}}",
                    "tenant.supported-tenants", "AR,PNPG",
                    "tenant.storage.mandatory-keys", "",
                    "MONGODB_CONNECTION_STRING_PNPG", "mongodb://localhost:27017",
                    "selfcare.tenant.strict-data-isolation", "false");
        }
    }
}
