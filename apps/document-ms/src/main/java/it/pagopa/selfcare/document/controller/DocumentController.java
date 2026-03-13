package it.pagopa.selfcare.document.controller;

import io.quarkus.security.Authenticated;
import io.quarkus.security.identity.SecurityIdentity;
import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.document.model.dto.request.DocumentBuilderRequest;
import it.pagopa.selfcare.document.model.dto.request.OnboardingDocumentRequest;
import it.pagopa.selfcare.document.model.dto.request.UploadAttachmentForm;
import it.pagopa.selfcare.document.model.dto.response.DocumentResponse;
import it.pagopa.selfcare.document.mapper.DocumentMapper;
import it.pagopa.selfcare.document.service.DocumentService;
import it.pagopa.selfcare.document.model.dto.response.ContractSignedReport;
import it.pagopa.selfcare.document.model.entity.Document;
import it.pagopa.selfcare.document.exception.ConflictException;
import jakarta.inject.Inject;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import java.io.File;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.resteasy.reactive.RestResponse;

import java.util.List;
import org.apache.http.HttpStatus;
import org.jboss.resteasy.reactive.server.core.ResteasyReactiveRequestContext;

import static it.pagopa.selfcare.document.util.LogSanitizer.sanitize;
import static it.pagopa.selfcare.document.util.Utils.retrieveAttachmentFromFormData;

@Authenticated
@Path("/v1/documents")
@AllArgsConstructor
@Slf4j
public class DocumentController {

  @Inject
  SecurityIdentity securityIdentity;

  private final DocumentService documentService;
  private final DocumentMapper documentMapper;

    /**
     * Retrieves the document for a given onboarding
     *
     * @param onboardingId onboarding's unique identifier
     * @return The document
     * * Code: 200, Message: successful operation, DataType: DocumentId
     * * Code: 400, Message: Invalid ID supplied, DataType: Problem
     * * Code: 404, Message: Document not found, DataType: Problem
     */

    @Operation(
            summary = "Retrieves the documents for a given onboarding",
            description = "Fetches a list of documents associated with the specified onboarding ID."
    )
    @GET
    @Path("/onboarding/{onboardingId}")
    public Uni<List<DocumentResponse>> getDocumentByOnboardingId(@PathParam(value = "onboardingId") String onboardingId) {
        return documentService.getDocumentsByOnboardingId(onboardingId)
                .map(documents -> documents.stream()
                        .map(documentMapper::toResponse)
                        .toList());
    }

    @Operation(
            summary = "Retrieves the document for a given document ID",
            description = "Fetches a document associated with the specified document ID."
    )
    @GET
    @Path("/{id}")
    public Uni<DocumentResponse> getDocumentById(@PathParam(value = "id") String id) {
        return documentService.getDocumentById(id)
                .map(documentMapper::toResponse);
    }

    @Operation(
            summary = "Retrieve contract not signed for a given onboarding",
            description = "Downloads the unsigned contract file associated with the specified onboarding ID."
    )
    @GET
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    @Path("/{onboardingId}/contract")
    public Uni<RestResponse<File>> getContract(@PathParam(value = "onboardingId") String onboardingId) {
        return documentService.retrieveContract(onboardingId, Boolean.FALSE);
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
        return documentService.retrieveTemplateAttachment(onboardingId, templatePath, name, institutionDescription, productId);
    }

    @Operation(
            summary = "Update contract signed path for a given onboarding",
            description = "Update contract signed path for a given onboarding"
    )
  @PUT
  @Tag(name = "internal-v1")
  @Produces(MediaType.APPLICATION_JSON)
  @Path("/{onboardingId}/contract-signed")
  public Uni<Response> updateContractSigned(@NotNull @PathParam(value = "onboardingId") String onboardingId,
                                            @NotNull @QueryParam(value = "contractSigned") String contractSigned) {
        return documentService.updateContractSigned(onboardingId, contractSigned)
                .map(updatedCount -> {
                    if (updatedCount > 0) {
                        return Response.noContent().build();
                    } else {
                        return Response.status(Response.Status.NOT_FOUND)
                                .entity("Attachment not found for onboardingId: " + onboardingId)
                                .build();
                    }
                });
    }

    @Operation(
            summary = "Retrieve contract signed for a given onboarding",
            description = "Downloads the contract file associated with the specified onboarding ID."
    )
    @GET
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    @Path("/{id}/contract-signed")
    public Uni<RestResponse<File>> getContractSigned(@PathParam(value = "id") String id) {
        return documentService.retrieveSignedFile(id);
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
    return documentService.retrieveAttachment(onboardingId, attachmentName);

  }

