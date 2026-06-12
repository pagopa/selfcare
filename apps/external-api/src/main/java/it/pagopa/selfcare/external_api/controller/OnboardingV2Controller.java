package it.pagopa.selfcare.external_api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import it.pagopa.selfcare.commons.base.security.SelfCareUser;
import it.pagopa.selfcare.commons.web.model.Problem;
import it.pagopa.selfcare.external_api.mapper.OnboardingResourceMapper;
import it.pagopa.selfcare.external_api.mapper.RelationshipMapper;
import it.pagopa.selfcare.external_api.model.onboarding.*;
import it.pagopa.selfcare.external_api.model.user.RelationshipInfo;
import it.pagopa.selfcare.external_api.model.user.RelationshipResult;
import it.pagopa.selfcare.external_api.service.OnboardingService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON_VALUE;

@Slf4j
@RestController
@RequestMapping(value = "/v2/onboarding", produces = APPLICATION_JSON_VALUE)
@Tag(name = "Onboarding")
public class OnboardingV2Controller {

  private final OnboardingService onboardingService;
  private final OnboardingResourceMapper onboardingResourceMapper;

  @Autowired
  public OnboardingV2Controller(
      OnboardingService onboardingService, OnboardingResourceMapper onboardingResourceMapper) {
    this.onboardingService = onboardingService;
    this.onboardingResourceMapper = onboardingResourceMapper;
  }

  @Tag(name = "internal-v1")
  @ApiResponse(
      responseCode = "403",
      description = "Forbidden",
      content = {
        @Content(
            mediaType = APPLICATION_PROBLEM_JSON_VALUE,
            schema = @Schema(implementation = Problem.class))
      })
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(summary = "onboarding", description = "${swagger.onboarding.institutions.api.onboarding.subunit}")
  public void onboarding(@RequestBody @Valid OnboardingProductDto request) {
    log.trace("onboarding start");
    log.debug("onboarding request = {}", request);
    onboardingService.autoApprovalOnboardingProductV2(onboardingResourceMapper.toEntity(request));
    log.trace("onboarding end");
  }

  @Tag(name = "internal-v1")
  @ApiResponse(
      responseCode = "403",
      description = "Forbidden",
      content = {
        @Content(
            mediaType = APPLICATION_PROBLEM_JSON_VALUE,
            schema = @Schema(implementation = Problem.class))
      })
  @PostMapping(value = "/import")
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(summary = "onboardingImport", description = "${swagger.onboarding.institutions.api.onboarding.import}")
  public void onboardingImport(@RequestBody @Valid OnboardingImportProductDto request) {
    log.trace("onboardingImport start");
    log.debug("onboardingImport request = {}", request);
    onboardingService.autoApprovalOnboardingImportProductV2(
        onboardingResourceMapper.toEntity(request));
    log.trace("onboardingImport end");
  }

  /**
   * The function persist user on registry if not exists and add relation with institution-product
   *
   * @param request OnboardingInstitutionUsersRequest
   * @return no content * Code: 204, Message: successful operation * Code: 404, Message: Not found,
   *     DataType: Problem * Code: 400, Message: Invalid request, DataType: Problem
   */
  @ResponseStatus(HttpStatus.OK)
  @Tag(name = "internal-v1")
  @Tag(name = "support")
  @Tag(name = "support-pnpg")
  @Tag(name = "Onboarding")
  @Operation(
      summary = "${swagger.mscore.onboarding.users}",
      description = "${swagger.mscore.onboarding.users}")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "OK",
            content =
                @Content(
                    array =
                        @ArraySchema(schema = @Schema(implementation = RelationshipResult.class)),
                    mediaType = "application/json")),
        @ApiResponse(
            responseCode = "400",
            description = "Bad Request",
            content =
                @Content(
                    schema = @Schema(implementation = Problem.class),
                    mediaType = "application/problem+json")),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized",
            content =
                @Content(
                    schema = @Schema(implementation = Problem.class),
                    mediaType = "application/problem+json")),
        @ApiResponse(
            responseCode = "403",
            description = "Forbidden",
            content =
                @Content(
                    schema = @Schema(implementation = Problem.class),
                    mediaType = "application/problem+json")),
        @ApiResponse(
            responseCode = "404",
            description = "Not Found",
            content =
                @Content(
                    schema = @Schema(implementation = Problem.class),
                    mediaType = "application/problem+json"))
      })
  @PostMapping(value = "/users")
  public ResponseEntity<List<RelationshipResult>> onboardingInstitutionUsers(
      @RequestBody @Valid OnboardingInstitutionUsersRequest request,
      Authentication authentication) {
    SelfCareUser selfCareUser = (SelfCareUser) authentication.getPrincipal();
    List<RelationshipInfo> response =
        onboardingService.onboardingUsers(
            onboardingResourceMapper.toOnboardingUsersRequest(request),
            selfCareUser.getUserName(),
            selfCareUser.getSurname());
    return ResponseEntity.ok().body(RelationshipMapper.toRelationshipResultList(response));
  }

  @Tag(name = "internal-v1")
  @ApiResponse(
      responseCode = "403",
      description = "Forbidden",
      content = {
        @Content(
            mediaType = APPLICATION_PROBLEM_JSON_VALUE,
            schema = @Schema(implementation = Problem.class))
      })
  @PostMapping(value = "/aggregation/{taxCode}")
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(
      summary = "onboardingAggregateImport",
      description = "${swagger.onboarding.institutions.api.onboarding.aggregate.import}", operationId = "#onboardingAggregateImport")
  public void onboardingAggregateImport(
      @PathVariable("taxCode") String taxCode,
      @RequestBody @Valid OnboardingAggregatorImportDto request) {
    log.trace("onboardingAggregateImport start");

    OnboardingAggregatorImportData input =
        onboardingService.onboardingAggregatorImportBuildRequest(request, taxCode);

    onboardingService.aggregationImport(
        onboardingResourceMapper.toOnboardingAggregationImport(input));

    log.trace("onboardingAggregateImport end");
  }
}
