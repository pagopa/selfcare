package it.pagopa.selfcare.onboarding.web.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiParam;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import it.pagopa.selfcare.commons.base.logging.LogUtils;
import it.pagopa.selfcare.commons.base.security.SelfCareUser;
import it.pagopa.selfcare.commons.web.model.Problem;
import it.pagopa.selfcare.commons.web.security.JwtAuthenticationToken;
import it.pagopa.selfcare.onboarding.connector.exceptions.InvalidRequestException;
import it.pagopa.selfcare.onboarding.connector.model.OnboardingResult;
import it.pagopa.selfcare.onboarding.core.InstitutionService;
import it.pagopa.selfcare.onboarding.web.model.*;
import it.pagopa.selfcare.onboarding.web.model.mapper.InstitutionResourceMapper;
import it.pagopa.selfcare.onboarding.web.model.mapper.OnboardingResourceMapper;
import it.pagopa.selfcare.onboarding.web.utils.FileValidationUtils;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.owasp.encoder.Encode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.security.Principal;
import java.util.List;

import static org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON_VALUE;

@Slf4j
@RestController
@RequestMapping(value = "/v2/institutions", produces = MediaType.APPLICATION_JSON_VALUE)
@Api(tags = "institutions")
public class InstitutionV2Controller {

    private final InstitutionService institutionService;
    private final OnboardingResourceMapper onboardingResourceMapper;
    private final InstitutionResourceMapper institutionMapper;
    private static final String ONBOARDING_START = "onboarding start";
    private static final String ONBOARDING_END = "onboarding end";

    @Autowired
    public InstitutionV2Controller(InstitutionService institutionService,
                                   OnboardingResourceMapper onboardingResourceMapper,
                                   InstitutionResourceMapper institutionMapper) {
        this.institutionService = institutionService;
        this.onboardingResourceMapper = onboardingResourceMapper;
        this.institutionMapper = institutionMapper;
    }

