package it.pagopa.selfcare.onboarding.controller;

import io.quarkus.security.Authenticated;
import io.quarkus.security.identity.SecurityIdentity;
import io.smallrye.common.annotation.Blocking;
import io.smallrye.mutiny.Uni;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.enums.ParameterIn;
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import it.pagopa.selfcare.onboarding.client.model.AvailableDocuments;
import it.pagopa.selfcare.onboarding.client.model.BinaryData;
import it.pagopa.selfcare.onboarding.client.model.OnboardingData;
import it.pagopa.selfcare.onboarding.client.model.UploadedFile;
import it.pagopa.selfcare.onboarding.model.dto.request.DownloadDocumentType;
import it.pagopa.selfcare.onboarding.model.dto.request.ReasonForRejectDto;
import it.pagopa.selfcare.onboarding.model.dto.response.AvailableDocumentsResource;
import it.pagopa.selfcare.onboarding.model.dto.response.OnboardingRequestResource;
import it.pagopa.selfcare.onboarding.exception.AccessDeniedException;
import it.pagopa.selfcare.onboarding.exception.InvalidRequestException;
import it.pagopa.selfcare.onboarding.mapper.OnboardingMapper;
import it.pagopa.selfcare.onboarding.model.OnboardingVerify;
import it.pagopa.selfcare.onboarding.security.AuthorizationService;
import it.pagopa.selfcare.onboarding.service.TokenService;
import it.pagopa.selfcare.onboarding.util.FileValidationUtils;
import it.pagopa.selfcare.onboarding.util.LogUtils;
import it.pagopa.selfcare.onboarding.util.PermissionConstants;
import it.pagopa.selfcare.onboarding.util.RequestParams;
import it.pagopa.selfcare.onboarding.util.SecurityIdentityUtils;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HEAD;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.core.Response;
import java.io.IOException;
import java.nio.file.Files;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jboss.resteasy.reactive.RestForm;
import org.jboss.resteasy.reactive.multipart.FileUpload;
import org.owasp.encoder.Encode;

@Slf4j
@ApplicationScoped
@Authenticated
@Path("/v2/tokens")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "tokens")
@RequiredArgsConstructor
public class TokenV2Controller {

    private static final String ACCESS_CONTROL_EXPOSE_HEADERS = "Access-Control-Expose-Headers";
    private static final String TENANT_HEADER = "X-Tenant-Id";
    private static final String SANITIZIER = "[^a-zA-Z0-9-_]";
    private static final String FORBIDDEN_DOCUMENTS = "Forbidden - user does not have permission to view account documents";

    private final TokenService tokenService;
    private final OnboardingMapper onboardingResourceMapper;

    @Inject
    SecurityIdentity securityIdentity;

    @Inject
    AuthorizationService authorizationService;

    @Context
    UriInfo uriInfo;

    @POST
    @Path("/{onboardingId}/complete")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @RequestBody(required = false, content = @Content(mediaType = MediaType.MULTIPART_FORM_DATA,
            schema = @Schema(requiredProperties = "contract")))
    @APIResponse(responseCode = "204", description = "No Content")
    @Operation(description = "${openapi.tokens.complete}", summary = "${openapi.tokens.complete}", operationId = "completeUsingPOST")
    public Response complete(@Parameter(description = "${openapi.tokens.onboardingId}")
                             @PathParam("onboardingId") String onboardingId,
                             @RestForm("contract") FileUpload contract) {
        log.trace("complete Token start");
        UploadedFile uploadedFile = toUploadedFile("contract", contract);
        FileValidationUtils.validatePdfOrP7m(uploadedFile);
        String sanitizedFileName = Encode.forJava(uploadedFile.fileName());
        String sanitizedOnboardingId = onboardingId.replaceAll(SANITIZIER, "");
        log.debug(LogUtils.CONFIDENTIAL_MARKER, "complete Token tokenId = {}, contract = {}", sanitizedOnboardingId, sanitizedFileName);
        tokenService.completeTokenV2(onboardingId, uploadedFile);
        return Response.noContent().build();
    }

