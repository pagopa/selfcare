package it.pagopa.selfcare.onboarding.controller;

import io.quarkus.security.Authenticated;
import io.quarkus.security.identity.CurrentIdentityAssociation;
import io.quarkus.security.identity.SecurityIdentity;
import io.smallrye.jwt.auth.principal.DefaultJWTCallerPrincipal;
import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.onboarding.common.InstitutionType;
import it.pagopa.selfcare.onboarding.common.OnboardingStatus;
import it.pagopa.selfcare.onboarding.common.WorkflowType;
import it.pagopa.selfcare.onboarding.constants.CustomError;
import it.pagopa.selfcare.onboarding.controller.request.*;
import it.pagopa.selfcare.onboarding.controller.response.OnboardingGet;
import it.pagopa.selfcare.onboarding.controller.response.OnboardingGetResponse;
import it.pagopa.selfcare.onboarding.controller.response.OnboardingResponse;
import it.pagopa.selfcare.onboarding.entity.Billing;
import it.pagopa.selfcare.onboarding.entity.CheckManagerResponse;
import it.pagopa.selfcare.onboarding.entity.Onboarding;
import it.pagopa.selfcare.onboarding.entity.OnboardingAggregationImportRequest;
import it.pagopa.selfcare.onboarding.entity.UserRequester;
import it.pagopa.selfcare.onboarding.exception.ResourceNotFoundException;
import it.pagopa.selfcare.onboarding.mapper.OnboardingMapper;
import it.pagopa.selfcare.onboarding.model.OnboardingGetFilters;
import it.pagopa.selfcare.onboarding.model.RecipientCodeStatus;
import it.pagopa.selfcare.onboarding.service.OnboardingService;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpStatus;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.extensions.Extension;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.resteasy.reactive.RestForm;
import org.jboss.resteasy.reactive.server.core.ResteasyReactiveRequestContext;

import java.io.File;
import java.util.List;
import java.util.Objects;

import static it.pagopa.selfcare.onboarding.util.Utils.retrieveContractFromFormData;

@Authenticated
@Path("/v1/onboarding")
@Tag(name = "Onboarding Controller")
@AllArgsConstructor
@Slf4j
public class OnboardingController {

    @Inject
    SecurityIdentity securityIdentity;

    private final OnboardingService onboardingService;
    private final OnboardingMapper onboardingMapper;
    @Inject
    CurrentIdentityAssociation currentIdentityAssociation;

    @Operation(
            summary = "Default onboarding for GSP/SA/AS institutions, saves user data, and triggers async onboarding activities.",
            description = "Perform default onboarding request, it is used for GSP/SA/AS institution type." +
                    "Users data will be saved on personal data vault if it doesn't already exist." +
                    "At the end, function triggers async activities related to onboarding based on institution type."
    )
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<OnboardingResponse> onboarding(@Valid OnboardingDefaultRequest onboardingRequest, @Context SecurityContext ctx) {
        return readUserIdFromToken(ctx)
                .onItem().transformToUni(userId -> onboardingService
                        .onboarding(fillUserId(onboardingMapper.toEntity(onboardingRequest), userId),
                                onboardingRequest.getUsers(),
                                null,
                                onboardingRequest.getUserRequester()));
    }

    @Operation(
            summary = "Onboard users for all institution types, save user data, and trigger async onboarding activities.",
            description = "Perform onboarding users request, it is used all institution types." +
                    "Users data will be saved on personal data vault if it doesn't already exist." +
                    "At the end, function triggers async activities related to onboarding based on institution type."
    )
    @POST
    @Path("/users")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<OnboardingResponse> onboardingUsers(@Valid OnboardingUserRequest onboardingRequest, @Context SecurityContext ctx) {
        return readUserIdFromToken(ctx)
                .onItem().transformToUni(userId -> onboardingService
                        .onboardingUsers(onboardingRequest, userId, WorkflowType.USERS));
    }


