package it.pagopa.selfcare.document.service;

import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.document.controller.request.DocumentBuilderRequest;
import it.pagopa.selfcare.document.controller.request.OnboardingDocumentRequest;
import it.pagopa.selfcare.document.controller.response.ContractSignedReport;
import it.pagopa.selfcare.document.controller.response.DocumentBuilderResponse;
import it.pagopa.selfcare.document.entity.Document;
import java.io.File;
import java.util.List;

import it.pagopa.selfcare.document.model.FormItem;
import org.jboss.resteasy.reactive.RestResponse;

public interface DocumentService {

    Uni<List<Document>> getDocumentsByOnboardingId(String onboardingId);

    Uni<Document> getDocumentById(String id);

    Uni<RestResponse<File>> retrieveContract(String onboardingId, boolean isSigned);

    Uni<RestResponse<File>> retrieveSignedFile(String id);

    Uni<RestResponse<File>> retrieveTemplateAttachment(String onboardingId, String templatePath, String attachmentName);

    Uni<RestResponse<File>> retrieveAttachment(String onboardingId, String attachmentName);

    Uni<Void> uploadAttachment(DocumentBuilderRequest request, FormItem file);

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
    Uni<DocumentBuilderResponse> saveDocument(DocumentBuilderRequest request);

    Uni<Document> persistDocumentForImport(OnboardingDocumentRequest request);

}