    @POST
    @Path("/{onboardingId}/complete-onboarding-users")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @RequestBody(required = false, content = @Content(mediaType = MediaType.MULTIPART_FORM_DATA,
            schema = @Schema(requiredProperties = "contract")))
    @APIResponse(responseCode = "204", description = "No Content")
    @Operation(description = "${openapi.tokens.completeOnboardingUsers}", summary = "${openapi.tokens.completeOnboardingUsers}",
            operationId = "completeOnboardingUsersUsingPOST")
    public Response completeOnboardingUsers(@Parameter(description = "${openapi.tokens.onboardingId}")
                                            @PathParam("onboardingId") String onboardingId,
                                            @RestForm("contract") FileUpload contract) {
        log.trace("complete Onboarding Users start");
        UploadedFile uploadedFile = toUploadedFile("contract", contract);
        FileValidationUtils.validatePdfOrP7m(uploadedFile);
        String sanitizedFileName = Encode.forJava(uploadedFile.fileName());
        String sanitizedOnboardingId = onboardingId.replaceAll(SANITIZIER, "");
        log.debug(LogUtils.CONFIDENTIAL_MARKER, "complete Onboarding Users tokenId = {}, contract = {}", sanitizedOnboardingId, sanitizedFileName);
        tokenService.completeOnboardingUsers(onboardingId, uploadedFile);
        return Response.noContent().build();
    }

    @POST
    @Path("/{onboardingId}/verify")
    @Operation(description = "${openapi.tokens.verify}",
            summary = "${openapi.tokens.verify}", operationId = "verifyOnboardingUsingPOST")
    public Uni<OnboardingVerify> verifyOnboarding(@Parameter(description = "${openapi.tokens.onboardingId}") @PathParam("onboardingId") String onboardingId) {
        String sanitizedOnboardingId = onboardingId.replace("\n", "").replace("\r", "");
        log.debug("Verify token identified with {}", sanitizedOnboardingId);
        return tokenService.verifyOnboarding(sanitizedOnboardingId)
                .map(onboardingResourceMapper::toOnboardingVerify)
                .invoke(result -> {
                    log.debug("Verify token identified result = {}", result);
                    log.trace("Verify token identified end");
                });
    }

    @GET
    @Path("/{onboardingId}")
    @Operation(summary = "${openapi.tokens.retrieveOnboardingRequest}",
            description = "${openapi.tokens.retrieveOnboardingRequest}", operationId = "retrieveOnboardingRequestUsingGET")
    public OnboardingRequestResource retrieveOnboardingRequest(@Parameter(description = "${openapi.tokens.onboardingId}")
                                                               @PathParam("onboardingId")
                                                               String onboardingId) {
        checkPermission(onboardingId, PermissionConstants.SELC_VIEW_ACCOUNT_PAGE);
        log.trace("retrieveOnboardingRequest start");
        String sanitizedOnboardingId = onboardingId.replace("\n", "").replace("\r", "");
        log.debug("retrieveOnboardingRequest onboardingId = {}", sanitizedOnboardingId);
        final OnboardingData onboardingData = tokenService.getOnboardingWithUserInfo(sanitizedOnboardingId);
        OnboardingRequestResource result = onboardingResourceMapper.toOnboardingRequestResource(onboardingData);
        log.debug("retrieveOnboardingRequest result = {}", result);
        log.trace("retrieveOnboardingRequest end");
        return result;
    }

    @POST
    @Path("/{onboardingId}/approve")
    @Operation(description = "${openapi.tokens.approveOnboardingRequest}",
            summary = "${openapi.tokens.approveOnboardingRequest}", operationId = "approveOnboardingUsingPOST")
    public Response approveOnboarding(@Parameter(description = "${openapi.tokens.onboardingId}")
                                      @PathParam("onboardingId") String onboardingId) {
        checkPermission(onboardingId, PermissionConstants.SELC_MANAGE_ACCOUNT_PAGE);
        log.debug("approve onboarding identified with {}", LogUtils.sanitize(onboardingId));
        tokenService.approveOnboarding(onboardingId, SecurityIdentityUtils.getUid(securityIdentity));
        return Response.ok().build();
    }

