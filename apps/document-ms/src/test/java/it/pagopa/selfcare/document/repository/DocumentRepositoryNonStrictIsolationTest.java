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
        assertThat(inRequest("AR", () -> documentRepository.findDocumentById("pnpg-contract").await().indefinitely()))
                .isNull();
    }

    @Test
    void writesDoNotModifyLegacyRecordsEvenWhenTheyAreReadable() {
        Long updated = inRequest("AR", () -> documentRepository
                .updateContractFilesById("legacy-contract", "ar-write.pdf", "ar-contract.pdf", 1)
                .await().indefinitely());
        Boolean deleted = inRequest("AR", () -> documentRepository.deleteDocument("legacy-contract").await().indefinitely());

        assertThat(updated).isZero();
        assertThat(deleted).isFalse();
        assertThat(inRequest("AR", () -> documentRepository.findDocumentById("legacy-contract").await().indefinitely()))
                .satisfies(document -> {
                    assertThat(document.getTenantId()).isNull();
                    assertThat(document.getContractSigned()).isEqualTo("signed-legacy-contract.pdf");
                });
    }

    @Test
    void scopedCountsIncludeReadableLegacyRecordsOnly() {
        assertThat(inRequest("AR", () -> documentRepository.countUserAttachmentsByDocumentId("legacy-onboarding", "legacy-doc").await().indefinitely()))
                .isEqualTo(1);
        assertThat(inRequest("PNPG", () -> documentRepository.countUserAttachmentsByDocumentId("legacy-onboarding", "legacy-doc").await().indefinitely()))
                .isEqualTo(1);
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
                    "MONGODB_CONNECTION_STRING_PNPG", "mongodb://localhost:27017",
                    "selfcare.tenant.strict-data-isolation", "false");
        }
    }
}