    @Operation(
            summary = "Onboard users for aggregators, save user data, and trigger async onboarding activities.",
            description = "Perform onboarding users request, it is used for aggregators." +
                    "Users data will be saved on personal data vault if it doesn't already exist." +
                    "At the end, function triggers async activities related to onboarding based on institution type."
    )
    @POST
    @Path("/users/aggregator")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<OnboardingResponse> onboardingUsersAggregator(@Valid OnboardingUserRequest onboardingRequest, @Context SecurityContext ctx) {
        return readUserIdFromToken(ctx)
                .onItem().transformToUni(userId -> onboardingService
                        .onboardingUsers(onboardingRequest, userId, WorkflowType.USERS_EA));
    }

    @Operation(
            summary = "Onboarding for PA institutions with billing.recipientCode, saves user data, creates contracts, and sends emails.",
            description = "Perform onboarding request for PA institution type, it require billing.recipientCode in addition to default request" +
                    "Users data will be saved on personal data vault if it doesn't already exist." +
                    "At the end, function triggers async activities related to onboarding that consist of create contract and sending mail to institution's digital address."
    )
    @POST
    @Path("/pa")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<OnboardingResponse> onboardingPa(@Valid OnboardingPaRequest onboardingRequest, @Context SecurityContext ctx) {
        return readUserIdFromToken(ctx)
                .onItem().transformToUni(userId -> onboardingService
                        .onboarding(fillUserId(onboardingMapper.toEntity(onboardingRequest), userId), onboardingRequest.getUsers(), null, onboardingRequest.getUserRequester()));
    }
    @Operation(
            summary = "Aggregated onboarding for PA institutions, saves user data, creates contracts, and sends emails.",
            description = "Perform onboarding aggregation request for PA institution type, it require billing.recipientCode in addition to default request" +
                    "Users data will be saved on personal data vault if it doesn't already exist." +
                    "At the end, function triggers async activities related to onboarding aggregation that consist of create contract and sending mail to institution's digital address."
    )
    @POST
    @Path("/pa/aggregation")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<OnboardingResponse> onboardingPaAggregation(@Valid OnboardingPaRequest onboardingRequest, @Context SecurityContext ctx) {
        return readUserIdFromToken(ctx)
                .onItem().transformToUni(userId -> onboardingService
                        .onboarding(fillUserId(onboardingMapper.toEntity(onboardingRequest), userId), onboardingRequest.getUsers(), onboardingRequest.getAggregates(), onboardingRequest.getUserRequester()));
    }

    @Operation(summary = "Perform Increment for aggregates",
            description = "Perform the increment of the aggregates for an aggregator entity that has already completed the initial onboarding."
                    + "The API initiates the onboarding process for the aggregated entities received as input.")
    @POST
    @Path("/aggregation/increment")
    @Tag(name = "internal-v1")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<OnboardingResponse> onboardingAggregationIncrement(@Valid OnboardingPaRequest onboardingRequest, @Context SecurityContext ctx) {
        return readUserIdFromToken(ctx)
                .onItem().transformToUni(userId -> onboardingService
                        .onboardingIncrement(fillUserId(onboardingMapper.toEntity(onboardingRequest), userId),
                                onboardingRequest.getUsers(),
                                onboardingRequest.getAggregates(),
                                onboardingRequest.getUserRequester()));
    }

    @Operation(
            summary = "Onboarding for PSP institutions, saves user data, and requests admin approval via email.",
            description = "Perform onboarding request for PSP institution type." +
                    "Users data will be saved on personal data vault if it doesn't already exist." +
                    "At the end, function triggers async activities related to onboarding that consist of sending mail to Selfcare admin for approve request."
    )
    @POST
    @Path("/psp")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<OnboardingResponse> onboardingPsp(@Valid OnboardingPspRequest onboardingRequest, @Context SecurityContext ctx) {
        return readUserIdFromToken(ctx)
                .onItem().transformToUni(userId -> onboardingService
                        .onboarding(fillUserId(onboardingMapper.toEntity(onboardingRequest), userId), onboardingRequest.getUsers(), null, onboardingRequest.getUserRequester()));
    }

