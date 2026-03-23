package it.pagopa.selfcare.document.service;

import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.document.model.dto.request.DocumentBuilderRequest;
import it.pagopa.selfcare.document.model.dto.request.OnboardingDocumentRequest;
import it.pagopa.selfcare.document.model.dto.response.ContractSignedReport;
import it.pagopa.selfcare.document.model.entity.Document;

import java.util.List;

public interface DocumentService {

    Uni<List<Document>> getDocumentsByOnboardingId(String onboardingId);

    Uni<Document> getDocumentInstitutionByOnboardingId(String onboardingId);

    Uni<Document> getDocumentById(String id);

    Uni<Long> updateContractSigned(String onboardingId, String documentSignedPath);

    Uni<List<String>> getAttachments(String onboardingId);

    Uni<ContractSignedReport> reportContractSigned(String onboardingId);

    Uni<Boolean> existsAttachment(String onboardingId, String attachmentName);

    Uni<Void> updateDocumentUpdatedAt(String onboardingId);

    /**
     * Updates the contract file paths for a document after contract deletion/move operations.
     *
     * @param document the document containing the updated file paths
     * @return the number of updated records
     */
    Uni<Long> updateDocumentContractFiles(Document document);

    /**
     * Saves a document (contract or attachment) based on the TokenType in the request.
     * For INSTITUTION/USER types, saves a contract document.
     * For ATTACHMENT type, saves an attachment document.
     *
     * @param request the document save request
     * @return the save response containing documentId, checksum and alreadyExists flag
     */
    Uni<Document> saveDocument(DocumentBuilderRequest request);

    Uni<Document> persistDocumentForImport(OnboardingDocumentRequest request);

    Uni<Document> handleContractDocument(DocumentBuilderRequest request);

}
