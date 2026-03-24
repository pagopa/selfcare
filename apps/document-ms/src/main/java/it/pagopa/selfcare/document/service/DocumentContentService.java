package it.pagopa.selfcare.document.service;

import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.document.model.FormItem;
import it.pagopa.selfcare.document.model.dto.request.*;
import it.pagopa.selfcare.document.model.dto.response.CreatePdfResponse;
import org.jboss.resteasy.reactive.RestResponse;
import org.jboss.resteasy.reactive.multipart.FileUpload;

import java.io.File;

/**
 * Service for creating PDF documents (contracts and attachments).
 */
public interface DocumentContentService {

    /**
     * Creates a contract PDF document from the provided data.
     * The PDF is generated from an HTML template, signed with PagoPA signature,
     * and stored in Azure Blob Storage.
     *
     * @param request the contract creation request containing all necessary data
     * @return response with storage path and filename
     */
    Uni<CreatePdfResponse> createContractPdf(ContractPdfRequest request);

    /**
     * Creates an attachment PDF document from the provided data.
     * The PDF is generated from an HTML template and stored in Azure Blob Storage.
     *
     * @param request the attachment creation request containing all necessary data
     * @return response with storage path and filename
     */
    Uni<CreatePdfResponse> createAttachmentPdf(AttachmentPdfRequest request);

    Uni<RestResponse<File>> retrieveSignedFile(String id);

    Uni<RestResponse<File>> retrieveContract(String onboardingId, boolean isSigned);

    Uni<RestResponse<File>> retrieveTemplateAttachment(
            String onboardingId,
            String templatePath,
            String attachmentName,
            String institutionDescription,
            String productId);

    Uni<RestResponse<File>> retrieveAttachment(String onboardingId, String attachmentName);

    Uni<Void> uploadAttachment(DocumentBuilderRequest request, FormItem file);

    Uni<Void> saveVisuraForMerchant(UploadVisuraRequest uploadVisuraRequest);

    Uni<String> deleteContract(String onboardingId);

    Uni<Void> uploadAggregatesCsv(UploadAggregateCsvRequest request);

    Uni<String> uploadSignedContract(String onboardingId, DocumentBuilderRequest request, boolean skipSignatureVerification, FileUpload fileUpload);
}