  @Operation(
          summary = "Retrieve attachment names for a given onboarding",
          description = "Returns the list of attachment names associated with the specified onboarding ID."
  )
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Path("/{onboardingId}/attachment-list")
  public Uni<List<String>> getAttachments(@PathParam(value = "onboardingId") String onboardingId) {
    log.info("Getting attachment names for onboardingId: {}", sanitize(onboardingId));
    return documentService.getAttachments(onboardingId);
  }

  @Operation(
          summary = "Check if contract signed is a CADES file",
          description = "Check if contract signed is a CADES file even if is not .p7m"
  )
  @GET
  @Tag(name = "internal-v1")
  @Produces(MediaType.APPLICATION_JSON)
  @Path("/contract-report")
  public Uni<ContractSignedReport> reportContractSigned(@NotNull @QueryParam(value = "onboardingId") String onboardingId) {
    return documentService.reportContractSigned(onboardingId);
  }

  @Operation(
          summary = "Upload attachment by verifying and signing document, then save into storage.",
          description = "Perform upload  of the file passed in input verifying digest e put company signature"
  )
  @POST
  @Path("/attachment")
  @Consumes(MediaType.MULTIPART_FORM_DATA)
  public Uni<Response> uploadAttachment(@Valid @BeanParam UploadAttachmentForm form, @Context ResteasyReactiveRequestContext ctx) {
      return documentService.uploadAttachment(form.getRequest(), retrieveAttachmentFromFormData(ctx.getFormData(), form.getFile()))
            .replaceWith(Response.status(HttpStatus.SC_NO_CONTENT).build())
            .onFailure(ConflictException.class)
            .recoverWithItem(err -> Response.status(HttpStatus.SC_CONFLICT).entity(err.getMessage()).build());
  }

  @Operation(
          summary = "Verify attachment availability",
          description = "Verifies the availability of the specified attachment in the storage system. "
                  + "A successful check returns HTTP 204 (No Content), while a missing attachment results in HTTP 404 (Not Found)."
  )
  @HEAD
  @Path("/{onboardingId}/attachment/status")
  public Uni<Response> headAttachment(
          @PathParam("onboardingId") String onboardingId,
          @NotNull @QueryParam("name") String attachmentName
  ) {
    log.info("Head attachment for {} - {}", sanitize(onboardingId), sanitize(attachmentName));
    return documentService.existsAttachment(onboardingId, attachmentName)
            .map(exists -> exists
                    ? Response.noContent().build()
                    : Response.status(Response.Status.NOT_FOUND).build()
            );
  }

  @Operation(
          summary = "Update document contract files",
          description = "Updates the contractSigned and contractFilename paths for a document after contract deletion/move operations."
  )
  @PUT
  @Path("/contract-files")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Uni<Response> updateDocumentContractFiles(@Valid Document request) {
    log.info("Updating document contract files for documentId: {}", sanitize(request.getId()));
    return documentService.updateDocumentContractFiles(
                    request)
            .map(updatedCount -> {
              if (updatedCount > 0) {
                return Response.noContent().build();
              } else {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity("Document not found with id: " + request.getId())
                        .build();
              }
            });
  }

  @Operation(summary = "Save document (contract or attachment)",
          description = "Persists a document associated with an onboarding. " +
                  "For INSTITUTION/USER token types, saves a contract. " +
                  "For ATTACHMENT token type, saves an attachment.")
  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Uni<Response> saveDocument(@Valid DocumentBuilderRequest request) {
    log.info("Saving document for onboardingId: {}, documentType: {}",
            sanitize(request.getOnboardingId()), request.getDocumentType());
    return documentService.saveDocument(request)
            .onItem().transform(response -> Response.status(Response.Status.CREATED).entity(response).build());
  }

    @Operation(summary = "Persist document for import",
            description = "Persists a document during the import process, associating it with the relevant onboarding and product.")
  @POST
  @Path("/import")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Uni<Response> persistDocumentForImport(@Valid OnboardingDocumentRequest request) {
    log.info("Persisting token for import, onboardingId: {}", sanitize(request.getOnboardingId()));
    return documentService.persistDocumentForImport(request)
            .onItem().transform(token -> Response.status(Response.Status.CREATED).entity(token).build());
  }

  @Operation(
          summary = "Update document updatedAt field",
          description = "Sets the updatedAt timestamp of the document associated with the given onboarding ID to the current date and time."
  )
  @PUT
  @Path("/{onboardingId}/updated-at")
  @Produces(MediaType.APPLICATION_JSON)
  public Uni<Response> updateDocumentUpdatedAt(@NotNull @PathParam(value = "onboardingId") String onboardingId) {
    log.info("Updating document 'updatedAt' for onboardingId: {}", sanitize(onboardingId));
    return documentService.updateDocumentUpdatedAt(onboardingId)
            .replaceWith(Response.noContent().build());
  }
}
