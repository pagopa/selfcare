package it.pagopa.selfcare.document.repository;

import io.quarkus.mongodb.panache.reactive.ReactivePanacheMongoRepositoryBase;
import io.quarkus.panache.common.Sort;
import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.document.model.entity.Document;
import jakarta.enterprise.context.ApplicationScoped;

import java.time.LocalDateTime;
import java.util.List;

import static it.pagopa.selfcare.onboarding.common.DocumentType.*;

@ApplicationScoped
    public class DocumentRepository implements ReactivePanacheMongoRepositoryBase<Document, String> {

    private static final List<String> CONTRACT_TYPES = List.of(INSTITUTION.name(), USER.name());
    private static final String ONBOARDING_AND_TYPES_FILTER = "onboardingId = ?1 and type in ?2";

    /**
     * Step_1 SELC-8 migration-phase tenant filter: matches documents tagged for the given tenant
     * plus documents written before the {@code tenantId} discriminator existed (untagged).
     * <p>
     * Once the backfill has tagged every pre-existing document, the {@code or tenantId is null}
     * branch MUST be dropped so that untagged documents are rejected rather than shared.
     */
    private static final String TENANT_FILTER_P3 = "(tenantId = ?3 or tenantId is null)";
    private static final String TENANT_FILTER_P2 = "(tenantId = ?2 or tenantId is null)";

    public Uni<Long> updateContractFiles(String onboardingId, String contractSigned, String contractFilename) {
        return update("contractSigned = ?1 and contractFilename = ?2 and updatedAt = ?3",
                contractSigned, contractFilename, LocalDateTime.now())
                .where(ONBOARDING_AND_TYPES_FILTER, onboardingId, CONTRACT_TYPES);
    }

    public Uni<Long> updateContractFilesById(String documentId, String contractSigned, String contractFilename, Integer signingStep) {
        return update("contractSigned = ?1 and contractFilename = ?2 and signingStep = ?3 and updatedAt = ?4",
                contractSigned, contractFilename, signingStep, LocalDateTime.now())
                .where("_id = ?1", documentId);
    }

    public Uni<Long> updateAttachmentPathById(String documentId, String attachmentPath) {
        return update("attachmentPath = ?1 and updatedAt = ?2", attachmentPath, LocalDateTime.now())
                .where("_id = ?1", documentId);
    }

    public Uni<Long> touchUpdatedAtById(String documentId) {
        return update("updatedAt = ?1", LocalDateTime.now())
                .where("_id = ?1", documentId);
    }

    public Uni<Document> findAttachment(String onboardingId, String type, String name) {
        return find("onboardingId = ?1 and type = ?2 and attachmentName = ?3", onboardingId, type, name)
                .firstResult();
    }

    public Uni<List<Document>> findAttachments(String onboardingId) {
        return find("onboardingId = ?1 and type = ?2", onboardingId, ATTACHMENT.name()).list();
    }

    /**
     * Counts USER-storage attachments matching a given {@code documentId} (RequiredDocument.id),
     * either exactly or with a numeric suffix like {@code documentId_2}, {@code documentId_3}.
     */
    public Uni<Long> countUserAttachmentsByDocumentId(String onboardingId, String documentId) {
      return count(
        "{ 'onboardingId': ?1, 'type': ?2, 'storageOrigin': ?3, 'attachmentName': { '$regex': ?4 } }",
        onboardingId,
        ATTACHMENT.name(),
        USER.name(),
        "^" + java.util.regex.Pattern.quote(documentId));
    }

    public Uni<Document> findByOnboardingId(String onboardingId) {
    return find(
            ONBOARDING_AND_TYPES_FILTER,
            Sort.by("createdAt").descending(),
            onboardingId,
            CONTRACT_TYPES)
        .firstResult();
    }

    /**
     * Step_1 SELC-8.1/8.3: tenant-scoped variant of {@link #findByOnboardingId(String)}.
     * The tenant filter MUST always be the validated tenant from Step_0's {@code TenantContext},
     * never re-derived independently (SELC-8.5).
     *
     * <p><b>Migration phase:</b> documents written before the {@code tenantId} discriminator was
     * introduced carry no tenant, and would otherwise become invisible to every tenant (breaking
     * all reads). Until the backfill has run, untagged documents are therefore still matched — see
     * {@code TENANT_FILTER_P3}.
     */
    public Uni<Document> findByOnboardingIdForTenant(String onboardingId, String tenantId) {
        return find(
                ONBOARDING_AND_TYPES_FILTER + " and " + TENANT_FILTER_P3,
                Sort.by("createdAt").descending(),
                onboardingId,
                CONTRACT_TYPES,
                tenantId)
            .firstResult();
    }

    /**
     * Step_1 SELC-8.1/8.3: tenant-scoped variant of {@code findById}. Returns empty (not the
     * document) when the document exists but is tagged for a different tenant, so callers cannot
     * distinguish "not found" from "found for another tenant" (no cross-tenant existence leak).
     * Same migration-phase caveat as {@link #findByOnboardingIdForTenant} applies.
     */
    public Uni<Document> findByIdForTenant(String id, String tenantId) {
        return find("_id = ?1 and " + TENANT_FILTER_P2, id, tenantId).firstResult();
    }

    public Uni<Long> updateContractSignedByOnboardingId(String onboardingId, String contractSignedPath) {
        return update("contractSigned = ?1", contractSignedPath)
                .where(ONBOARDING_AND_TYPES_FILTER, onboardingId, CONTRACT_TYPES);
    }

    public Uni<Long> updateUpdatedAt(String onboardingId, LocalDateTime updatedAt) {
        return update("updatedAt = ?1", updatedAt)
                .where(ONBOARDING_AND_TYPES_FILTER, onboardingId, CONTRACT_TYPES);
    }

    public Uni<Boolean> deleteDocument(String documentId) {
        return deleteById(documentId);
    }

}