    @Operation(
            summary = "Complete onboarding request and set status to COMPLETED.",
            description = "Perform onboarding as /onboarding but completing the onboarding request to COMPLETED phase."
    )
    @Path("/completion")
    @POST
    @Tag(name = "Onboarding Controller")
    @Tag(name = "internal-v1")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<OnboardingResponse> onboardingCompletion(@Valid OnboardingDefaultRequest onboardingRequest, @Context SecurityContext ctx) {
        return readUserIdFromToken(ctx)
                .onItem().transformToUni(userId -> onboardingService
                        .onboardingCompletion(fillUserId(onboardingMapper.toEntity(onboardingRequest), userId),
                                onboardingRequest.getUsers(),
                                onboardingRequest.getUserRequester()));
    }

    @Operation(
            summary = "Complete PA onboarding request and set status to COMPLETED.",
            description =
                    "Perform onboarding as /onboarding/pa but completing the onboarding request to COMPLETED phase.")
    @POST
    @Path("/pa/completion")
    @Tag(name = "Onboarding Controller")
    @Tag(name = "internal-v1")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<OnboardingResponse> onboardingPaCompletion(@Valid OnboardingPaRequest onboardingRequest, @Context SecurityContext ctx) {
        return readUserIdFromToken(ctx)
                .onItem().transformToUni(userId -> onboardingService
                        .onboardingCompletion(fillUserId(onboardingMapper.toEntity(onboardingRequest), userId),
                                onboardingRequest.getUsers(),
                                onboardingRequest.getUserRequester()));
    }

  @Operation(
    summary = "Import PA onboarding with token creation and complete to COMPLETED.",
    description = "Perform onboarding as /onboarding/pa but create token and completing the onboarding request to COMPLETED phase."
  )
  @POST
  @Path("/pa/import")
  @Tag(name = "Onboarding Controller")
  @Tag(name = "internal-v1")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Uni<OnboardingResponse> onboardingPaImport(@Valid OnboardingImportRequest onboardingRequest, @Context SecurityContext ctx) {
    return readUserIdFromToken(ctx)
      .onItem().transformToUni(userId -> onboardingService
        .onboardingImport(fillUserId(onboardingMapper.toEntity(onboardingRequest), userId),
                onboardingRequest.getUsers(),
                onboardingRequest.getContractImported(),
                null));
  }

    @Operation(
            summary = "Import onboarding (not PA and PSP) with token creation and complete to COMPLETED.",
            description = "Perform onboarding (not PA and PSP) but create token and completing the request to COMPLETED phase."
    )
    @POST
    @Path("/import")
    @Tag(name = "Onboarding Controller")
    @Tag(name = "internal-v1")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<OnboardingResponse> onboardingImport(@Valid OnboardingDefaultRequest onboardingRequest, @Context SecurityContext ctx) {
        return readUserIdFromToken(ctx)
                .onItem().transformToUni(userId -> onboardingService
                        .onboardingImport(fillUserId(onboardingMapper.toEntity(onboardingRequest), userId), onboardingRequest.getUsers(), null, null));
    }

    @Operation(
            summary = "Import PSP onboarding with token creation and complete to COMPLETED.",
            description = "Perform onboarding as /onboarding/psp but create token and completing the onboarding request to COMPLETED phase."
    )
    @POST
    @Path("/psp/import")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<OnboardingResponse> onboardingPspImport(@Valid OnboardingImportPspRequest onboardingRequest, @Context SecurityContext ctx) {
        return readUserIdFromToken(ctx)
                .onItem().transformToUni(userId -> onboardingService
                        .onboardingImport(fillUserId(onboardingMapper.toEntity(onboardingRequest), userId), List.of(), onboardingRequest.getContractImported(), null));
    }

