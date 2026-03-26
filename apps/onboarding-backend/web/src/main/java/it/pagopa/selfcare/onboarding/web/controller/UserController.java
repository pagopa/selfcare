package it.pagopa.selfcare.onboarding.web.controller;

import static org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON_VALUE;

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
import jakarta.validation.Valid;
import java.security.Principal;
import lombok.extern.slf4j.Slf4j;
import org.owasp.encoder.Encode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping(value = "/v1/users", produces = MediaType.APPLICATION_JSON_VALUE)
@Api(tags = "user")
public class UserController {

    private final UserService userService;
    private final OnboardingResourceMapper onboardingResourceMapper;
    private final UserResourceMapper userResourceMapper;

    @Autowired
    public UserController(UserService userService,
                          OnboardingResourceMapper onboardingResourceMapper,
                          UserResourceMapper userResourceMapper) {
        this.userService = userService;
        this.onboardingResourceMapper = onboardingResourceMapper;
        this.userResourceMapper = userResourceMapper;
    }

    @PostMapping("/validate")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @ApiResponse(responseCode = "409",
            description = "Conflict",
            content = {
                    @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = Problem.class))
            })
    @Operation(summary = "${swagger.onboarding.user.api.validate}",
            description = "${swagger.onboarding.user.api.validate}", operationId = "validateUsingPOST")
    public void validate(@RequestBody @Valid UserDataValidationDto request) {
        log.trace("validate start");
        log.debug(LogUtils.CONFIDENTIAL_MARKER, "validate request = {}", request);
        userService.validate(userResourceMapper.toUser(request));
        log.trace("validate end");
    }

    @ApiResponse(responseCode = "403",
            description = "Forbidden",
            content = {
                    @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = Problem.class))
            })
    @PostMapping(value = "/onboarding")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary= "${swagger.onboarding.users.api.onboarding}",
            description = "${swagger.onboarding.users.api.onboarding}", operationId = "onboardingUsers")
    public void onboarding(@RequestBody @Valid OnboardingUserDto request) {
        log.trace("onboarding start");
        log.debug("onboarding request = {}", request);
        userService.onboardingUsers(onboardingResourceMapper.toEntity(request));
        log.trace("onboarding end");
    }


    @ApiResponse(responseCode = "403",
            description = "Forbidden",
            content = {
                    @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = Problem.class))
            })
    @PostMapping(value = "/onboarding/aggregator")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "${swagger.onboarding.users.api.onboarding-aggregator}",
            description = "${swagger.onboarding.users.api.onboarding-aggregator}", operationId = "onboardingAggregatorUsingPOST")
    public void onboardingAggregator(@RequestBody @Valid OnboardingUserDto request) {
        log.trace("onboardingAggregator start");
        log.debug("onboardingAggregator request = {}", Encode.forJava(request.toString()));
        userService.onboardingUsersAggregator(onboardingResourceMapper.toEntity(request));
        log.trace("onboardingAggregator end");
    }

    @ApiResponse(responseCode = "403",
            description = "Forbidden",
            content = {
                    @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = Problem.class))
            })
    @PostMapping(value = "/check-manager")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "${swagger.onboarding.users.api.check-manager}",
            description = "${swagger.onboarding.users.api.check-manager}", operationId = "checkManager")
    public CheckManagerResponse checkManager(@RequestBody @Valid CheckManagerDto request) {
        log.trace("checkManager start");
        boolean checkManager =  userService.checkManager(onboardingResourceMapper.toCheckManagerData(request));
        log.trace("checkManager end");
        return new CheckManagerResponse(checkManager);
    }

    @ApiResponse(responseCode = "403",
            description = "Forbidden",
            content = {
                    @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = Problem.class))
            })
    @GetMapping(value = "/onboarding/{onboardingId}/manager")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "${swagger.onboarding.users.api.check-manager}",
            description = "${swagger.onboarding.users.api.check-manager}", operationId = "getManagerInfo")
    public ManagerInfoResponse getManagerInfo(@PathVariable("onboardingId") String onboardingId, Principal principal) {
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
                    @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = Problem.class))
            })
    @PostMapping(value = "/search-user")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "${swagger.onboarding.users.api.search-user}",
            description = "${swagger.onboarding.users.api.search-user}", operationId = "searchUserId")
    public UserId searchUser(@RequestBody @Valid UserTaxCodeDto request) {
        log.trace("searchUser start");
        UserId userId =  userService.searchUser(userResourceMapper.toString(request));
        log.trace("searchUser end");
        return userId;
    }
}