    @POST
    @Path("/{onboardingId}/reject")
    @Operation(summary = "Service to reject a specific onboarding request",
            description = "Service to reject a specific onboarding request", operationId = "rejectOnboardingUsingPOST")
    public Response rejectOnboarding(@Parameter(description = "${openapi.tokens.onboardingId}")
                                     @PathParam("onboardingId") String onboardingId,
                                     ReasonForRejectDto reasonForRejectDto) {
        RequestParams.requiredBody(reasonForRejectDto);
        checkPermission(onboardingId, PermissionConstants.SELC_MANAGE_ACCOUNT_PAGE);
        log.debug("reject onboarding identified with {}", LogUtils.sanitize(onboardingId));
        tokenService.rejectOnboarding(onboardingId, reasonForRejectDto.getReason(), SecurityIdentityUtils.getUid(securityIdentity));
        return Response.ok().build();
    }

    @DELETE
    @Path("/{onboardingId}/complete")
    @APIResponse(responseCode = "204", description = "No Content")
    @Operation(summary = "${openapi.tokens.complete}",
            description = "${openapi.tokens.complete}", operationId = "deleteUsingDELETE")
    public Response deleteOnboarding(@Parameter(description = "${openapi.tokens.tokenId}")
                                     @PathParam("onboardingId") String onboardingId) {
        log.trace("delete Token start");
        String sanitizedOnboardingId = onboardingId.replace("\n", "").replace("\r", "");
        log.debug("delete Token tokenId = {}", sanitizedOnboardingId);
        tokenService.rejectOnboarding(sanitizedOnboardingId, "REJECTED_BY_USER", SecurityIdentityUtils.getUid(securityIdentity));
        return Response.noContent().build();
    }

    @GET
    @Path("/{onboardingId}/contract")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    @Operation(summary = "${openapi.tokens.getContract}",
            description = "${openapi.tokens.getContract}", operationId = "getContractUsingGET")
    @APIResponse(responseCode = "200", description = "OK", content = @Content(mediaType = MediaType.APPLICATION_OCTET_STREAM))
    public Response getContract(@Parameter(description = "${openapi.tokens.onboardingId}")
                                @PathParam("onboardingId")
                                String onboardingId) {
        log.trace("getContract start");
        log.debug("getContract onboardingId = {}", LogUtils.sanitize(onboardingId));
        BinaryData contract = tokenService.getContract(onboardingId);
        return binaryResponse(contract);
    }

    @GET
    @Path("/{onboardingId}/backstage/contract")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    @Operation(summary = "Service to download a specific onboarding contract from the backstage (IAM protected)",
            description = "Service to download a specific onboarding contract from the backstage (IAM protected)",
            operationId = "getContractBackstageUsingGET")
    @APIResponse(responseCode = "200", description = "OK", content = @Content(mediaType = MediaType.APPLICATION_OCTET_STREAM))
    @APIResponse(responseCode = "403", description = FORBIDDEN_DOCUMENTS, content = @Content(mediaType = MediaType.APPLICATION_OCTET_STREAM))
    public Response getContractBackstage(@Parameter(description = "${openapi.tokens.onboardingId}")
                                         @PathParam("onboardingId") String onboardingId) {
        checkPermission(onboardingId, PermissionConstants.SELC_VIEW_ACCOUNT_DOCUMENTS);
        log.trace("getContractBackstage start");
        log.debug("getContractBackstage onboardingId = {}", LogUtils.sanitize(onboardingId));
        return binaryResponse(tokenService.getContract(onboardingId));
    }