    @Operation(
            summary = "Complete PSP onboarding request and set status to COMPLETED.",
            description =
                    "Perform onboarding as /onboarding/psp but completing the onboarding request to COMPLETED phase.")
    @POST
    @Path("/psp/completion")
    @Tag(name = "Onboarding Controller")
    @Tag(name = "internal-v1")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<OnboardingResponse> onboardingPspCompletion(@Valid OnboardingPspRequest onboardingRequest, @Context SecurityContext ctx) {
        return readUserIdFromToken(ctx)
                .onItem().transformToUni(userId -> onboardingService
                        .onboardingCompletion(fillUserId(onboardingMapper.toEntity(onboardingRequest), userId), onboardingRequest.getUsers(), onboardingRequest.getUserRequester()));
    }

    @Operation(
            summary = "Complete PG onboarding request on PNPG domain and set status to COMPLETED.",
            description = "Perform onboarding as /onboarding/psp but completing the onboarding request to COMPLETED phase."
    )
    @POST
    @Path("/pg/completion")
    @Tag(name = "Onboarding Controller")
    @Tag(name = "internal-pnpg")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<OnboardingResponse> onboardingPgCompletion(@Valid OnboardingPgRequest onboardingRequest, @Context SecurityContext ctx) {
        return readUserIdFromToken(ctx)
                .onItem().transformToUni(userId -> onboardingService
                        .onboardingPgCompletion(fillUserId(onboardingMapper.toEntity(onboardingRequest), userId),
                                onboardingRequest.getUsers()));
    }

    @Operation(
            summary = "Add Manager of PG institution",
            description = "Create new onboarding request to add new Manager and replace the old inactive Managers of the institution."
    )
    @POST
    @Path("/users/pg")
    public Uni<OnboardingResponse> onboardingUsersPg(@Valid OnboardingUserPgRequest onboardingRequest, @Context SecurityContext ctx) {
        return readUserIdFromToken(ctx)
                .onItem().transformToUni(userId -> onboardingService
                        .onboardingUserPg(fillUserId(onboardingMapper.toEntity(onboardingRequest), userId), onboardingRequest.getUsers()));
    }

    private Uni<String> readUserIdFromToken(SecurityContext ctx) {
        return currentIdentityAssociation.getDeferredIdentity()
                .onItem().transformToUni(identity -> {
                    if (ctx.getUserPrincipal() == null || !ctx.getUserPrincipal().getName().equals(identity.getPrincipal().getName())) {
                        return Uni.createFrom().failure(new InternalServerErrorException("Principal and JsonWebToken names do not match"));
                    }

                    if (identity.getPrincipal() instanceof DefaultJWTCallerPrincipal jwtCallerPrincipal) {
                        String uid = jwtCallerPrincipal.getClaim("uid");
                        return Uni.createFrom().item(uid);
                    }

                    return Uni.createFrom().nullItem();
                });
    }

    @Operation(
            summary = "Complete onboarding by verifying and uploading contract, then trigger async activities.",
            description = "Perform complete operation of an onboarding request receiving onboarding id and contract signed by the institution." +
                    "It checks the contract's signature and upload the contract on an azure storage" +
                    "At the end, function triggers async activities related to complete onboarding " +
                    "that consist of create the institution, activate the onboarding and sending data to notification queue.",
            operationId = "completeOnboardingUsingPUT"
    )
    @PUT
    @Path("/{onboardingId}/complete")
    @Tag(name = "internal-v1")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Uni<Response> complete(@PathParam(value = "onboardingId") String onboardingId, @NotNull @RestForm("contract") File file, @Context ResteasyReactiveRequestContext ctx) {
        return onboardingService.complete(onboardingId, retrieveContractFromFormData(ctx.getFormData(), file))
                .map(ignore -> Response
                        .status(HttpStatus.SC_NO_CONTENT)
                        .build());
    }

