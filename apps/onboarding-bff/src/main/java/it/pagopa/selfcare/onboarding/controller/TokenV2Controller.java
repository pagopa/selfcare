package it.pagopa.selfcare.onboarding.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import it.pagopa.selfcare.commons.base.logging.LogUtils;
import it.pagopa.selfcare.commons.base.security.SelfCareUser;
import it.pagopa.selfcare.onboarding.common.OnboardingStatus;
import it.pagopa.selfcare.onboarding.exception.UnauthorizedUserException;
import it.pagopa.selfcare.onboarding.client.model.BinaryData;
import it.pagopa.selfcare.onboarding.client.model.UploadedFile;
import it.pagopa.selfcare.onboarding.client.model.OnboardingData;
import it.pagopa.selfcare.onboarding.service.TokenService;
import it.pagopa.selfcare.onboarding.service.UserInstitutionService;
import it.pagopa.selfcare.onboarding.service.UserService;
import it.pagopa.selfcare.onboarding.controller.response.OnboardingRequestResource;
import it.pagopa.selfcare.onboarding.model.OnboardingVerify;
import it.pagopa.selfcare.onboarding.controller.request.ReasonForRejectDto;
import it.pagopa.selfcare.onboarding.mapper.OnboardingResourceMapper;
import it.pagopa.selfcare.onboarding.util.FileValidationUtils;
import it.pagopa.selfcare.onboarding.util.PrincipalUtils;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HEAD;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.io.IOException;
import java.nio.file.Files;
import java.security.Principal;
import lombok.extern.slf4j.Slf4j;
import org.jboss.resteasy.reactive.RestForm;
import org.jboss.resteasy.reactive.multipart.FileUpload;
import org.owasp.encoder.Encode;

@Slf4j
@ApplicationScoped
@Path("/v2/tokens")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "tokens")
public class TokenV2Controller {

    private static final String ACCESS_CONTROL_EXPOSE_HEADERS = "Access-Control-Expose-Headers";
    private final TokenService tokenService;
    private final UserService userService;
    private final UserInstitutionService userInstitutionService;
    private final OnboardingResourceMapper onboardingResourceMapper;
    private static final String SANITIZIER = "[^a-zA-Z0-9-_]";

    public TokenV2Controller(TokenService tokenService, UserService userService, UserInstitutionService userInstitutionService, OnboardingResourceMapper onboardingResourceMapper) {
        this.tokenService = tokenService;
        this.userService = userService;
        this.userInstitutionService = userInstitutionService;
        this.onboardingResourceMapper = onboardingResourceMapper;
    }

