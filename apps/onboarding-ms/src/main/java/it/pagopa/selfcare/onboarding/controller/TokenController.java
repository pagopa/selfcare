package it.pagopa.selfcare.onboarding.controller;

import io.quarkus.security.Authenticated;
import io.quarkus.security.identity.SecurityIdentity;
import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.onboarding.controller.response.ContractSignedReport;
import it.pagopa.selfcare.onboarding.controller.response.TokenResponse;
import it.pagopa.selfcare.onboarding.exception.UpdateNotAllowedException;
import it.pagopa.selfcare.onboarding.mapper.TokenMapper;
import it.pagopa.selfcare.onboarding.service.TokenService;
import jakarta.inject.Inject;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.io.File;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpStatus;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.resteasy.reactive.RestForm;
import org.jboss.resteasy.reactive.RestResponse;
import org.jboss.resteasy.reactive.server.core.ResteasyReactiveRequestContext;
import org.owasp.encoder.Encode;

import static it.pagopa.selfcare.onboarding.util.Utils.retrieveAttachmentFromFormData;

@Authenticated
@Path("/v1/tokens")
@AllArgsConstructor
@Slf4j
public class TokenController {

  @Inject
  SecurityIdentity securityIdentity;

    private final TokenService tokenService;
    private final TokenMapper tokenMapper;

    /**
     * Retrieves the token for a given onboarding
     *
     * @param onboardingId onboarding's unique identifier
     * @return The token
     * * Code: 200, Message: successful operation, DataType: TokenId
     * * Code: 400, Message: Invalid ID supplied, DataType: Problem
     * * Code: 404, Message: Token not found, DataType: Problem
     */

    @Operation(
            summary = "Retrieves the token for a given onboarding",
            description = "Fetches a list of tokens associated with the specified onboarding ID."
    )
    @GET
    public Uni<List<TokenResponse>> getToken(@QueryParam(value = "onboardingId") String onboardingId) {
        return tokenService.getToken(onboardingId)
                .map(tokens -> tokens.stream()
                        .map(tokenMapper::toResponse)
                        .toList());
    }

    @Operation(
            summary = "Retrieve contract not signed for a given onboarding",
            description = "Downloads the unsigned contract file associated with the specified onboarding ID."
    )
    @GET
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    @Path("/{onboardingId}/contract")
    public Uni<RestResponse<File>> getContract(@PathParam(value = "onboardingId") String onboardingId) {
        return tokenService.retrieveContract(onboardingId, Boolean.FALSE);
    }

    @Operation(
            summary = "Retrieve contract signed for a given onboarding",
            description = "Downloads the contract file associated with the specified onboarding ID."
    )
    @GET
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    @Path("/{onboardingId}/contract-signed")
    public Uni<RestResponse<File>> getContractSigned(@PathParam(value = "onboardingId") String onboardingId) {
        return tokenService.retrieveSignedFile(onboardingId);
    }

    @Operation(
            summary = "Retrieve template attachment for a given onboarding and filename",
            description = "Downloads the template attachment file associated with the specified onboarding ID and filename."
    )
    @GET
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    @Path("/{onboardingId}/template-attachment")
    public Uni<RestResponse<File>> getTemplateAttachment(@PathParam(value = "onboardingId") String onboardingId,
                                                         @NotNull @QueryParam(value = "name") String attachmentName) {
        return tokenService.retrieveTemplateAttachment(onboardingId, attachmentName);
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
        return tokenService.retrieveAttachment(onboardingId, attachmentName);
    }

    @Operation(
            summary = "Find an attachment for a given onboarding id and update the contract signed path",
            description = "Find  an attachment for a given onboarding id and update the contract signed path"
    )
    @PUT
    @Tag(name = "internal-v1")
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/contract-signed")
    public Uni<Long> updateContractSigned(@NotNull @QueryParam(value = "onboardingId") String onboardingId,
                                          @NotNull @QueryParam(value = "contractSigned") String contractSigned) {
        return tokenService.updateContractSigned(onboardingId, contractSigned);
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
        return tokenService.reportContractSigned(onboardingId);
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
        return tokenService.uploadAttachment(onboardingId, retrieveAttachmentFromFormData(ctx.getFormData(), file), attachmentName)
                .replaceWith(Response.status(HttpStatus.SC_NO_CONTENT).build())
                .onFailure(UpdateNotAllowedException.class)
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
        return tokenService.existsAttachment(onboardingId, attachmentName)
                .map(exists -> exists
                        ? Response.noContent().build()
                        : Response.status(Response.Status.NOT_FOUND).build()
                );
    }
}
