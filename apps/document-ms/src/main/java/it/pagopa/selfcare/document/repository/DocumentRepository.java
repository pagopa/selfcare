package it.pagopa.selfcare.document.repository;

import io.quarkus.mongodb.panache.reactive.ReactivePanacheMongoRepositoryBase;
import io.quarkus.panache.common.Sort;
import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.document.model.StorageOrigin;
import it.pagopa.selfcare.document.model.entity.Document;
import it.pagopa.selfcare.document.service.CurrentTenantProvider;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static it.pagopa.selfcare.onboarding.common.DocumentType.*;

/**
 * All queries are scoped to the tenant validated for the current request (Step_1 SELC-8).
 *
 * <p>The scoping is applied here rather than by each caller on purpose. The earlier approach - a
 * separate {@code ...ForTenant} variant alongside each unscoped method - left both versions
 * callable, so a caller could silently opt out of isolation by picking the wrong one, and every new
 * query method started out unscoped by default. Centralising it means a query cannot reach MongoDB
 * without the tenant predicate.
 *
 * <p>The tenant always comes from {@link CurrentTenantProvider}, that is from the {@code
 * TenantContext} the inbound filter has already reconciled against the JWT claim; it is never
 * re-derived from a raw header (SELC-8.5).
 */
@ApplicationScoped
public class DocumentRepository implements ReactivePanacheMongoRepositoryBase<Document, String> {

    private static final List<String> CONTRACT_TYPES = List.of(INSTITUTION.name(), USER.name());
    private static final String ONBOARDING_AND_TYPES_FILTER = "onboardingId = ?1 and type in ?2";
    private static final String ID_FILTER = "_id = ?1";

    @Inject
    CurrentTenantProvider currentTenantProvider;

    /**
     * Whether untagged documents are still treated as belonging to the current tenant.
     *
     * <p>It is configuration rather than a code constant because the backfill runs at a different
     * time in each environment: a hardcoded switch would mean the strict build cannot be promoted
     * to PROD until PROD has been backfilled, holding back every unrelated change behind a data
     * migration. As a flag, each environment turns isolation strict the moment its own
     * {@code --verify} comes back clean, and can revert without a deployment.
     *
     * <p>It defaults to the lenient behaviour so that deploying this change alone changes nothing.
     * It is temporary: once every environment runs strict, the flag and the {@code or tenantId is
     * null} branch must both be deleted (Step_1/EPIC.md sub-tasks 2 and 10).
     */
    @ConfigProperty(name = "selfcare.tenant.strict-data-isolation", defaultValue = "false")
    boolean strictTenantIsolation;

    /**
     * Appends the tenant predicate to a positional-parameter query, adding the tenant as the next
     * positional argument.
     *
     * <p><b>Migration phase:</b> the predicate matches the current tenant <em>or</em> documents with
     * no tenant at all. Documents written before the discriminator existed carry none, and a strict
     * equality would make every one of them invisible to both tenants - turning existing reads into
     * blanket "not found" the moment this shipped. Once the backfill has tagged every document
     * {@code selfcare.tenant.strict-data-isolation} drops that branch, at which point the filter
     * becomes strictly fail-closed.
     *
     * <p>When no tenant is resolvable the query is left unscoped rather than made unsatisfiable.
     * That is the pre-multitenant behaviour and it keeps callers that legitimately run outside a
     * request working; it is a migration-phase concession, not a security boundary.
     */
    private String tenantScoped(String query, List<Object> params) {
        Optional<String> tenantId = currentTenantProvider.currentTenantId();
        if (tenantId.isEmpty()) {
            return query;
        }
        params.add(tenantId.get());
        return strictTenantIsolation
                ? query + " and tenantId = ?" + params.size()
                : query + " and (tenantId = ?" + params.size() + " or tenantId is null)";
    }

    private static Object[] args(List<Object> params) {
        return params.toArray();
    }

    public Uni<Long> updateContractFiles(String onboardingId, String contractSigned, String contractFilename) {
        List<Object> params = new ArrayList<>(List.of(onboardingId, CONTRACT_TYPES));
        String where = tenantScoped(ONBOARDING_AND_TYPES_FILTER, params);
        return update("contractSigned = ?1 and contractFilename = ?2 and updatedAt = ?3",
                contractSigned, contractFilename, LocalDateTime.now())
                .where(where, args(params));
    }

    public Uni<Long> updateContractFilesById(String documentId, String contractSigned, String contractFilename, Integer signingStep) {
        List<Object> params = new ArrayList<>(List.of(documentId));
        String where = tenantScoped(ID_FILTER, params);
        return update("contractSigned = ?1 and contractFilename = ?2 and signingStep = ?3 and updatedAt = ?4",
                contractSigned, contractFilename, signingStep, LocalDateTime.now())
                .where(where, args(params));
    }

    public Uni<Long> updateAttachmentPathById(String documentId, String attachmentPath) {
        List<Object> params = new ArrayList<>(List.of(documentId));
        String where = tenantScoped(ID_FILTER, params);
        return update("attachmentPath = ?1 and updatedAt = ?2", attachmentPath, LocalDateTime.now())
                .where(where, args(params));
    }

