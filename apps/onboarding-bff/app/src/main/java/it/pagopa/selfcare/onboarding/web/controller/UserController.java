package it.pagopa.selfcare.onboarding.web.controller;

import io.swagger.annotations.Api;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import it.pagopa.selfcare.commons.base.logging.LogUtils;
import it.pagopa.selfcare.commons.base.security.SelfCareUser;
import it.pagopa.selfcare.commons.web.model.Problem;
import it.pagopa.selfcare.commons.web.security.JwtAuthenticationToken;
import it.pagopa.selfcare.onboarding.connector.model.user.UserId;
import it.pagopa.selfcare.onboarding.core.UserService;
import it.pagopa.selfcare.onboarding.web.model.*;
import it.pagopa.selfcare.onboarding.web.model.mapper.OnboardingResourceMapper;
import it.pagopa.selfcare.onboarding.web.model.mapper.UserResourceMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.security.Principal;
import lombok.extern.slf4j.Slf4j;
import org.owasp.encoder.Encode;

@Slf4j
@ApplicationScoped
@Path("/v1/users")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Api(tags = "user")
public class UserController {

    private final UserService userService;
    private final OnboardingResourceMapper onboardingResourceMapper;
    private final UserResourceMapper userResourceMapper;

    public UserController(UserService userService,
                          OnboardingResourceMapper onboardingResourceMapper,
                          UserResourceMapper userResourceMapper) {
        this.userService = userService;
        this.onboardingResourceMapper = onboardingResourceMapper;
        this.userResourceMapper = userResourceMapper;
    }

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
        log.debug(LogUtils.CONFIDENTIAL_MARKER, "validate request = {}", request);
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
        log.debug("onboarding request = {}", request);
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
    public ManagerInfoResponse getManagerInfo(@PathParam("onboardingId") String onboardingId, Principal principal) {
        log.trace("getManagerInfo start");
        JwtAuthenticationToken jwtAuthenticationToken = (JwtAuthenticationToken) principal;
        SelfCareUser selfCareUser = (SelfCareUser) jwtAuthenticationToken.getPrincipal();

        ManagerInfoResponse managerInfoResponse = userResourceMapper.toManagerInfoResponse(userService.getManagerInfo(onboardingId, selfCareUser.getFiscalCode()));
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
