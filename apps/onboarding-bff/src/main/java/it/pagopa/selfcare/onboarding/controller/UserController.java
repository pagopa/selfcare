package it.pagopa.selfcare.onboarding.controller;

import io.quarkus.security.Authenticated;
import io.quarkus.security.identity.SecurityIdentity;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import it.pagopa.selfcare.onboarding.util.LogUtils;
import it.pagopa.selfcare.onboarding.client.model.UserId;
import it.pagopa.selfcare.onboarding.service.UserService;
import it.pagopa.selfcare.onboarding.model.dto.request.*;
import it.pagopa.selfcare.onboarding.model.dto.response.*;
import it.pagopa.selfcare.onboarding.model.error.Problem;
import it.pagopa.selfcare.onboarding.mapper.OnboardingMapper;
import it.pagopa.selfcare.onboarding.mapper.UserMapper;
import it.pagopa.selfcare.onboarding.util.RequestParams;
import it.pagopa.selfcare.onboarding.util.SecurityIdentityUtils;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.owasp.encoder.Encode;

@Slf4j
@ApplicationScoped
@Authenticated
@Path("/v1/users")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final OnboardingMapper onboardingResourceMapper;
    private final UserMapper userResourceMapper;

    @Inject
    SecurityIdentity securityIdentity;

    @POST
    @Path("/validate")
    @APIResponse(responseCode = "204", description = "No Content")
    @APIResponse(responseCode = "409",
            description = "Conflict",
            content = {
                    @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = Problem.class))
            })
    @Operation(summary = "${openapi.onboarding.user.api.validate}",
            description = "${openapi.onboarding.user.api.validate}", operationId = "validateUsingPOST")
    public Response validate(@Valid UserDataValidationDto request) {
        RequestParams.requiredBody(request);
        log.trace("validate start");
        log.debug(LogUtils.CONFIDENTIAL_MARKER, "validate request = {}", LogUtils.sanitize(request));
        userService.validate(userResourceMapper.toUser(request));
        log.trace("validate end");
        return Response.noContent().build();
    }

    @APIResponse(responseCode = "403",
            description = "Forbidden",
            content = {
                    @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = Problem.class))
            })
    @POST
    @Path("/onboarding")
    @APIResponse(responseCode = "201", description = "Created")
    @Operation(summary= "${openapi.onboarding.users.api.onboarding}",
            description = "${openapi.onboarding.users.api.onboarding}", operationId = "onboardingUsers")
    public Response onboarding(@Valid OnboardingUserDto request) {
        RequestParams.requiredBody(request);
        log.trace("onboarding start");
        log.debug("onboarding request = {}", LogUtils.sanitize(request));
        userService.onboardingUsers(onboardingResourceMapper.toEntity(request));
        log.trace("onboarding end");
        return Response.status(Response.Status.CREATED).build();
    }


    @APIResponse(responseCode = "403",
            description = "Forbidden",
            content = {
                    @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = Problem.class))
            })
    @POST
    @Path("/onboarding/aggregator")
    @APIResponse(responseCode = "201", description = "Created")
    @Operation(summary = "${openapi.onboarding.users.api.onboarding-aggregator}",
            description = "${openapi.onboarding.users.api.onboarding-aggregator}", operationId = "onboardingAggregatorUsingPOST")
    public Response onboardingAggregator(@Valid OnboardingUserDto request) {
        RequestParams.requiredBody(request);
        log.trace("onboardingAggregator start");
        log.debug("onboardingAggregator request = {}", Encode.forJava(request.toString()));
        userService.onboardingUsersAggregator(onboardingResourceMapper.toEntity(request));
        log.trace("onboardingAggregator end");
        return Response.status(Response.Status.CREATED).build();
    }

    @APIResponse(responseCode = "403",
            description = "Forbidden",
            content = {
                    @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = Problem.class))
            })
    @POST
    @Path("/check-manager")
    @APIResponse(responseCode = "200", description = "OK")
    @Operation(summary = "${openapi.onboarding.users.api.check-manager}",
            description = "${openapi.onboarding.users.api.check-manager}", operationId = "checkManager")
    public CheckManagerResponse checkManager(@Valid CheckManagerDto request) {
        RequestParams.requiredBody(request);
        log.trace("checkManager start");
        boolean checkManager =  userService.checkManager(onboardingResourceMapper.toCheckManagerData(request));
        log.trace("checkManager end");
        return new CheckManagerResponse(checkManager);
    }

    @APIResponse(responseCode = "403",
            description = "Forbidden",
            content = {
                    @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = Problem.class))
            })
    @GET
    @Path("/onboarding/{onboardingId}/manager")
    @APIResponse(responseCode = "200", description = "OK")
    @Operation(summary = "${openapi.onboarding.users.api.check-manager}",
            description = "${openapi.onboarding.users.api.check-manager}", operationId = "getManagerInfo")
    public ManagerInfoResponse getManagerInfo(@PathParam("onboardingId") String onboardingId) {
        log.trace("getManagerInfo start");
        String fiscalCode = SecurityIdentityUtils.getFiscalCode(securityIdentity);
        ManagerInfoResponse managerInfoResponse = userResourceMapper.toManagerInfoResponse(userService.getManagerInfo(onboardingId, fiscalCode));
        log.trace("getManagerInfo end");
        return managerInfoResponse;
    }

    @APIResponse(responseCode = "403",
            description = "Forbidden",
            content = {
                    @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = Problem.class))
            })
    @POST
    @Path("/search-user")
    @APIResponse(responseCode = "200", description = "OK")
    @Operation(summary = "${openapi.onboarding.users.api.search-user}",
            description = "${openapi.onboarding.users.api.search-user}", operationId = "searchUserId")
    public UserId searchUser(@Valid UserTaxCodeDto request) {
        RequestParams.requiredBody(request);
        log.trace("searchUser start");
        UserId userId =  userService.searchUser(userResourceMapper.toString(request));
        log.trace("searchUser end");
        return userId;
    }
}
