package it.pagopa.selfcare.document.controller;

import io.quarkus.security.Authenticated;
import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.document.exception.UpdateNotAllowedException;
import it.pagopa.selfcare.document.model.dto.request.CreateAttachmentPdfRequest;
import it.pagopa.selfcare.document.model.dto.request.CreateContractPdfRequest;
import it.pagopa.selfcare.document.model.dto.request.UploadAttachmentForm;
import it.pagopa.selfcare.document.model.dto.request.UploadVisuraRequest;
import it.pagopa.selfcare.document.model.dto.response.CreatePdfResponse;
import it.pagopa.selfcare.document.service.DocumentContentService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpStatus;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.resteasy.reactive.RestResponse;
import org.jboss.resteasy.reactive.server.core.ResteasyReactiveRequestContext;


import java.io.File;

import static it.pagopa.selfcare.document.util.LogSanitizer.sanitize;
import static it.pagopa.selfcare.document.util.Utils.retrieveAttachmentFromFormData;

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

    @Operation(
            summary = "Retrieve contract signed for a given onboarding",
            description = "Downloads the contract file associated with the specified onboarding ID."
    )
    @GET
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    @Path("/{id}/contract-signed")
    public Uni<RestResponse<File>> getContractSigned(@PathParam(value = "id") String id) {
        return documentContentService.retrieveSignedFile(id);
    }

    @Operation(
            summary = "Retrieve contract not signed for a given onboarding",
            description = "Downloads the unsigned contract file associated with the specified onboarding ID."
    )
    @GET
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    @Path("/{onboardingId}/contract")
    public Uni<RestResponse<File>> getContract(@PathParam(value = "onboardingId") String onboardingId) {
        return documentContentService.retrieveContract(onboardingId, Boolean.FALSE);
    }

    @Operation(
            summary = "Retrieve template attachment for a given onboarding and filename and template path",
            description = "Downloads the template attachment file associated with the specified onboarding ID and filename and template path."
    )
    @GET
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    @Path("/{onboardingId}/template-attachment")
    public Uni<RestResponse<File>> getTemplateAttachment(
            @PathParam(value = "onboardingId") String onboardingId,
            @NotNull @QueryParam("templatePath") String templatePath,
            @NotNull @QueryParam("name") String name,
            @NotNull @QueryParam("institutionDescription") String institutionDescription,
            @NotNull @QueryParam("productId") String productId) {
        return documentContentService.retrieveTemplateAttachment(onboardingId, templatePath, name, institutionDescription, productId);
    }

    @Operation(
            summary = "Retrieve attachment for a given onboarding and filename",
            description = "Downloads the attachment file associated with the specified onboarding ID and filename."
    )
    @GET
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    @Path("/{onboardingId}/attachment")
    public Uni<RestResponse<File>> getAttachment(@PathParam(value = "onboardingId") String onboardingId,
                                                 @NotNull @QueryParam(value = "name") String attachmentName) {
        return documentContentService.retrieveAttachment(onboardingId, attachmentName);

    }

    @Operation(
            summary = "Upload attachment by verifying and signing document, then save into storage.",
            description = "Perform upload  of the file passed in input verifying digest e put company signature"
    )
    @POST
    @Path("/upload-attachment")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Uni<Response> uploadAttachment(@Valid @BeanParam UploadAttachmentForm form, @Context ResteasyReactiveRequestContext ctx) {
        return documentContentService.uploadAttachment(form.getRequest(), retrieveAttachmentFromFormData(ctx.getFormData(), form.getFile()))
                .replaceWith(Response.status(HttpStatus.SC_NO_CONTENT).build())
                .onFailure(UpdateNotAllowedException.class)
                .recoverWithItem(err -> Response.status(HttpStatus.SC_CONFLICT).entity(err.getMessage()).build());
    }

    @Operation(
            summary = "Store the Visura in Azure Blob Storage",
            description = "Receives the Visura data (filename and content) and stores it in Azure Blob Storage."
    )
    @POST
    @Path("/visura")
    @Consumes(MediaType.APPLICATION_JSON)
    public Uni<Response> saveVisuraForMerchant(@Valid UploadVisuraRequest request) {
        return documentContentService.saveVisuraForMerchant(request)
                .replaceWith(Response.status(HttpStatus.SC_NO_CONTENT).build())
                .onFailure()
                .recoverWithItem(err -> Response.status(HttpStatus.SC_INTERNAL_SERVER_ERROR).entity(err.getMessage()).build());
    }
}