    @Operation(
            summary = "Complete user onboarding by verifying and uploading contract, then trigger async activities.",
            description = "Perform complete operation of an user onboarding request receiving onboarding id and contract signed by the institution." +
                    "It checks the contract's signature and upload the contract on an azure storage" +
                    "At the end, function triggers async activities related to complete onboarding " +
                    "that consist of create the institution, activate the onboarding and sending data to notification queue.",
            extensions = @Extension(name = "x-legacy-api", value = "true")
    )
    @PUT
    @Path("/{onboardingId}/completeOnboardingUsers")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Uni<Response> completeOnboardingUser(@PathParam(value = "onboardingId") String onboardingId, @NotNull @RestForm("contract") File file, @Context ResteasyReactiveRequestContext ctx) {
        return onboardingService.completeOnboardingUsers(onboardingId, retrieveContractFromFormData(ctx.getFormData(), file))
                .map(ignore -> Response
                        .status(HttpStatus.SC_NO_CONTENT)
                        .build());
    }

    @Operation(
            summary = "Complete onboarding without verifying contract signature.",
            description = "Perform complete operation of an onboarding request as /complete but without signature verification of the contract",
            operationId = "completeOnboardingTokenConsume"
    )
    @PUT
    @Path("/{onboardingId}/consume")
    @Tag(name = "support")
    @Tag(name = "internal-v1")
    @Tag(name = "Onboarding")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Uni<Response> consume(@PathParam(value = "onboardingId") String onboardingId, @NotNull @RestForm("contract") File file, @Context ResteasyReactiveRequestContext ctx) {
        return onboardingService.completeWithoutSignatureVerification(onboardingId, retrieveContractFromFormData(ctx.getFormData(), file))
                .map(ignore -> Response
                        .status(HttpStatus.SC_NO_CONTENT)
                        .build());
    }

    @Operation(
            summary = "Approve onboarding request and trigger async activities.",
            description = "Perform approve operation of an onboarding request receiving onboarding id." +
                    "Function triggers async activities related to onboarding based on institution type or completing onboarding."
    )
    @PUT
    @Path("/{onboardingId}/approve")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Uni<Response> approve(@PathParam(value = "onboardingId") String onboardingId) {
        return onboardingService.approve(onboardingId)
                .map(ignore -> Response
                        .status(HttpStatus.SC_OK)
                        .build());
    }

    @Operation(
            summary = "Retrieve paged onboardings with optional filters and sorting.",
            description = "The API retrieves paged onboarding using optional filter, order by descending creation date"
    )
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<OnboardingGetResponse> getOnboardingWithFilter(@QueryParam(value = "productId") String productId,
                                                              @QueryParam(value = "taxCode") String taxCode,
                                                              @QueryParam(value = "institutionId") String institutionId,
                                                              @QueryParam(value = "onboardingId") String onboardingId,
                                                              @QueryParam(value = "subunitCode") String subunitCode,
                                                              @QueryParam(value = "from") String from,
                                                              @QueryParam(value = "to") String to,
                                                              @QueryParam(value = "status") String status,
                                                              @QueryParam(value = "userId") String userId,
                                                              @QueryParam(value = "productIds") List<String> productIds,
                                                              @QueryParam(value = "page") @DefaultValue("0") Integer page,
                                                              @QueryParam(value = "size") @DefaultValue("20") Integer size,
                                                              @QueryParam(value = "skipPagination") boolean skipPagination) {
        OnboardingGetFilters filters = OnboardingGetFilters.builder()
                .productId(productId)
                .taxCode(taxCode)
                .institutionId(institutionId)
                .onboardingId(onboardingId)
                .subunitCode(subunitCode)
                .from(from)
                .to(to)
                .status(status)
                .userId(userId)
                .productIds(productIds)
                .page(page)
                .skipPagination(skipPagination)
                .size(size)
                .build();
        return onboardingService.onboardingGet(filters);
    }

