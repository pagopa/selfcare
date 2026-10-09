package it.pagopa.selfcare.onboarding.controller;

import io.smallrye.mutiny.Uni;
import io.quarkus.security.Authenticated;
import io.quarkus.security.identity.SecurityIdentity;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody;
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
@Consumes({MediaType.APPLICATION_JSON, "application/*"})
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
    public Uni<Response> validate(@RequestBody(content = @Content(mediaType = MediaType.APPLICATION_JSON))
                                 @Valid UserDataValidationDto request) {
        RequestParams.requiredBody(request, "public void it.pagopa.selfcare.onboarding.web.controller.UserController.validate("
                + "it.pagopa.selfcare.onboarding.web.model.UserDataValidationDto)");
        log.trace("validate start");
        log.debug(LogUtils.CONFIDENTIAL_MARKER, "validate request = {}", LogUtils.sanitize(request));
        return userService.validate(userResourceMapper.toUser(request))
                .invoke(() -> log.trace("validate end"))
                .replaceWith(() -> Response.noContent().build());
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
    public Uni<Response> onboarding(@RequestBody(content = @Content(mediaType = MediaType.APPLICATION_JSON))
                                   @Valid OnboardingUserDto request) {
        RequestParams.requiredBody(request, "public void it.pagopa.selfcare.onboarding.web.controller.UserController.onboarding("
                + "it.pagopa.selfcare.onboarding.web.model.OnboardingUserDto)");
        log.trace("onboarding start");
        log.debug("onboarding request = {}", LogUtils.sanitize(request));
        return userService.onboardingUsers(onboardingResourceMapper.toEntity(request))
                .invoke(() -> log.trace("onboarding end"))
                .replaceWith(() -> Response.status(Response.Status.CREATED).build());
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
    public Uni<Response> onboardingAggregator(@RequestBody(content = @Content(mediaType = MediaType.APPLICATION_JSON))
                                             @Valid OnboardingUserDto request) {
        RequestParams.requiredBody(request, "public void it.pagopa.selfcare.onboarding.web.controller.UserController.onboardingAggregator("
                + "it.pagopa.selfcare.onboarding.web.model.OnboardingUserDto)");
        log.trace("onboardingAggregator start");
        log.debug("onboardingAggregator request = {}", Encode.forJava(request.toString()));
        return userService.onboardingUsersAggregator(onboardingResourceMapper.toEntity(request))
                .invoke(() -> log.trace("onboardingAggregator end"))
                .replaceWith(() -> Response.status(Response.Status.CREATED).build());
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
    public Uni<CheckManagerResponse> checkManager(@RequestBody(content = @Content(mediaType = MediaType.APPLICATION_JSON))
                                                 @Valid CheckManagerDto request) {
        RequestParams.requiredBody(request, "public it.pagopa.selfcare.onboarding.web.model.CheckManagerResponse "
                + "it.pagopa.selfcare.onboarding.web.controller.UserController.checkManager("
                + "it.pagopa.selfcare.onboarding.web.model.CheckManagerDto)");
        log.trace("checkManager start");
        return userService.checkManager(onboardingResourceMapper.toCheckManagerData(request))
                .map(CheckManagerResponse::new)
                .invoke(response -> log.trace("checkManager end"));
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
    public Uni<ManagerInfoResponse> getManagerInfo(@PathParam("onboardingId") String onboardingId) {
        log.trace("getManagerInfo start");
        String fiscalCode = SecurityIdentityUtils.getFiscalCode(securityIdentity);
        return userService.getManagerInfo(onboardingId, fiscalCode)
                .map(userResourceMapper::toManagerInfoResponse)
                .invoke(response -> log.trace("getManagerInfo end"));
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
    public Uni<UserId> searchUser(@RequestBody(content = @Content(mediaType = MediaType.APPLICATION_JSON))
                                 @Valid UserTaxCodeDto request) {
        RequestParams.requiredBody(request, "public it.pagopa.selfcare.onboarding.connector.model.user.UserId "
                + "it.pagopa.selfcare.onboarding.web.controller.UserController.searchUser("
                + "it.pagopa.selfcare.onboarding.web.model.UserTaxCodeDto)");
        log.trace("searchUser start");
        return userService.searchUser(userResourceMapper.toString(request))
                .invoke(userId -> log.trace("searchUser end"));
    }
}
