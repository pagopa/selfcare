package it.pagopa.selfcare.document.controller;

import io.quarkus.security.Authenticated;
import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.document.model.dto.request.CreateAttachmentPdfRequest;
import it.pagopa.selfcare.document.model.dto.request.CreateContractPdfRequest;
import it.pagopa.selfcare.document.model.dto.response.CreatePdfResponse;
import it.pagopa.selfcare.document.service.DocumentContentService;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;


import static it.pagopa.selfcare.document.util.LogSanitizer.sanitize;

/**
 * Controller for creating PDF document content (contracts and attachments).
 * This controller receives complete data from the calling service (e.g., onboarding-ms)
 * and generates the PDF documents without making external calls.
 */
@Authenticated
@Path("/v1/document-content")
@AllArgsConstructor
@Slf4j
@Tag(name = "Document Content Controller", description = "Endpoints for creating PDF document content (contracts and attachments)")
public class DocumentContentController {

    private final DocumentContentService documentContentService;

    /**
     * Creates a contract PDF document from the provided data.
     * The caller must provide all necessary information (institution, manager, delegates, etc.)
     * so that this service can generate the PDF without external calls.
     *
     * @param request the contract creation request containing all necessary data
     * @return response with storage path and filename
     */
    @Operation(
            summary = "Create contract PDF",
            description = "Generates a contract PDF document from the provided data, signs it with PagoPA signature, and stores it in Azure Blob Storage."
    )
    @APIResponses({
            @APIResponse(
                    responseCode = "200",
                    description = "Contract PDF created successfully",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON,
                            schema = @Schema(implementation = CreatePdfResponse.class)
                    )
            ),
            @APIResponse(responseCode = "400", description = "Invalid request data"),
            @APIResponse(responseCode = "500", description = "Internal server error during PDF generation")
    })
    @POST
    @Path("/contract")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<CreatePdfResponse> createContractPdf(@Valid CreateContractPdfRequest request) {
        log.info("Creating contract PDF for onboardingId: {}, productId: {}",
                sanitize(request.getOnboardingId()), sanitize(request.getProductId()));
        return documentContentService.createContractPdf(request);
    }

    /**
     * Creates an attachment PDF document from the provided data.
     * The caller must provide all necessary information (institution, manager, GPU data, etc.)
     * so that this service can generate the PDF without external calls.
     *
     * @param request the attachment creation request containing all necessary data
     * @return response with storage path and filename
     */
    @Operation(
            summary = "Create attachment PDF",
            description = "Generates an attachment PDF document from the provided data and stores it in Azure Blob Storage."
    )
    @APIResponses({
            @APIResponse(
                    responseCode = "200",
                    description = "Attachment PDF created successfully",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON,
                            schema = @Schema(implementation = CreatePdfResponse.class)
                    )
            ),
            @APIResponse(responseCode = "400", description = "Invalid request data"),
            @APIResponse(responseCode = "500", description = "Internal server error during PDF generation")
    })
    @POST
    @Path("/attachment")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<CreatePdfResponse> createAttachmentPdf(@Valid CreateAttachmentPdfRequest request) {
        log.info("Creating attachment PDF for onboardingId: {}, attachmentName: {}",
                sanitize(request.getOnboardingId()), sanitize(request.getAttachmentName()));
        return documentContentService.createAttachmentPdf(request);
    }
}