    @Operation(summary = "Perform reject operation of an onboarding request",
            description = "Perform reject operation of an onboarding request receiving onboarding id." +
                    "Function change status to REJECT for an onboarding request that is not COMPLETED. ")
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("/{onboardingId}/reject")
    public Uni<Response> delete(@PathParam(value = "onboardingId") String onboardingId, @Valid ReasonRequest reason) {
        String reasonForReject = reason.getReasonForReject();
        return onboardingService.rejectOnboarding(onboardingId, reasonForReject)
                .map(ignore -> Response
                        .status(HttpStatus.SC_NO_CONTENT)
                        .build());
    }

    @Operation(
            summary = "Get onboarding record by ID.",
            description = "Retrieve an onboarding record given its ID"
    )
    @GET
    @Path("/{onboardingId}")
    public Uni<OnboardingGet> getById(@PathParam(value = "onboardingId") String onboardingId) {
        return onboardingService.onboardingGet(onboardingId);
    }

    @Operation(
            summary = "Get onboarding record by ID with user sensitive info.",
            description = "Retrieve an onboarding record given its ID adding to user sensitive information",
            extensions = @Extension(name = "x-legacy-api", value = "true")
    )
    @GET
    @Path("/{onboardingId}/withUserInfo")
    public Uni<OnboardingGet> getByIdWithUserInfo(@PathParam(value = "onboardingId") String onboardingId) {
        return onboardingService.onboardingGetWithUserInfo(onboardingId);
    }

    @Operation(
            summary = "Get pending onboarding by ID.",
            description = "Returns an onboarding record by its ID only if its status is PENDING. " +
                    "This feature is crucial for ensuring that the onboarding process can be completed only when " +
                    "the onboarding status is appropriately set to PENDING."
    )
    @GET
    @Path("/{onboardingId}/pending")
    public Uni<OnboardingGet> getOnboardingPending(@PathParam(value = "onboardingId") String onboardingId) {
        return onboardingService.onboardingPending(onboardingId);
    }

    @Operation(
            summary = "Get onboardings by institution taxCode, subunitCode, origin, or originId.",
            description = "Returns onboardings record by institution taxCode/subunitCode/origin/originId",
            operationId = "onboardingInstitutionUsingGET",
            extensions = @Extension(name = "x-legacy-api", value = "true")
    )
    @GET
    @Tag(name = "support")
    @Tag(name = "internal-v1")
    @Tag(name = "Onboarding")
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/institutionOnboardings")
    public Uni<List<OnboardingResponse>> getOnboardingPending(@QueryParam(value = "taxCode") String taxCode,
                                                              @QueryParam(value = "subunitCode") String subunitCode,
                                                              @QueryParam(value = "origin") String origin,
                                                              @QueryParam(value = "originId") String originId,
                                                              @QueryParam(value = "status") OnboardingStatus status) {
        return onboardingService.institutionOnboardings(taxCode, subunitCode, origin, originId, status);
    }

    private Onboarding fillUserId(Onboarding onboarding, String userRequestUid) {
        onboarding.setUserRequester(UserRequester.builder().userRequestUid(userRequestUid).build());
        return onboarding;
    }

    @Operation(
            summary = "Update onboarding request with new values.",
            description = "Update onboarding request receiving onboarding id." +
                    "Function can change some values.",
            operationId = "updateOnboardiUsingPUT"
    )
    @PUT
    @Tag(name = "support")
    @Tag(name = "Onboarding")
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("/{onboardingId}/update")
    public Uni<Response> update(@PathParam(value = "onboardingId") String onboardingId,
                                @QueryParam(value = "status") OnboardingStatus status,
                                OnboardingDefaultRequest onboardingRequest) {
        return onboardingService.updateOnboarding(onboardingId, onboardingMapper.toEntity(onboardingRequest, status))
                .map(ignore -> Response.status(HttpStatus.SC_NO_CONTENT).build());
    }