    @ApiResponse(responseCode = "403",
            description = "Forbidden",
            content = {
                    @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = Problem.class))
            })
    @PostMapping(value = "/onboarding")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "${swagger.onboarding.institutions.api.onboarding.subunit}",
            description = "${swagger.onboarding.institutions.api.onboarding.subunit}", operationId = "institutionOnboarding")
    public void onboarding(@RequestBody @Valid OnboardingProductDto request) {
        log.trace(ONBOARDING_START);
        log.debug("onboarding request = {}", request);
        institutionService.validateOnboardingByProductOrInstitutionTaxCode(request.getTaxCode(), request.getProductId());
        if (Boolean.TRUE.equals(request.getIsAggregator())) {
            institutionService.onboardingPaAggregator(onboardingResourceMapper.toEntity(request));
        } else {
            institutionService.onboardingProductV2(onboardingResourceMapper.toEntity(request));
        }
        log.trace(ONBOARDING_END);
    }

    @ApiResponse(responseCode = "403",
            description = "Forbidden",
            content = {
                    @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = Problem.class))
            })
    @PostMapping(value = "/company/onboarding")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "${swagger.onboarding.institutions.api.onboarding.subunit}",
            description = "${swagger.onboarding.institutions.api.onboarding.subunit}", operationId = "institutionOnboardingCompany")
    public void onboarding(@RequestBody @Valid CompanyOnboardingDto request, Principal principal) {
        log.trace(ONBOARDING_START);
        log.debug("onboarding request = {}", Encode.forJava(request.toString()));
        JwtAuthenticationToken jwtAuthenticationToken = (JwtAuthenticationToken) principal;
        SelfCareUser selfCareUser = (SelfCareUser) jwtAuthenticationToken.getPrincipal();
        institutionService.onboardingCompanyV2(onboardingResourceMapper.toEntity(request), selfCareUser.getFiscalCode());
        log.trace(ONBOARDING_END);
    }

    @ApiResponse(responseCode = "403",
            description = "Forbidden",
            content = {
                    @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = Problem.class))
            })
    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "${swagger.onboarding.institutions.api.onboarding.subunit}",
            description = "${swagger.onboarding.institutions.api.onboarding.subunit}", operationId = "v2GetInstitutionByFilters")
    public List<InstitutionResource> getInstitution(@ApiParam("${swagger.onboarding.institutions.model.productFilter}")
                                                    @RequestParam(value = "productId")
                                                    String productId,
                                                    @ApiParam("${swagger.onboarding.institutions.model.taxCode}")
                                                    @RequestParam(value = "taxCode", required = false)
                                                    String taxCode,
                                                    @ApiParam("${swagger.onboarding.institutions.model.origin}")
                                                    @RequestParam(value = "origin", required = false)
                                                    String origin,
                                                    @ApiParam("${swagger.onboarding.institutions.model.originId}")
                                                    @RequestParam(value = "originId", required = false)
                                                    String originId,
                                                    @ApiParam("${swagger.onboarding.institutions.model.subunitCode}")
                                                    @RequestParam(value = "subunitCode", required = false)
                                                    String subunitCode) {
        log.trace("getInstitution start");
        final List<InstitutionResource> institutions = institutionService.getByFilters(productId, taxCode, origin, originId, subunitCode)
                .stream()
                .map(institutionMapper::toResource)
                .toList();
        log.debug(LogUtils.CONFIDENTIAL_MARKER, "getInstitution result = {}", institutions);
        log.trace("getInstitution end");
        return institutions;
    }

    @PostMapping(value = "/onboarding/aggregation/verification")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "${swagger.onboarding.institutions.api.onboarding.verifyAggregatesCsv}",
            description = "${swagger.onboarding.institutions.api.onboarding.verifyAggregatesCsv}",  operationId = "verifyAggregatesCsvUsingPOST")
    public VerifyAggregatesResponse verifyAggregatesCsv(@RequestParam("aggregates") MultipartFile file,
                                                        @RequestParam(value = "institutionType", required = false) String institutionType,
                                                        @RequestParam("productId") String productId){
        log.trace("Verify Aggregates Csv start");
        log.debug("Verify Aggregates Csv start for productId {}", productId);

        FileValidationUtils.validateAggregatesFile(file);
        VerifyAggregatesResponse response = onboardingResourceMapper.toVerifyAggregatesResponse(institutionService.validateAggregatesCsv(file, productId));
        log.trace("Verify Aggregates Csv end");
        return response;
    }

    @PostMapping(value = "/company/verify-manager")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "${swagger.onboarding.institutions.api.onboarding.verifyManager}",
            description = "${swagger.onboarding.institutions.api.onboarding.verifyManager}", operationId = "verifyManagerUsingPOST")
    public VerifyManagerResponse verifyManager(
            @RequestBody @Valid VerifyManagerRequest request,
            Principal principal
    ) {
        log.trace("verifyManager start");
        JwtAuthenticationToken jwtAuthenticationToken = (JwtAuthenticationToken) principal;
        SelfCareUser selfCareUser = (SelfCareUser) jwtAuthenticationToken.getPrincipal();

        VerifyManagerResponse response = onboardingResourceMapper.toManagerVerification(institutionService.verifyManager(selfCareUser.getFiscalCode(), request.getCompanyTaxCode()));
        log.trace("verifyManager end");
        return response;
    }

    @GetMapping(value = "/onboarding/active")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "${swagger.onboarding.institutions.api.onboarding.getActiveOnboarding}",
            description = "${swagger.onboarding.institutions.api.onboarding.getActiveOnboarding}", operationId = "getActiveOnboardingUsingGET")
    public List<InstitutionOnboardingResource> getActiveOnboarding(@RequestParam("taxCode") String taxCode,
                                                                   @RequestParam("productId") String productId,
                                                                   @RequestParam(value = "subunitCode", required = false) String subunitCode
    ) {
        log.trace("getActiveOnboarding start");
        log.debug("getActiveOnboarding taxCode = {}, productId = {}", Encode.forJava(taxCode), Encode.forJava(productId));
        if ((StringUtils.isBlank(taxCode) || StringUtils.isBlank(productId)))
            throw new InvalidRequestException("taxCode and/or productId must not be blank! ");
        List<InstitutionOnboardingResource> response = institutionService.getActiveOnboarding(taxCode, productId,subunitCode)
                .stream()
                .map(onboardingResourceMapper::toOnboardingResource)
                .toList();
        log.debug("getActiveOnboarding result = {}", response);
        log.trace("getActiveOnboarding end");
        return response;
    }

    @GetMapping(value = "/onboarding/recipient-code/verification")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "${swagger.onboarding.institutions.api.onboarding.checkRecipientCode}",
            description = "${swagger.onboarding.institutions.api.onboarding.checkRecipientCode}", operationId = "checkRecipientCodeUsingGET")
    public RecipientCodeStatus checkRecipientCode(@RequestParam(value = "originId") String originId,
                                                  @RequestParam(value = "recipientCode") String recipientCode) {
        log.trace("Check recipientCode start");
        log.debug("Check originId start for institution with originId {} and recipientCode {}", originId, recipientCode);
        RecipientCodeStatus response = onboardingResourceMapper.toRecipientCodeStatus(institutionService.checkRecipientCode(originId, recipientCode));
        log.trace("Check recipientCode end");
        return response;
    }

    @PostMapping(value = "/onboarding/users/pg")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "${swagger.onboarding.institutions.api.onboardingUsersPg}",
            description = "${swagger.onboarding.institutions.api.onboardingUsersPg}", operationId = "onboardingUsersPgUsingPOST")
    public void onboardingUsers(@Valid @RequestBody(required = false) CompanyOnboardingUserDto companyOnboardingUserDto) {
        log.trace("onboardingUsersPgFromIcAndAde start");
        log.debug("onboardingUsersPgFromIcAndAde request = {}", Encode.forJava(companyOnboardingUserDto.toString()));
        institutionService.onboardingUsersPgFromIcAndAde(onboardingResourceMapper.toEntity(companyOnboardingUserDto));
        log.trace("onboardingUsersPgFromIcAndAde end");
    }

    @GetMapping(value = "/onboardings")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "${swagger.onboarding.institutions.api.onboardingInfo.summary}",
            description = "${swagger.onboarding.institutions.api.onboardingInfo.description}", operationId = "getOnboardingInfo")
    public List<OnboardingResult> getOnboardingsInfo(@RequestParam(value = "taxCode") String inputTaxCode,
                                                     @RequestParam(value = "status") String inputStatus) {
        log.trace("onboardingInfo start");
        String taxCode = Encode.forJava(inputTaxCode);
        String status = Encode.forJava(inputStatus);
        log.debug("onboardingInfo request = {} - {}", taxCode, status);
        List<OnboardingResult> results = institutionService.getOnboardingWithFilter(taxCode, status);
        log.trace("onboardingInfo end");
        return results;
    }

}