    @POST
    @Path("/{onboardingId}/complete")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Operation(description = "${swagger.tokens.complete}", summary = "${swagger.tokens.complete}", operationId = "completeUsingPOST")
    public Response complete(@Parameter(description = "${swagger.tokens.onboardingId}")
                             @PathParam("onboardingId") String onboardingId,
                             @RestForm("contract") FileUpload contract) {
        log.trace("complete Token start");
        UploadedFile uploadedFile = toUploadedFile(contract);
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
    @Operation(description = "${swagger.tokens.completeOnboardingUsers}", summary = "${swagger.tokens.completeOnboardingUsers}",
            operationId = "completeOnboardingUsersUsingPOST")
    public Response completeOnboardingUsers(@Parameter(description = "${swagger.tokens.onboardingId}")
                                            @PathParam("onboardingId") String onboardingId,
                                            @RestForm("contract") FileUpload contract) {
        log.trace("complete Onboarding Users start");
        UploadedFile uploadedFile = toUploadedFile(contract);
        FileValidationUtils.validatePdfOrP7m(uploadedFile);
        String sanitizedFileName = Encode.forJava(uploadedFile.fileName());
        String sanitizedOnboardingId = onboardingId.replaceAll(SANITIZIER, "");
        log.debug(LogUtils.CONFIDENTIAL_MARKER, "complete Onboarding Users tokenId = {}, contract = {}", sanitizedOnboardingId, sanitizedFileName);
        tokenService.completeOnboardingUsers(onboardingId, uploadedFile);
        return Response.noContent().build();
    }

    @POST
    @Path("/{onboardingId}/verify")
    @Operation(description = "${swagger.tokens.verify}",
            summary = "${swagger.tokens.verify}", operationId = "verifyOnboardingUsingPOST")
    public OnboardingVerify verifyOnboarding(@Parameter(description = "${swagger.tokens.onboardingId}") @PathParam("onboardingId") String onboardingId) {
        String sanitizedOnboardingId = onboardingId.replace("\n", "").replace("\r", "");
        log.debug("Verify token identified with {}", sanitizedOnboardingId);
        final OnboardingData onboardingData = tokenService.verifyOnboarding(sanitizedOnboardingId);
        OnboardingVerify result = onboardingResourceMapper.toOnboardingVerify(onboardingData);
        log.debug("Verify token identified result = {}", result);
        log.trace("Verify token identified end");
        return result;
    }

    @GET
    @Path("/{onboardingId}")
    @Operation(summary = "${swagger.tokens.retrieveOnboardingRequest}",
            description = "${swagger.tokens.retrieveOnboardingRequest}", operationId = "retrieveOnboardingRequestUsingGET")
    public OnboardingRequestResource retrieveOnboardingRequest(@Parameter(description = "${swagger.tokens.onboardingId}")
                                                               @PathParam("onboardingId")
                                                               String onboardingId) {
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
    @Operation(description = "${swagger.tokens.approveOnboardingRequest}",
            summary = "${swagger.tokens.approveOnboardingRequest}", operationId = "approveOnboardingUsingPOST")
    public void approveOnboarding(@Parameter(description = "${swagger.tokens.onboardingId}")
                                  @PathParam("onboardingId") String onboardingId) {
        log.debug("approve onboarding identified with {}", onboardingId);
        tokenService.approveOnboarding(onboardingId);
    }

    @POST
    @Path("/{onboardingId}/reject")
    @Operation(summary = "Service to reject a specific onboarding request",
            description = "Service to reject a specific onboarding request", operationId = "rejectOnboardingUsingPOST")
    public void rejectOnboarding(@Parameter(description = "${swagger.tokens.onboardingId}")
                                 @PathParam("onboardingId") String onboardingId,
                                 ReasonForRejectDto reasonForRejectDto) {
        log.debug("reject onboarding identified with {}", onboardingId);
        tokenService.rejectOnboarding(onboardingId, reasonForRejectDto.getReason());
    }

    @DELETE
    @Path("/{onboardingId}/complete")
    @Operation(summary = "${swagger.tokens.complete}",
            description = "${swagger.tokens.complete}", operationId = "deleteUsingDELETE")
    public Response deleteOnboarding(@Parameter(description = "${swagger.tokens.tokenId}")
                                     @PathParam("onboardingId") String onboardingId) {
        log.trace("delete Token start");
        String sanitizedOnboardingId = onboardingId.replace("\n", "").replace("\r", "");
        log.debug("delete Token tokenId = {}", sanitizedOnboardingId);
        tokenService.rejectOnboarding(sanitizedOnboardingId, "REJECTED_BY_USER");
        return Response.noContent().build();
    }

    @GET
    @Path("/{onboardingId}/contract")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    @Operation(summary = "${swagger.tokens.getContract}",
            description = "${swagger.tokens.getContract}", operationId = "getContractUsingGET")
    public Response getContract(@Parameter(description = "${swagger.tokens.onboardingId}")
                                @PathParam("onboardingId")
                                String onboardingId) {
        log.trace("getContract start");
        log.debug("getContract onboardingId = {}", onboardingId);
        BinaryData contract = tokenService.getContract(onboardingId);
        return binaryResponse(contract);
    }

    @GET
    @Path("/{onboardingId}/template-attachment")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    @Operation(summary = "${swagger.tokens.getTemplateAttachment}",
            description = "${swagger.tokens.getTemplateAttachment}", operationId = "getTemplateAttachmentUsingGET")
    public Response getTemplateAttachment(@Parameter(description = "${swagger.tokens.onboardingId}")
                                          @PathParam("onboardingId")
                                          String onboardingId,
                                          @Parameter(description = "${swagger.tokens.attachmentName}")
                                          @QueryParam("name") String filename) {
        log.trace("getTemplateAttachment start");
        String sanitizedFilename = filename.replaceAll(SANITIZIER, "_");
        log.debug("getTemplateAttachment onboardingId = {}, filename = {}", Encode.forJava(onboardingId), sanitizedFilename);
        BinaryData contract = tokenService.getTemplateAttachment(onboardingId, filename);
        return binaryResponse(contract);
    }

    @GET
    @Path("/{onboardingId}/attachment")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    @Operation(summary = "${swagger.tokens.getAttachment}",
            description = "${swagger.tokens.getAttachment}", operationId = "getAttachmentUsingGET")
    public Response getAttachment(@Parameter(description = "${swagger.tokens.onboardingId}")
                                  @PathParam("onboardingId")
                                  String onboardingId,
                                  @Parameter(description = "${swagger.tokens.attachmentName}")
                                  @QueryParam("name") String filename) {
        log.trace("getAttachment start");
        String sanitizedFilename = filename.replaceAll(SANITIZIER, "_");
        log.debug("getAttachment onboardingId = {}, filename = {}", Encode.forJava(onboardingId), sanitizedFilename);
        BinaryData contract = tokenService.getAttachment(onboardingId, filename);
        return binaryResponse(contract);
    }

    @HEAD
    @Path("/{onboardingId}/attachment/status")
    @Operation(summary = "${swagger.tokens.headAttachment}",
            description = "${swagger.tokens.headAttachment}", operationId = "headAttachmentUsingGET")
    public Response headAttachment(@Parameter(description = "${swagger.tokens.onboardingId}")
                                   @PathParam("onboardingId") String onboardingId,
                                   @NotNull @QueryParam("name") String attachmentName) {
        log.trace("headAttachment start");
        log.debug("headAttachment onboardingId = {}, filename = {}", Encode.forJava(onboardingId), Encode.forJava(attachmentName));
        int attachmentResponse = tokenService.headAttachment(onboardingId, attachmentName);
        return attachmentResponse >= 200 && attachmentResponse < 300
                ? Response.noContent().build()
                : Response.status(Response.Status.NOT_FOUND).build();
    }

    @POST
    @Path("/{onboardingId}/attachment")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Operation(description = "${swagger.tokens.uploadAttachment}", summary = "${swagger.tokens.uploadAttachment}", operationId = "uploadAttachmentUsingPOST")
    public Response uploadAttachment(@Parameter(description = "${swagger.tokens.onboardingId}")
                                     @PathParam("onboardingId") String onboardingId,
                                     @QueryParam("name") String attachmentName,
                                     @RestForm("file") FileUpload attachment) {
        log.trace("uploadAttachment start");
        UploadedFile uploadedFile = toUploadedFile(attachment);
        FileValidationUtils.validatePdfOrP7m(uploadedFile);
        String sanitizedFileName = Encode.forJava(uploadedFile.fileName());
        String sanitizedOnboardingId = onboardingId.replaceAll(SANITIZIER, "");
        log.debug(LogUtils.CONFIDENTIAL_MARKER, "upload Attachment tokenId = {}, file = {}", sanitizedOnboardingId, sanitizedFileName);
        tokenService.uploadAttachment(onboardingId, uploadedFile, attachmentName);
        return Response.noContent().build();
    }

    @GET
    @Path("/{onboardingId}/products/{productId}/aggregates-csv")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    @Operation(summary = "${swagger.tokens.getAggregatesCsv}",
            description = "${swagger.tokens.getAggregatesCsv}", operationId = "getAggregatesCsvUsingGET")
    public Response getAggregatesCsv(@Parameter(description = "${swagger.tokens.onboardingId}") @PathParam("onboardingId")
                                     String onboardingIdInput,
                                     @Parameter(description = "${swagger.tokens.productId}")
                                     @PathParam("productId")
                                     String productIdInput, @Context Principal principal) {

        SelfCareUser selfCareUser = PrincipalUtils.getSelfCareUser(principal);
        log.trace("getAggregatesCsv start");
        String onboardingId = Encode.forJava(onboardingIdInput);
        String productId = Encode.forJava(productIdInput);
        log.debug("getAggregatesCsv onboardingId = {}, productId = {}", onboardingId, productId);

        String userUid = selfCareUser.getId();
        OnboardingData onboardingWithUserInfo = tokenService.getOnboardingWithUserInfo(onboardingId);

        if ((OnboardingStatus.COMPLETED.name().equalsIgnoreCase(onboardingWithUserInfo.getStatus()) && userInstitutionService.verifyAllowedUserInstitution(
                onboardingWithUserInfo.getInstitutionUpdate().getId(), productId, userUid)) || tokenService.verifyAllowedUserByRole(onboardingId, userUid)
                || userService.isAllowedUserByUid(userUid)) {
            BinaryData csv = tokenService.getAggregatesCsv(onboardingId, productId);
            return binaryResponse(csv);
        } else {
            throw new UnauthorizedUserException("Normal-User not allowed to use this endpoint.");
        }

    }

    private static Response binaryResponse(BinaryData data) {
        String fileName = data.fileName() == null || data.fileName().isBlank() ? "download.bin" : data.fileName();
        return Response.ok(data.content(), MediaType.APPLICATION_OCTET_STREAM)
                .header(ACCESS_CONTROL_EXPOSE_HEADERS, HttpHeaders.CONTENT_DISPOSITION)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + fileName)
                .build();
    }

    private static UploadedFile toUploadedFile(FileUpload fileUpload) {
        if (fileUpload == null) {
            return null;
        }
        try {
            return new UploadedFile(fileUpload.fileName(), fileUpload.contentType(), Files.readAllBytes(fileUpload.uploadedFile()));
        } catch (IOException e) {
            throw new IllegalStateException("Cannot read uploaded file", e);
        }
    }
}