    public Uni<Long> touchUpdatedAtById(String documentId) {
        List<Object> params = new ArrayList<>(List.of(documentId));
        String where = tenantScoped(ID_FILTER, params);
        return update("updatedAt = ?1", LocalDateTime.now())
                .where(where, args(params));
    }

    public Uni<Document> findAttachment(String onboardingId, String type, String name) {
        List<Object> params = new ArrayList<>(List.of(onboardingId, type, name));
        String query = tenantScoped("onboardingId = ?1 and type = ?2 and attachmentName = ?3", params);
        return find(query, args(params)).firstResult();
    }

    public Uni<List<Document>> findAttachments(String onboardingId) {
        List<Object> params = new ArrayList<>(List.of(onboardingId, ATTACHMENT.name()));
        String query = tenantScoped("onboardingId = ?1 and type = ?2", params);
        return find(query, args(params)).list();
    }

    public Uni<List<Document>> findUserAttachmentsByOnboardingId(String onboardingId) {
        return find("onboardingId = ?1 and type = ?2 and storageOrigin = ?3",
                onboardingId, ATTACHMENT.name(), StorageOrigin.USER).list();
    }

    /**
     * Counts USER-storage attachments matching a given {@code documentId} (RequiredDocument.id),
     * either exactly or with a numeric suffix like {@code documentId_2}, {@code documentId_3}.
     *
     * <p>This query is expressed in native MongoDB syntax, so the tenant predicate is written as an
     * explicit {@code $or} rather than going through {@link #tenantScoped}; the semantics are the
     * same, including the migration-phase match on untagged documents.
     */
    public Uni<Long> countUserAttachmentsByDocumentId(String onboardingId, String documentId) {
        String nameRegex = "^" + java.util.regex.Pattern.quote(documentId);
        Optional<String> tenantId = currentTenantProvider.currentTenantId();
        if (tenantId.isEmpty()) {
            return count(
                "{ 'onboardingId': ?1, 'type': ?2, 'storageOrigin': ?3, 'attachmentName': { '$regex': ?4 } }",
                onboardingId, ATTACHMENT.name(), USER.name(), nameRegex);
        }
        if (strictTenantIsolation) {
            return count(
                "{ 'onboardingId': ?1, 'type': ?2, 'storageOrigin': ?3, 'attachmentName': { '$regex': ?4 },"
                    + " 'tenantId': ?5 }",
                onboardingId, ATTACHMENT.name(), USER.name(), nameRegex, tenantId.get());
        }
        return count(
            "{ 'onboardingId': ?1, 'type': ?2, 'storageOrigin': ?3, 'attachmentName': { '$regex': ?4 },"
                + " '$or': [ { 'tenantId': ?5 }, { 'tenantId': null } ] }",
            onboardingId, ATTACHMENT.name(), USER.name(), nameRegex, tenantId.get());
    }

    public Uni<Document> findByOnboardingId(String onboardingId) {
        List<Object> params = new ArrayList<>(List.of(onboardingId, CONTRACT_TYPES));
        String query = tenantScoped(ONBOARDING_AND_TYPES_FILTER, params);
        return find(query, Sort.by("createdAt").descending(), args(params)).firstResult();
    }

    /**
     * Tenant-scoped lookup by id. Returns empty when the document exists but belongs to another
     * tenant, so a caller cannot tell "not found" from "found for another tenant" (no cross-tenant
     * existence leak).
     */
    public Uni<Document> findDocumentById(String id) {
        List<Object> params = new ArrayList<>(List.of(id));
        String query = tenantScoped(ID_FILTER, params);
        return find(query, args(params)).firstResult();
    }

    public Uni<Long> updateContractSignedByOnboardingId(String onboardingId, String contractSignedPath) {
        List<Object> params = new ArrayList<>(List.of(onboardingId, CONTRACT_TYPES));
        String where = tenantScoped(ONBOARDING_AND_TYPES_FILTER, params);
        return update("contractSigned = ?1", contractSignedPath).where(where, args(params));
    }

    public Uni<Long> updateUpdatedAt(String onboardingId, LocalDateTime updatedAt) {
        List<Object> params = new ArrayList<>(List.of(onboardingId, CONTRACT_TYPES));
        String where = tenantScoped(ONBOARDING_AND_TYPES_FILTER, params);
        return update("updatedAt = ?1", updatedAt).where(where, args(params));
    }

    /**
     * Deletes only if the document belongs to the current tenant. Reports {@code false} - the same
     * answer as "no such document" - when it belongs to another one, so a failed delete cannot be
     * used to probe for the existence of another tenant's documents.
     */
    public Uni<Boolean> deleteDocument(String documentId) {
        List<Object> params = new ArrayList<>(List.of(documentId));
        String query = tenantScoped(ID_FILTER, params);
        return delete(query, args(params)).map(deleted -> deleted > 0);
    }

}
