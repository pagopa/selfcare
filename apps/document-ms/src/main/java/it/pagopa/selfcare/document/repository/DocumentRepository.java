package it.pagopa.selfcare.document.repository;

import io.quarkus.mongodb.panache.reactive.ReactivePanacheMongoRepositoryBase;
import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.document.entity.Document;
import jakarta.enterprise.context.ApplicationScoped;
import org.bson.types.ObjectId;

import java.time.LocalDateTime;
import java.util.List;

@ApplicationScoped
public class DocumentRepository implements ReactivePanacheMongoRepositoryBase<Document, ObjectId> {

    private static final String ONBOARDING_ID = "onboardingId";

    public Uni<Long> updateContractFiles(String documentId, String contractSigned, String contractFilename) {
        return update("contractSigned = ?1 and contractFilename = ?2 and updatedAt = ?3",
                contractSigned, contractFilename, LocalDateTime.now())
                .where("_id", documentId);
    }

    public Uni<Document> findAttachment(String onboardingId, String type, String name) {
        return find("onboardingId = ?1 and type = ?2 and name = ?3", onboardingId, type, name)
                .firstResult();
    }

    public Uni<Document> findByOnboardingId(String onboardingId) {
        return find(ONBOARDING_ID, onboardingId).firstResult();
    }

    public Uni<List<Document>> findAllByOnboardingId(String onboardingId) {
        return find(ONBOARDING_ID, onboardingId).list();
    }

}