    @GET
    @Path("/{onboardingId}/template-attachment")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    @Operation(summary = "${openapi.tokens.getTemplateAttachment}",
            description = "${openapi.tokens.getTemplateAttachment}", operationId = "getTemplateAttachmentUsingGET")
    public Uni<Response> getTemplateAttachment(@Parameter(description = "${openapi.tokens.onboardingId}")
                                          @PathParam("onboardingId")
                                          String onboardingId,
                                          @Parameter(description = "${openapi.tokens.attachmentName}", required = true)
                                          @QueryParam("attachmentName") String attachmentName) {
        attachmentName = RequestParams.stringQuery(uriInfo, "attachmentName", attachmentName);
        RequestParams.requiredQuery("attachmentName", attachmentName);
        log.trace("getTemplateAttachment start");
        String sanitizedFilename = attachmentName.replaceAll(SANITIZIER, "_");
        log.debug("getTemplateAttachment onboardingId = {}, filename = {}", Encode.forJava(onboardingId), sanitizedFilename);
        return tokenService.getTemplateAttachment(onboardingId, attachmentName).map(TokenV2Controller::binaryResponse);
    }

    @GET
    @Path("/{onboardingId}/attachment")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    @Operation(summary = "${openapi.tokens.getAttachment}",
            description = "${openapi.tokens.getAttachment}", operationId = "getAttachmentUsingGET")
    public Response getAttachment(@Parameter(description = "${openapi.tokens.onboardingId}")
                                  @PathParam("onboardingId")
                                  String onboardingId,
                                  @Parameter(description = "${openapi.tokens.attachmentName}", required = true)
                                  @QueryParam("name") String filename) {
        filename = RequestParams.stringQuery(uriInfo, "name", filename);
        RequestParams.requiredQuery("name", filename);
        log.trace("getAttachment start");
        String sanitizedFilename = filename.replaceAll(SANITIZIER, "_");
        log.debug("getAttachment onboardingId = {}, filename = {}", Encode.forJava(onboardingId), sanitizedFilename);
        BinaryData contract = tokenService.getAttachment(onboardingId, filename);
        return binaryResponse(contract);
    }

