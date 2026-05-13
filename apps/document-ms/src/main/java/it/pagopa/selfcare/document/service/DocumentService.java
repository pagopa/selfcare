package it.pagopa.selfcare.document.service;

import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.document.model.dto.request.DocumentBuilderRequest;
import it.pagopa.selfcare.document.model.dto.request.OnboardingDocumentRequest;
import it.pagopa.selfcare.document.model.dto.response.ContractSignedReport;
import it.pagopa.selfcare.document.model.entity.Document;

import java.util.List;

public interface DocumentService {

    Uni<Document> getDocumentById(String id);

    Uni<Document> getDocumentByOnboardingId(String onboardingId);

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
     * Updates contract files and signingStep for a specific document by its ID.
     * Used during uploadSignedContract to avoid updating all documents with the same onboardingId.
     */
    Uni<Long> updateDocumentContractFilesById(Document document);

    /**
     * Saves a document (contract or attachment) based on the DocumentType in the request.
     * For INSTITUTION/USER types, saves a contract document.
     * For ATTACHMENT type, saves an attachment document.
     *
     * @param request the document save request
     * @return the save response containing documentId, checksum and alreadyExists flag
     */
    Uni<Document> saveDocument(DocumentBuilderRequest request);

    Uni<Document> persistDocumentForImport(OnboardingDocumentRequest request);

    /**
     * Handles a contract document, reusing the existing record if unsigned,
     * or creating a new one for subsequent signing steps.
     *
     * @param request the document builder request
     * @return the document (existing or newly created)
     */
    Uni<Document> handleContractDocument(DocumentBuilderRequest request);

    Uni<Boolean> deleteDocumentById(String documentId);

}
