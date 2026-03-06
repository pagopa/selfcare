package it.pagopa.selfcare.document.controller;

import io.quarkus.security.Authenticated;
import io.quarkus.security.identity.SecurityIdentity;
import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.document.controller.response.ContractSignedReport;
import it.pagopa.selfcare.document.entity.Document;
import it.pagopa.selfcare.document.exception.ConflictException;
import it.pagopa.selfcare.document.service.DocumentService;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.io.File;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpStatus;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.resteasy.reactive.RestForm;
import org.jboss.resteasy.reactive.RestResponse;
import org.jboss.resteasy.reactive.server.core.ResteasyReactiveRequestContext;
import org.owasp.encoder.Encode;

import static it.pagopa.selfcare.document.util.Utils.retrieveAttachmentFromFormData;

@Authenticated
@Path("/v1/documents")
@AllArgsConstructor
@Slf4j
public class DocumentController {

  @Inject
  SecurityIdentity securityIdentity;

  private final DocumentService documentService;

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
  @Path("/{onboardingId}/attachment")
  @Consumes(MediaType.MULTIPART_FORM_DATA)
  public Uni<Response> uploadAttachment(@PathParam(value = "onboardingId") String onboardingId,
                                        @NotNull @RestForm("file") File file, @Context ResteasyReactiveRequestContext ctx,
                                        @NotNull @QueryParam(value = "name") String attachmentName) {
    return documentService.uploadAttachment(onboardingId, retrieveAttachmentFromFormData(ctx.getFormData(), file), attachmentName)
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
    log.info("Head attachment for {} - {}", Encode.forJava(onboardingId), Encode.forJava(attachmentName));
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
    log.info("Updating document contract files for documentId: {}", Encode.forJava(request.getId()));
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
}
