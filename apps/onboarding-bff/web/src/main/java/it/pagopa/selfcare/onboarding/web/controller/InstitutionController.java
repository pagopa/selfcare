package it.pagopa.selfcare.onboarding.web.controller;

import static it.pagopa.selfcare.commons.base.utils.ProductId.PROD_FD;
import static it.pagopa.selfcare.commons.base.utils.ProductId.PROD_FD_GARANTITO;
import static org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON_VALUE;
import static org.springframework.web.bind.annotation.RequestMethod.HEAD;

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
import it.pagopa.selfcare.onboarding.connector.model.InstitutionLegalAddressData;
import it.pagopa.selfcare.onboarding.connector.model.InstitutionOnboardingData;
import it.pagopa.selfcare.onboarding.connector.model.institutions.MatchInfoResult;
import it.pagopa.selfcare.onboarding.connector.model.institutions.infocamere.InstitutionInfoIC;
import it.pagopa.selfcare.onboarding.core.InstitutionService;
import it.pagopa.selfcare.onboarding.web.model.*;
import it.pagopa.selfcare.onboarding.web.model.mapper.*;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.owasp.encoder.Encode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping(value = "/v1/institutions", produces = MediaType.APPLICATION_JSON_VALUE)
@Api(tags = "institutions")
public class InstitutionController {

    private final InstitutionService institutionService;
    private final OnboardingResourceMapper onboardingResourceMapper;
    private final InstitutionResourceMapper institutionMapper;
    private final UserResourceMapper userMapper;
    private final OnboardingInstitutionInfoMapper onboardingInstitutionInfoMapper;
    private static final String ONBOARDING_START = "onboarding start";
    private static final String ONBOARDING_END = "onboarding end";
    private final GeographicTaxonomyMapper geographicTaxonomyMapper;

    @Autowired
    public InstitutionController(InstitutionService institutionService,
                                 OnboardingResourceMapper onboardingResourceMapper,
                                 OnboardingInstitutionInfoMapper onboardingInstitutionInfoMapper,
                                 GeographicTaxonomyMapper geographicTaxonomyMapper,
                                 InstitutionResourceMapper institutionMapper,
                                 UserResourceMapper userMapper) {
        this.institutionService = institutionService;
        this.onboardingResourceMapper = onboardingResourceMapper;
        this.onboardingInstitutionInfoMapper = onboardingInstitutionInfoMapper;
        this.geographicTaxonomyMapper = geographicTaxonomyMapper;
        this.institutionMapper = institutionMapper;
        this.userMapper = userMapper;
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
            description = "${swagger.onboarding.institutions.api.onboarding.subunit}", operationId = "onboardingUsingPOST")
    public void onboarding(@RequestBody @Valid OnboardingProductDto request) {
        log.trace(ONBOARDING_START);
        log.debug("onboarding request = {}", request);
        institutionService.onboardingProduct(onboardingResourceMapper.toEntity(request));
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
            description = "${swagger.onboarding.institutions.api.onboarding.subunit}", operationId = "onboardingCompanyUsingPOST")
    public void onboarding(@RequestBody @Valid CompanyOnboardingDto request) {
        log.trace(ONBOARDING_START);
        log.debug("onboarding request = {}", request);
        institutionService.onboardingProduct(onboardingResourceMapper.toEntity(request));
        log.trace(ONBOARDING_END);
    }

