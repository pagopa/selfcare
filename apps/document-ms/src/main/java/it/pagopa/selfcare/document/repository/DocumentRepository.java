package it.pagopa.selfcare.document.repository;

import io.quarkus.mongodb.panache.reactive.ReactivePanacheMongoRepositoryBase;
import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.document.model.entity.Document;
import jakarta.enterprise.context.ApplicationScoped;
import org.bson.types.ObjectId;

import java.time.LocalDateTime;
import java.util.List;

import static it.pagopa.selfcare.onboarding.common.TokenType.*;

@ApplicationScoped
public class DocumentRepository implements ReactivePanacheMongoRepositoryBase<Document, ObjectId> {

    private static final String ONBOARDING_ID = "onboardingId";
    private static final List<String> CONTRACT_TYPES = List.of(INSTITUTION.name(), USER.name());

    public Uni<Long> updateContractFiles(String onboardingId, String contractSigned, String contractFilename) {
        return update("contractSigned = ?1 and contractFilename = ?2 and updatedAt = ?3",
                contractSigned, contractFilename, LocalDateTime.now())
                .where("onboardingId = ?1 and type in ?2", onboardingId, CONTRACT_TYPES);
    }

    public Uni<Document> findAttachment(String onboardingId, String type, String name) {
        return find("onboardingId = ?1 and type = ?2 and name = ?3", onboardingId, type, name)
                .firstResult();
    }

    public Uni<List<Document>> findAttachments(String onboardingId) {
        return find("referenceOnboardingId = ?1 and type = ?2", onboardingId, ATTACHMENT.name()).list();
    }

    public Uni<Document> findByOnboardingId(String onboardingId) {
        return find("onboardingId = ?1 and type in ?2", onboardingId, CONTRACT_TYPES)
                .firstResult();
    }

    public Uni<List<Document>> findAllByOnboardingId(String onboardingId) {
        return find(ONBOARDING_ID, onboardingId).list();
    }

    public Uni<Long> updateContractSignedByOnboardingId(String onboardingId, String contractSignedPath) {
        return update("contractSigned = ?1", contractSignedPath)
                .where("onboardingId = ?1 and type in ?2", onboardingId, CONTRACT_TYPES);
    }

    public Uni<Long> updateUpdatedAt(String onboardingId, LocalDateTime updatedAt) {
        return update("updatedAt = ?1", updatedAt)
                .where("onboardingId = ?1 and type in ?2", onboardingId, CONTRACT_TYPES);
    }

}
