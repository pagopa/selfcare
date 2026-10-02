package it.pagopa.selfcare.document.repository;

import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Sorts;
import io.quarkus.mongodb.panache.reactive.ReactivePanacheMongoRepositoryBase;
import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.document.model.StorageOrigin;
import it.pagopa.selfcare.document.model.entity.Document;
import it.pagopa.selfcare.tenant.TenantContext;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.bson.conversions.Bson;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.time.LocalDateTime;
import java.util.List;

import static it.pagopa.selfcare.document.util.LogSanitizer.sanitize;
import static it.pagopa.selfcare.onboarding.common.DocumentType.*;

@Slf4j
@ApplicationScoped
public class DocumentRepository implements ReactivePanacheMongoRepositoryBase<Document, String> {

    private static final List<String> CONTRACT_TYPES = List.of(INSTITUTION.name(), USER.name());

    @Inject
    TenantContext tenantContext;

    @ConfigProperty(name = "selfcare.tenant.strict-data-isolation", defaultValue = "false")
    boolean strictDataIsolation;

    @Override
    public Uni<Document> persist(Document document) {
        document.setTenantId(tenantContext.requiredTenantId());
        return ReactivePanacheMongoRepositoryBase.super.persist(document);
    }

    public Uni<Document> findDocumentById(String documentId) {
        return find(scoped(Filters.eq("_id", documentId))).firstResult();
    }

    public Uni<Long> updateContractFiles(String onboardingId, String contractSigned, String contractFilename) {
        return update("contractSigned = ?1 and contractFilename = ?2 and updatedAt = ?3",
                contractSigned, contractFilename, LocalDateTime.now())
                .where(scopedForWrite(onboardingAndContractTypes(onboardingId)))
                .invoke(updated -> logIfNoWrite("updateContractFiles", "onboardingId", onboardingId, updated));
    }

    public Uni<Long> updateContractFilesById(String documentId, String contractSigned, String contractFilename, Integer signingStep) {
        return update("contractSigned = ?1 and contractFilename = ?2 and signingStep = ?3 and updatedAt = ?4",
                contractSigned, contractFilename, signingStep, LocalDateTime.now())
                .where(scopedForWrite(Filters.eq("_id", documentId)))
                .invoke(updated -> logIfNoWrite("updateContractFilesById", "documentId", documentId, updated));
    }

    public Uni<Long> updateAttachmentPathById(String documentId, String attachmentPath) {
        return update("attachmentPath = ?1 and updatedAt = ?2", attachmentPath, LocalDateTime.now())
                .where(scopedForWrite(Filters.eq("_id", documentId)))
                .invoke(updated -> logIfNoWrite("updateAttachmentPathById", "documentId", documentId, updated));
    }

    public Uni<Long> touchUpdatedAtById(String documentId) {
        return update("updatedAt = ?1", LocalDateTime.now())
                .where(scopedForWrite(Filters.eq("_id", documentId)))
                .invoke(updated -> logIfNoWrite("touchUpdatedAtById", "documentId", documentId, updated));
    }

    public Uni<Document> findAttachment(String onboardingId, String type, String name) {
        return find(scoped(Filters.and(
                        Filters.eq("onboardingId", onboardingId),
                        Filters.eq("type", type),
                        Filters.eq("attachmentName", name))))
                .firstResult();
    }

    public Uni<Document> findRelatedDocument(String onboardingId, String documentId) {
        return find(scoped(Filters.and(
                        Filters.eq("_id", documentId),
                        Filters.or(
                                Filters.eq("onboardingId", onboardingId),
                                Filters.eq("rootOnboardingId", onboardingId)))))
                .firstResult();
    }

    public Uni<List<Document>> findAttachments(String onboardingId) {
        return find(scoped(Filters.and(
                Filters.eq("onboardingId", onboardingId),
                Filters.eq("type", ATTACHMENT.name())))).list();
    }

    public Uni<List<Document>> findUserAttachmentsByOnboardingId(String onboardingId) {
        return find(scoped(Filters.and(
                Filters.eq("onboardingId", onboardingId),
                Filters.eq("type", ATTACHMENT.name()),
                Filters.eq("storageOrigin", StorageOrigin.USER.name())))).list();
    }

    /**
     * Counts USER-storage attachments matching a given {@code documentId} (RequiredDocument.id),
     * either exactly or with a numeric suffix like {@code documentId_2}, {@code documentId_3}.
     */
    public Uni<Long> countUserAttachmentsByDocumentId(String onboardingId, String documentId) {
        return count(scoped(Filters.and(
                Filters.eq("onboardingId", onboardingId),
                Filters.eq("type", ATTACHMENT.name()),
                Filters.eq("storageOrigin", USER.name()),
                Filters.regex("attachmentName", "^" + java.util.regex.Pattern.quote(documentId)))));
    }

    public Uni<Document> findByOnboardingId(String onboardingId) {
        return find(
                scoped(onboardingAndContractTypes(onboardingId)),
                Sorts.descending("createdAt"))
                .firstResult();
    }

    public Uni<Long> updateContractSignedByOnboardingId(String onboardingId, String contractSignedPath) {
        return update("contractSigned = ?1", contractSignedPath)
                .where(scopedForWrite(onboardingAndContractTypes(onboardingId)))
                .invoke(updated -> logIfNoWrite("updateContractSignedByOnboardingId", "onboardingId", onboardingId, updated));
    }

    public Uni<Long> updateUpdatedAt(String onboardingId, LocalDateTime updatedAt) {
        return update("updatedAt = ?1", updatedAt)
                .where(scopedForWrite(onboardingAndContractTypes(onboardingId)))
                .invoke(updated -> logIfNoWrite("updateUpdatedAt", "onboardingId", onboardingId, updated));
    }

    public Uni<Boolean> deleteDocument(String documentId) {
        return delete(scopedForWrite(Filters.eq("_id", documentId)))
                .invoke(deleted -> logIfNoWrite("deleteDocument", "documentId", documentId, deleted))
                .map(deleted -> deleted > 0);
    }

    private Bson scoped(Bson query) {
        String tenantId = tenantContext.requiredTenantId();
        Bson tenantScope = strictDataIsolation
                ? Filters.eq("tenantId", tenantId)
                : Filters.or(Filters.eq("tenantId", tenantId), Filters.eq("tenantId", null));
        return Filters.and(query, tenantScope);
    }

    private Bson scopedForWrite(Bson query) {
        return Filters.and(query, Filters.eq("tenantId", tenantContext.requiredTenantId()));
    }

    private Bson onboardingAndContractTypes(String onboardingId) {
        return Filters.and(
                Filters.eq("onboardingId", onboardingId),
                Filters.in("type", CONTRACT_TYPES));
    }

    private void logIfNoWrite(String operation, String key, String value, Long count) {
        if (count == null || count == 0) {
            log.warn("Scoped document write matched no records operation={}, {}={}, tenant={}",
                    sanitize(operation), sanitize(key), sanitize(value), sanitize(tenantContext.requiredTenantId()));
        }
    }

}