    @GetMapping(value = "/onboarding/")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "${swagger.onboarding.institutions.api.getInstitutionOnboardingInfo}",
            description = "${swagger.onboarding.institutions.api.getInstitutionOnboardingInfo}", operationId = "getInstitutionOnboardingInfoUsingGET")
    public InstitutionOnboardingInfoResource getInstitutionOnboardingInfoById(@ApiParam("${swagger.onboarding.institutions.model.id}")
                                                                          @RequestParam("institutionId")
                                                                          String institutionId,
                                                                          @ApiParam("${swagger.onboarding.product.model.id}")
                                                                          @RequestParam("productId")
                                                                          String productId) {
        log.trace("getInstitutionOnboardingInfoById start");
        log.debug("getInstitutionOnboardingInfoById institutionId = {}, productId = {}", Encode.forJava(institutionId), Encode.forJava(productId));
        InstitutionOnboardingData institutionOnboardingData = institutionService.getInstitutionOnboardingDataById(institutionId, productId);
        InstitutionOnboardingInfoResource result = onboardingInstitutionInfoMapper.toResource(institutionOnboardingData);
        log.debug("getInstitutionOnboardingInfoById result = {}", result);
        log.trace("getInstitutionOnboardingInfoById end");
        return result;
    }

    @GetMapping(value = "/{externalInstitutionId}/geographic-taxonomy")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "${swagger.onboarding.institutions.api.getInstitutionGeographicTaxonomy}",
            description = "${swagger.onboarding.institutions.api.getInstitutionGeographicTaxonomy}", operationId = "getInstitutionGeographicTaxonomyUsingGET")
    public List<GeographicTaxonomyResource> getInstitutionGeographicTaxonomy(@ApiParam("${swagger.onboarding.institutions.model.externalId}")
                                                                             @PathVariable("externalInstitutionId")
                                                                             String externalInstitutionId) {
        log.trace("getInstitutionGeographicTaxonomy start");
        log.debug("getInstitutionGeographicTaxonomy institutionId = {}", externalInstitutionId);
        List<GeographicTaxonomyResource> geographicTaxonomies = institutionService.getGeographicTaxonomyList(externalInstitutionId)
                .stream()
                .map(geographicTaxonomyMapper::toResource)
                .toList();
        log.debug("getInstitutionGeographicTaxonomy result = {}", geographicTaxonomies);
        log.trace("getInstitutionGeographicTaxonomy end");
        return geographicTaxonomies;
    }

    @GetMapping(value = "/geographic-taxonomies")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "${swagger.onboarding.institutions.api.getInstitutionGeographicTaxonomy}",
            description = "${swagger.onboarding.institutions.api.getInstitutionGeographicTaxonomy}", operationId = "getGeographicTaxonomiesByTaxCodeAndSubunitCodeUsingGET")
    public List<GeographicTaxonomyResource> getGeographicTaxonomiesByTaxCodeAndSubunitCode(@ApiParam("${swagger.onboarding.institutions.model.taxCode}")
                                                                                           @RequestParam("taxCode")
                                                                                           String taxCode,
                                                                                           @ApiParam("${swagger.onboarding.institutions.model.subunitCode}")
                                                                                           @RequestParam(value = "subunitCode", required = false)
                                                                                           String subunitCode) {
        log.trace("getGeographicTaxonomiesByTaxCodeAndSubunitCode start");
        log.debug("getGeographicTaxonomiesByTaxCodeAndSubunitCode taxCode = {}, subunitCode = {}", taxCode, subunitCode);
        if (StringUtils.isBlank(taxCode) || (Objects.nonNull(subunitCode) && StringUtils.isBlank(subunitCode)))
            throw new InvalidRequestException("taxCode and/or subunitCode must not be blank! ");

        List<GeographicTaxonomyResource> geographicTaxonomies = institutionService.getGeographicTaxonomyList(taxCode, subunitCode)
                .stream()
                .map(geographicTaxonomyMapper::toResource)
                .toList();
        log.debug("getGeographicTaxonomiesByTaxCodeAndSubunitCode result = {}", geographicTaxonomies);
        log.trace("getGeographicTaxonomiesByTaxCodeAndSubunitCode end");
        return geographicTaxonomies;
    }

    @GetMapping("")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "${swagger.onboarding.institutions.api.getInstitutions}",
            description = "${swagger.onboarding.institutions.api.getInstitutions}", operationId = "getInstitutionsUsingGET")
    public List<InstitutionResource> getInstitutions(@ApiParam("${swagger.onboarding.institutions.model.productFilter}")
                                                     @RequestParam(value = "productId", required = false)
                                                     String productId,
                                                     Principal principal) {
        log.trace("getInstitutions start");
        JwtAuthenticationToken jwtAuthenticationToken = (JwtAuthenticationToken) principal;
        SelfCareUser selfCareUser = (SelfCareUser) jwtAuthenticationToken.getPrincipal();

        List<InstitutionResource> institutionResources = institutionService.getInstitutions(productId, selfCareUser.getId())
                .stream()
                .map(institutionMapper::toResource)
                .toList();
        log.debug("getInstitutions result = {}", institutionResources);
        log.trace("getInstitutions end");
        return institutionResources;
    }


    @ApiResponse(responseCode = "403",
            description = "Forbidden",
            content = {
                    @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = Problem.class))
            })
    @RequestMapping(method = HEAD, value = "/{externalInstitutionId}/products/{productId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "${swagger.onboarding.institutions.api.verifyOnboarding}",
            description = "${swagger.onboarding.institutions.api.verifyOnboarding}", operationId = "verifyOnboardingProductUsingHEAD")
    public void verifyOnboarding(@ApiParam("${swagger.onboarding.institutions.model.externalId}")
                                 @PathVariable("externalInstitutionId")
                                 String externalInstitutionId,
                                 @ApiParam("${swagger.onboarding.product.model.id}")
                                 @PathVariable("productId")
                                 String productId) {
        log.trace("verifyOnboarding start");
        log.debug("verifyOnboarding externalInstitutionId = {}, productId = {}", externalInstitutionId, productId);
        institutionService.verifyOnboarding(externalInstitutionId, productId);
        log.trace("verifyOnboarding end");
    }

    @ApiResponse(responseCode = "403",
            description = "Forbidden",
            content = {
                    @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = Problem.class))
            })
    @RequestMapping(method = HEAD, value = "/onboarding")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "${swagger.onboarding.institutions.api.verifyOnboarding}",
            description = "${swagger.onboarding.institutions.api.verifyOnboarding}", operationId = "verifyOnboardingUsingHEAD")
    public void verifyOnboarding(@ApiParam("${swagger.onboarding.institutions.model.taxCode}")
                                     @RequestParam(value = "taxCode", required = false)
                                     String taxCode,
                                 @ApiParam("${swagger.onboarding.institutions.model.subunitCode}")
                                     @RequestParam(value = "subunitCode", required = false)
                                     String subunitCode,
                                 @ApiParam("${swagger.onboarding.product.model.id}")
                                     @RequestParam("productId")
                                     String productId,
                                 @ApiParam("${swagger.onboarding.institutions.model.origin}")
                                     @RequestParam(value = "origin", required = false)
                                     String origin,
                                 @ApiParam("${swagger.onboarding.institutions.model.originId}")
                                     @RequestParam(value = "originId", required = false)
                                     String originId,
                                 @ApiParam("${swagger.onboarding.institutions.model.vatNumber}")
                                     @RequestParam(value = "vatNumber", required = false)
                                     Optional<String> vatNumber,
                                 @ApiParam("${swagger.onboarding.institutions.model.institutionType}")
                                     @RequestParam(value = "institutionType", required = false)
                                     String institutionType,
                                 @ApiParam("${swagger.onboarding.institutions.model.verifyType}")
                                     @RequestParam(value = "verifyType", required = false) VerifyType type) {
        log.trace("verifyOnboarding start");
        if (VerifyType.EXTERNAL.equals(type) && vatNumber.isPresent() && (PROD_FD.getValue().equals(productId) || PROD_FD_GARANTITO.getValue().equals(productId))) {
            institutionService.checkOrganization(productId, taxCode, vatNumber.get());
        } else
            institutionService.verifyOnboarding(productId, taxCode, origin, originId, subunitCode, institutionType);
        log.trace("verifyOnboarding end");
    }

    @GetMapping(value = "/from-infocamere/")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "${swagger.onboarding.institutions.api.getInstitutionsByUser}",
            description = "${swagger.onboarding.institutions.api.getInstitutionsByUser}", operationId = "getInstitutionsFromInfocamereUsingGET")
    public InstitutionResourceIC getInstitutionsFromInfocamere(Principal principal) {
        log.trace("getInstitutionsFromInfocamere start");

        JwtAuthenticationToken jwtAuthenticationToken = (JwtAuthenticationToken) principal;
        SelfCareUser selfCareUser = (SelfCareUser) jwtAuthenticationToken.getPrincipal();

        InstitutionInfoIC institutionInfoIC = institutionService.getInstitutionsByUser(selfCareUser.getFiscalCode());
        InstitutionResourceIC institutionResourceIC = institutionMapper.toResource(institutionInfoIC);
        log.debug(LogUtils.CONFIDENTIAL_MARKER, "getInstitutionsFromInfocamere result = {}", institutionResourceIC);
        log.trace("getInstitutionsFromInfocamere end");
        return institutionResourceIC;
    }

    @PostMapping(value = "/verification/match")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "${swagger.onboarding.institutions.api.matchInstitutionAndUser}",
            description = "${swagger.onboarding.institutions.api.matchInstitutionAndUser}", operationId = "postVerificationMatchUsingPOST")
    public MatchInfoResultResource postVerificationMatch(@RequestBody
                                                         @Valid
                                                         VerificationMatchRequest verificationMatchRequest) {
        log.trace("matchInstitutionAndUser start");
        log.debug(LogUtils.CONFIDENTIAL_MARKER, "matchInstitutionAndUser userDto = {}", verificationMatchRequest);
        MatchInfoResult matchInfoResult = institutionService.matchInstitutionAndUser(verificationMatchRequest.getTaxCode(),
                userMapper.toUser(verificationMatchRequest.getUserDto()));
        MatchInfoResultResource result = institutionMapper.toResource(matchInfoResult);
        log.debug("matchInstitutionAndUser result = {}", result);
        log.trace("matchInstitutionAndUser end");
        return result;
    }

    @PostMapping(value = "/verification/legal-address")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "${swagger.onboarding.institutions.api.getInstitutionLegalAddress}",
            description = "${swagger.onboarding.institutions.api.getInstitutionLegalAddress}", operationId = "postVerificationLegalAddressUsingPOST")
    public InstitutionLegalAddressResource postVerificationLegalAddress(@RequestBody @Valid VerificationLegalAddressRequest verificationLegalAddressRequest) {
        log.trace("getInstitutionLegalAddress start");
        log.debug(LogUtils.CONFIDENTIAL_MARKER, "getInstitutionLegalAddress institutionId = {}", verificationLegalAddressRequest.getTaxCode());
        InstitutionLegalAddressData institutionLegalAddressData = institutionService.getInstitutionLegalAddress(verificationLegalAddressRequest.getTaxCode());
        InstitutionLegalAddressResource result = onboardingResourceMapper.toResource(institutionLegalAddressData);
        log.debug("getInstitutionLegalAddress result = {}", result);
        log.trace("getInstitutionLegalAddress end");
        return result;
    }

    /**
     * @param externalInstitutionId
     * @deprecated [reference SELC-2815]
     */
    @Deprecated(forRemoval = true)
    @GetMapping(value = "/{externalInstitutionId}/products/{productId}/onboarded-institution-info")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "${swagger.onboarding.institutions.api.getInstitutionOnboardingInfo}",
            description = "${swagger.onboarding.institutions.api.getInstitutionOnboardingInfo}", operationId = "getInstitutionOnboardingInfoUsingGET_1")
    public InstitutionOnboardingInfoResource getInstitutionOnboardingInfo(@ApiParam("${swagger.onboarding.institutions.model.externalId}")
                                                                          @PathVariable("externalInstitutionId")
                                                                          String externalInstitutionId,
                                                                          @ApiParam("${swagger.onboarding.product.model.id}")
                                                                          @PathVariable("productId")
                                                                          String productId) {
        log.trace("getInstitutionOnBoardingInfo start");
        log.debug("getInstitutionOnBoardingInfo institutionId = {}, productId = {}", externalInstitutionId, productId);
        InstitutionOnboardingData institutionOnboardingData = institutionService.getInstitutionOnboardingData(externalInstitutionId, productId);
        InstitutionOnboardingInfoResource result = onboardingInstitutionInfoMapper.toResource(institutionOnboardingData);
        log.debug("getInstitutionOnBoardingInfo result = {}", result);
        log.trace("getInstitutionOnBoardingInfo end");
        return result;
    }

}