    @Operation(
            summary = "Update recipient code",
            description = "Update recipient code receiving onboarding id.",
            operationId = "updateOnboardingRecipientIdUsingPUT")
    @PUT
    @Tag(name = "billing-portal")
    @Tag(name = "Onboarding")
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("/{onboardingId}/recipient-code")
    public Uni<Response> updateRecipientCodeByOnboardingId(
            @PathParam(value = "onboardingId") String onboardingId,
            @QueryParam(value = "recipientCode") String recipientCode) {
        Onboarding onboarding = new Onboarding();
        Billing billing = new Billing();
        billing.setRecipientCode(recipientCode.trim());
        onboarding.setBilling(billing);
        log.trace("update RecipientCode start");
        log.debug("Onboarding id {} and recipientCode {}", onboardingId.replace("\n", "").replace("\r", ""), recipientCode.replace("\n", "").replace("\r", ""));
        return onboardingService
                .updateOnboarding(onboardingId, onboarding)
                .map(ignore -> Response.status(HttpStatus.SC_NO_CONTENT).build())
                .log("update RecipientCode end");
    }

    @Operation(
            summary = "Check if new manager matches the current manager.",
            description = "In the addition administrator flow, it checks " +
                    "if the new manager is equal from old one, returning true "
    )
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/check-manager")
    public Uni<CheckManagerResponse> checkManager(CheckManagerRequest checkManagerRequest) {
        return onboardingService.checkManager(checkManagerRequest);
    }

    @Operation(
            summary = "Asynchronously complete aggregated onboarding to PENDING status.",
            description = "Perform onboarding aggregation as /onboarding to PENDING phase. The operation will be performed async due to the possible amount of time the process could take."
    )
    @Path("/aggregation/completion")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<OnboardingResponse> onboardingAggregationCompletion(@Valid OnboardingDefaultRequest onboardingRequest, @Context SecurityContext ctx) {
        return readUserIdFromToken(ctx)
                .onItem().transformToUni(userId -> onboardingService
                        .onboardingAggregationCompletion(fillUserId(onboardingMapper.toEntity(onboardingRequest), userId),
                                onboardingRequest.getUsers(),
                                onboardingRequest.getAggregates(),
                                onboardingRequest.getUserRequester()));
    }

    @Operation(
            summary = "Asynchronously complete aggregated PSP onboarding to PENDING status.",
            description = "Perform onboarding aggregation as /onboarding to PENDING phase. The operation will be performed async due to the possible amount of time the process could take."
    )
    @Path("/aggregation/psp/completion")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<OnboardingResponse> onboardingAggregationPspCompletion(@Valid OnboardingPspRequest onboardingRequest, @Context SecurityContext ctx) {
        return readUserIdFromToken(ctx)
                .onItem().transformToUni(userId -> onboardingService
                        .onboardingAggregationCompletion(fillUserId(onboardingMapper.toEntity(onboardingRequest), userId),
                                onboardingRequest.getUsers(),
                                onboardingRequest.getAggregates(),
                                onboardingRequest.getUserRequester()));
    }

    @Operation(
            summary = "Check if product is already onboarded for the institution.",
            description = "Verify if the onboarded product is already onboarded for the institution"
    )
    @HEAD
    @Path("/verify")
    public Uni<Response> verifyOnboardingInfoByFilters(@QueryParam("productId") String productId,
                                                       @QueryParam("taxCode") String taxCode,
                                                       @QueryParam("origin") String origin,
                                                       @QueryParam("originId") String originId,
                                                       @QueryParam("subunitCode") String subunitCode,
                                                       @QueryParam("institutionType") String institutionType) {
        InstitutionType institutionTypeEnum = null;
        if (institutionType != null && !institutionType.isEmpty()) {
            institutionTypeEnum = InstitutionType.valueOf(institutionType);
        }
        return onboardingService.verifyOnboarding(taxCode, subunitCode, origin, originId, OnboardingStatus.COMPLETED, productId, institutionTypeEnum)
                .onItem().transform(onboardingList -> {
                    if (onboardingList.isEmpty()) {
                        throw new ResourceNotFoundException(CustomError.INSTITUTION_NOT_ONBOARDED_BY_FILTERS.getMessage(),
                                CustomError.INSTITUTION_NOT_ONBOARDED_BY_FILTERS.getCode());
                    } else {
                        return Response.status(HttpStatus.SC_NO_CONTENT).build();
                    }
                });
    }