    @GET
    @Path("/{onboardingId}/available-documents")
    @Operation(summary = "Retrieve the list of documents available for download for the given onboarding",
            description = "Returns the list of attachment names and, if present, the filename of the signed contract associated with the onboarding.",
            operationId = "getAvailableDocumentsUsingGET")
    @APIResponse(responseCode = "200", description = "Successful operation",
            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = AvailableDocumentsResource.class)))
    @APIResponse(responseCode = "403", description = FORBIDDEN_DOCUMENTS,
            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = AvailableDocumentsResource.class)))
    public AvailableDocumentsResource getAvailableDocuments(@Parameter(description = "${openapi.tokens.onboardingId}")
                                                            @PathParam("onboardingId") String onboardingId) {
        checkPermission(onboardingId, PermissionConstants.SELC_VIEW_ACCOUNT_DOCUMENTS);
        log.trace("getAvailableDocuments start");
        log.debug("getAvailableDocuments onboardingId = {}", Encode.forJava(onboardingId));
        AvailableDocuments source = tokenService.getAvailableDocuments(onboardingId);
        AvailableDocumentsResource resource = new AvailableDocumentsResource();
        resource.setAttachments(source.getAttachments());
        resource.setContractFilename(source.getContractFilename());
        log.trace("getAvailableDocuments end");
        return resource;
    }

    @GET
    @Path("/{onboardingId}/download")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    @Operation(summary = "Download a document (signed contract or attachment) for the given onboarding",
            description = "When type=CONTRACT_SIGNED downloads the signed contract; "
                    + "when type=ATTACHMENT the 'name' query parameter is required.",
            operationId = "downloadDocumentUsingGET")
    @APIResponses(value = {
            @APIResponse(responseCode = "200", description = "Successful operation",
                    content = @Content(mediaType = MediaType.APPLICATION_OCTET_STREAM)),
            @APIResponse(responseCode = "400", description = "Invalid request - missing 'name' when type=ATTACHMENT or unsupported download type"),
            @APIResponse(responseCode = "401", description = "Unauthorized"),
            @APIResponse(responseCode = "403", description = FORBIDDEN_DOCUMENTS,
                    content = @Content(mediaType = MediaType.APPLICATION_OCTET_STREAM)),
            @APIResponse(responseCode = "404", description = "Onboarding or document not found")
    })
    // Both query parameters are read from UriInfo and declared here, so the document lists them in the Spring order
    @Parameter(name = "type", in = ParameterIn.QUERY,
            description = "Type of document to download", required = true,
            schema = @Schema(implementation = DownloadDocumentType.class))
    @Parameter(name = "name", in = ParameterIn.QUERY,
            description = "Name of the attachment. Required when type=ATTACHMENT, ignored otherwise.",
            schema = @Schema(type = SchemaType.STRING))
    public Response downloadDocument(@Parameter(description = "${openapi.tokens.onboardingId}")
                                     @PathParam("onboardingId") String onboardingId,
                                     @Context UriInfo uriInfo) {
        // Scalar @QueryParam binding loses the distinction between missing and explicitly empty values.
        String type = uriInfo.getQueryParameters().getFirst("type");
        String name = RequestParams.stringQuery(uriInfo, "name", uriInfo.getQueryParameters().getFirst("name"));
        DownloadDocumentType documentType = RequestParams.requiredEnum("type", type, DownloadDocumentType.class);
        checkPermission(onboardingId, PermissionConstants.SELC_VIEW_ACCOUNT_DOCUMENTS);
        log.trace("downloadDocument start");
        log.debug("downloadDocument onboardingId = {}, type = {}, name = {}",
                Encode.forJava(onboardingId), documentType, Encode.forJava(name));

        BinaryData document;
        switch (documentType) {
            case CONTRACT_SIGNED -> document = tokenService.getContractSigned(onboardingId);
            case ATTACHMENT -> {
                if (StringUtils.isBlank(name)) {
                    throw new InvalidRequestException(
                            "Query parameter 'name' is required when type=" + DownloadDocumentType.ATTACHMENT);
                }
                document = tokenService.getAttachment(onboardingId, name);
            }
            default -> throw new InvalidRequestException("Unsupported download type: " + documentType);
        }
        log.trace("downloadDocument end");
        return binaryResponse(document);
    }

    @HEAD
    @Path("/{onboardingId}/attachment/status")
    @Operation(summary = "${openapi.tokens.headAttachment}",
            description = "${openapi.tokens.headAttachment}", operationId = "headAttachmentUsingGET")
    public Response headAttachment(@Parameter(description = "${openapi.tokens.onboardingId}")
                                   @PathParam("onboardingId") String onboardingId,
                                   @Parameter(required = true) @QueryParam("name") String attachmentName) {
        attachmentName = RequestParams.stringQuery(uriInfo, "name", attachmentName);
        RequestParams.requiredQuery("name", attachmentName);
        log.trace("headAttachment start");
        log.debug("headAttachment onboardingId = {}, filename = {}", Encode.forJava(onboardingId), Encode.forJava(attachmentName));
        return attachmentStatus(tokenService.headAttachment(onboardingId, attachmentName));
    }

    @GET
    @Path("/{onboardingId}/attachment/status")
    @Operation(summary = "${openapi.tokens.headAttachment}",
            description = "${openapi.tokens.headAttachment}", operationId = "getAttachmentStatusUsingGET")
    public Response getAttachmentStatus(@Parameter(description = "${openapi.tokens.onboardingId}")
                                        @PathParam("onboardingId") String onboardingId,
                                        @Parameter(required = true) @QueryParam("name") String attachmentName) {
        attachmentName = RequestParams.stringQuery(uriInfo, "name", attachmentName);
        RequestParams.requiredQuery("name", attachmentName);
        log.trace("getAttachmentStatus start");
        log.debug("getAttachmentStatus onboardingId = {}, filename = {}", Encode.forJava(onboardingId), Encode.forJava(attachmentName));
        return attachmentStatus(tokenService.headAttachment(onboardingId, attachmentName));
    }

    @POST
    @Path("/{onboardingId}/attachment")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @RequestBody(required = false, content = @Content(mediaType = MediaType.MULTIPART_FORM_DATA,
            schema = @Schema(requiredProperties = "attachment")))
    @APIResponse(responseCode = "204", description = "No Content")
    @Operation(description = "${openapi.tokens.uploadAttachment}", summary = "${openapi.tokens.uploadAttachment}", operationId = "uploadAttachmentUsingPOST")
    @Blocking
    public Uni<Response> uploadAttachment(@Parameter(description = "${openapi.tokens.onboardingId}")
                                     @PathParam("onboardingId") String onboardingId,
                                     @Parameter(required = true) @QueryParam("attachmentName") String attachmentName,
                                     @RestForm("attachmentId") String attachmentId,
                                     @RestForm("attachmentDescription") String attachmentDescription,
                                     @RestForm("attachment") FileUpload attachment,
                                     @Parameter(hidden = true) @HeaderParam(TENANT_HEADER) String tenantId) {
        attachmentName = RequestParams.stringQuery(uriInfo, "attachmentName", attachmentName);
        RequestParams.requiredQuery("attachmentName", attachmentName);
        log.trace("uploadAttachment start");
        UploadedFile uploadedFile = toUploadedFile("attachment", attachment);
        FileValidationUtils.validatePdfOrP7m(uploadedFile);
        String sanitizedFileName = Encode.forJava(uploadedFile.fileName());
        String sanitizedOnboardingId = onboardingId.replaceAll(SANITIZIER, "");
        log.debug(LogUtils.CONFIDENTIAL_MARKER, "upload Attachment tokenId = {}, file = {}", sanitizedOnboardingId, sanitizedFileName);
        return tokenService.uploadAttachment(requiredTenantId(tenantId), onboardingId, uploadedFile,
                attachmentName, attachmentId, attachmentDescription)
                .replaceWith(() -> Response.noContent().build());
    }

    @GET
    @Path("/{onboardingId}/products/{productId}/aggregates-csv")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    @Operation(summary = "${openapi.tokens.getAggregatesCsv}",
            description = "${openapi.tokens.getAggregatesCsv}", operationId = "getAggregatesCsvUsingGET")
    @APIResponse(responseCode = "200", description = "OK", content = @Content(mediaType = MediaType.APPLICATION_OCTET_STREAM))
    @APIResponse(responseCode = "403", description = FORBIDDEN_DOCUMENTS, content = @Content(mediaType = MediaType.APPLICATION_OCTET_STREAM))
    public Response getAggregatesCsv(@Parameter(description = "${openapi.tokens.onboardingId}")
                                     @PathParam("onboardingId") String onboardingId,
                                     @Parameter(description = "${openapi.tokens.productId}")
                                     @PathParam("productId") String productId) {
        checkPermission(onboardingId, PermissionConstants.SELC_VIEW_ACCOUNT_DOCUMENTS);
        log.trace("getAggregatesCsv start");
        log.debug("getAggregatesCsv onboardingId = {}, productId = {}", Encode.forJava(onboardingId), Encode.forJava(productId));
        BinaryData csv = tokenService.getAggregatesCsv(onboardingId, productId);
        return binaryResponse(csv);
    }

    private void checkPermission(String onboardingId, String permission) {
        if (!authorizationService.hasPermission(securityIdentity, onboardingId, permission)) {
            throw new AccessDeniedException();
        }
    }

    private static String requiredTenantId(String tenantId) {
        if (StringUtils.isBlank(tenantId)) {
            throw new InvalidRequestException("Tenant context is required");
        }
        return tenantId;
    }

    private static Response attachmentStatus(int status) {
        return status >= 200 && status < 300
                ? Response.noContent().build()
                : Response.status(Response.Status.NOT_FOUND).build();
    }

    // The downstream file name is exposed as is: a missing one is rendered as "null", like the former Spring service
    private static Response binaryResponse(BinaryData data) {
        return Response.ok(data.content(), MediaType.APPLICATION_OCTET_STREAM)
                .header(ACCESS_CONTROL_EXPOSE_HEADERS, HttpHeaders.CONTENT_DISPOSITION)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + data.fileName())
                .build();
    }

    private static UploadedFile toUploadedFile(String partName, FileUpload fileUpload) {
        RequestParams.requiredPart(partName, fileUpload);
        try {
            return new UploadedFile(fileUpload.fileName(), fileUpload.contentType(), Files.readAllBytes(fileUpload.uploadedFile()));
        } catch (IOException e) {
            throw new IllegalStateException("Cannot read uploaded file", e);
        }
    }
}
