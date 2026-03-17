package it.pagopa.selfcare.document.service;

import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.document.model.dto.request.CreateAttachmentPdfRequest;
import it.pagopa.selfcare.document.model.dto.request.CreateContractPdfRequest;
import it.pagopa.selfcare.document.model.dto.response.CreatePdfResponse;

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
    Uni<CreatePdfResponse> createContractPdf(CreateContractPdfRequest request);

    /**
     * Creates an attachment PDF document from the provided data.
     * The PDF is generated from an HTML template and stored in Azure Blob Storage.
     *
     * @param request the attachment creation request containing all necessary data
     * @return response with storage path and filename
     */
    Uni<CreatePdfResponse> createAttachmentPdf(CreateAttachmentPdfRequest request);
}