    @Operation(
            summary = "Validate recipientCode.",
            description = "check if recipientCode is valid or not",
            extensions = @Extension(name = "x-legacy-api", value = "true")
    )
    @GET
    @Tag(name = "billing-portal")
    @Tag(name = "Onboarding Controller")
    @Path("/checkRecipientCode")
    public Uni<RecipientCodeStatus> checkRecipientCode(@QueryParam("recipientCode") String recipientCode,
                                                       @QueryParam("originId") String originId) {
        return onboardingService.checkRecipientCode(recipientCode, originId)
                .onItem().transform(customError -> {
                    if (Objects.nonNull(customError) && customError.name().equals(RecipientCodeStatus.DENIED_NO_BILLING.name())) {
                        return RecipientCodeStatus.DENIED_NO_BILLING;
                    }
                    if (Objects.nonNull(customError) && customError.name().equals(RecipientCodeStatus.DENIED_NO_ASSOCIATION.name())) {
                        return RecipientCodeStatus.DENIED_NO_ASSOCIATION;
                    }
                    return RecipientCodeStatus.ACCEPTED;
                });
    }

    @Operation(
        summary = "Asynchronously import aggregated onboarding to COMPLETED status and create token",
        description = "Perform a manual onboarding with aggregator, create token and set onboarding status to COMPLETED phase."
    )
    @Path("/aggregation/import")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<OnboardingResponse> onboardingAggregationImport(@Valid OnboardingAggregationImportRequest onboardingRequest,
        @Context SecurityContext ctx) {
        return readUserIdFromToken(ctx)
            .onItem().transformToUni(userId -> onboardingService
                .onboardingAggregationImport(fillUserId(onboardingMapper.toEntity(onboardingRequest), userId),
                    onboardingRequest.getOnboardingImportContract(),
                        onboardingRequest.getUsers(),
                        onboardingRequest.getAggregates(),
                        onboardingRequest.getUserRequester()));
    }

    @Operation(summary = "Perform delete operation of an onboarding request",
            description = "Perform delete operation of an onboarding request receiving onboarding id, " +
                    "then invokes async process to set DELETED as status for institution and user onboardings.")
    @DELETE
    @Tag(name = "internal-v1")
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("/{onboardingId}")
    public Uni<Response> deleteOnboarding(@PathParam(value = "onboardingId") String onboardingId) {
        return onboardingService.deleteOnboarding(onboardingId)
                .map(ignore -> Response
                        .status(HttpStatus.SC_NO_CONTENT)
                        .build());
    }

    @Operation(
            summary = "Get onboarding COMPLETED by institutionId and productId.",
            description = "Retrieve an onboarding record given institutionId and productId"
    )
    @GET
    @Tag(name = "external-v2")
    @Tag(name = "internal-v1")
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/institutions/{institutionId}")
    public Uni<OnboardingGet> getOnboardingProduct(@PathParam(value = "institutionId") String institutionId,
                                                   @QueryParam(value = "productId") String productId) {
        return onboardingService.retrieveOnboardingByInstitutionId(institutionId, productId);
    }

    @Operation(
            summary = "Update COMPELTED onboarding uploading signed contract",
            description = "Uploading signed contract given onboardingId, updating related token",
            operationId = "uploadContractSigned"
    )
    @PUT
    @Path("/{onboardingId}/upload-contract-signed")
    @Tag(name = "internal-v1")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Uni<Response> uploadContractSigned(@PathParam(value = "onboardingId") String onboardingId, @NotNull @RestForm("contract") File file, @Context ResteasyReactiveRequestContext ctx) {
        return onboardingService.uploadContractSigned(onboardingId, retrieveContractFromFormData(ctx.getFormData(), file))
                .map(ignore -> Response
                        .status(HttpStatus.SC_NO_CONTENT)
                        .build());
    }
}
