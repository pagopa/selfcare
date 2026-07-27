package it.pagopa.selfcare.document.service;

import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.document.model.FormItem;
import it.pagopa.selfcare.document.model.dto.request.*;
import it.pagopa.selfcare.document.model.dto.response.CreatePdfResponse;
import org.jboss.resteasy.reactive.RestResponse;

import java.io.File;
import java.io.InputStream;

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

    Uni<Void> uploadUserAttachment(UserAttachmentRequest request, FormItem file);

    Uni<Void> saveVisuraForMerchant(UploadVisuraRequest uploadVisuraRequest);

    Uni<String> deleteContract(String onboardingId);

    /**
     * Soft-deletes every user-uploaded attachment associated with the given onboarding.
     * Each blob is moved from the contracts path (e.g. {@code parties/docs/…}) to the
     * configured delete path (e.g. {@code parties/deleted/…}) on the USER storage account,
     * and the corresponding {@code attachmentPath} on the Document collection is updated
     * to keep DB and Blob Storage aligned. Retention is enforced by an Azure lifecycle
     * management policy on the delete-path prefix.
     *
     * @param onboardingId onboarding whose user attachments must be soft-deleted
     * @return an outcome message (never fails the caller: failures are logged and tracked)
     */
    Uni<String> deleteUserAttachments(String onboardingId);

    Uni<Void> uploadAggregatesCsv(UploadAggregateCsvRequest request);

  Uni<String> uploadSignedContract(
      String onboardingId,
      DocumentBuilderRequest request,
      boolean skipSignatureVerification,
      InputStream file,
      String fileName,
      boolean skipSignerIdentityCheck,
      int signingStep);

    Uni<RestResponse<File>> retrieveAggregatesCsv(String onboardingId, String productId);
}
