package it.pagopa.selfcare.onboarding.controller;

import io.quarkus.security.Authenticated;
import io.quarkus.security.identity.SecurityIdentity;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import it.pagopa.selfcare.onboarding.util.LogUtils;
import it.pagopa.selfcare.onboarding.client.model.UserId;
import it.pagopa.selfcare.onboarding.service.UserService;
import it.pagopa.selfcare.onboarding.controller.request.*;
import it.pagopa.selfcare.onboarding.controller.response.*;
import it.pagopa.selfcare.onboarding.model.error.Problem;
import it.pagopa.selfcare.onboarding.mapper.OnboardingMapper;
import it.pagopa.selfcare.onboarding.mapper.UserMapper;
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
    @ApiResponse(responseCode = "409",
            description = "Conflict",
            content = {
                    @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = Problem.class))
            })
    @Operation(summary = "${swagger.onboarding.user.api.validate}",
            description = "${swagger.onboarding.user.api.validate}", operationId = "validateUsingPOST")
    public Response validate(@Valid UserDataValidationDto request) {
        log.trace("validate start");
        log.debug(LogUtils.CONFIDENTIAL_MARKER, "validate request = {}", LogUtils.sanitize(request));
        userService.validate(userResourceMapper.toUser(request));
        log.trace("validate end");
        return Response.noContent().build();
    }

    @ApiResponse(responseCode = "403",
            description = "Forbidden",
            content = {
                    @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = Problem.class))
            })
    @POST
    @Path("/onboarding")
    @Operation(summary= "${swagger.onboarding.users.api.onboarding}",
            description = "${swagger.onboarding.users.api.onboarding}", operationId = "onboardingUsers")
    public Response onboarding(@Valid OnboardingUserDto request) {
        log.trace("onboarding start");
        log.debug("onboarding request = {}", LogUtils.sanitize(request));
        userService.onboardingUsers(onboardingResourceMapper.toEntity(request));
        log.trace("onboarding end");
        return Response.status(Response.Status.CREATED).build();
    }


    @ApiResponse(responseCode = "403",
            description = "Forbidden",
            content = {
                    @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = Problem.class))
            })
    @POST
    @Path("/onboarding/aggregator")
    @Operation(summary = "${swagger.onboarding.users.api.onboarding-aggregator}",
            description = "${swagger.onboarding.users.api.onboarding-aggregator}", operationId = "onboardingAggregatorUsingPOST")
    public Response onboardingAggregator(@Valid OnboardingUserDto request) {
        log.trace("onboardingAggregator start");
        log.debug("onboardingAggregator request = {}", Encode.forJava(request.toString()));
        userService.onboardingUsersAggregator(onboardingResourceMapper.toEntity(request));
        log.trace("onboardingAggregator end");
        return Response.status(Response.Status.CREATED).build();
    }

    @ApiResponse(responseCode = "403",
            description = "Forbidden",
            content = {
                    @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = Problem.class))
            })
    @POST
    @Path("/check-manager")
    @Operation(summary = "${swagger.onboarding.users.api.check-manager}",
            description = "${swagger.onboarding.users.api.check-manager}", operationId = "checkManager")
    public CheckManagerResponse checkManager(@Valid CheckManagerDto request) {
        log.trace("checkManager start");
        boolean checkManager =  userService.checkManager(onboardingResourceMapper.toCheckManagerData(request));
        log.trace("checkManager end");
        return new CheckManagerResponse(checkManager);
    }

    @ApiResponse(responseCode = "403",
            description = "Forbidden",
            content = {
                    @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = Problem.class))
            })
    @GET
    @Path("/onboarding/{onboardingId}/manager")
    @Operation(summary = "${swagger.onboarding.users.api.check-manager}",
            description = "${swagger.onboarding.users.api.check-manager}", operationId = "getManagerInfo")
    public ManagerInfoResponse getManagerInfo(@PathParam("onboardingId") String onboardingId) {
        log.trace("getManagerInfo start");
        String fiscalCode = SecurityIdentityUtils.getFiscalCode(securityIdentity);
        ManagerInfoResponse managerInfoResponse = userResourceMapper.toManagerInfoResponse(userService.getManagerInfo(onboardingId, fiscalCode));
        log.trace("getManagerInfo end");
        return managerInfoResponse;
    }

    @ApiResponse(responseCode = "403",
            description = "Forbidden",
            content = {
                    @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = Problem.class))
            })
    @POST
    @Path("/search-user")
    @Operation(summary = "${swagger.onboarding.users.api.search-user}",
            description = "${swagger.onboarding.users.api.search-user}", operationId = "searchUserId")
    public UserId searchUser(@Valid UserTaxCodeDto request) {
        log.trace("searchUser start");
        UserId userId =  userService.searchUser(userResourceMapper.toString(request));
        log.trace("searchUser end");
        return userId;
    }
}
