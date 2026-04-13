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
import it.pagopa.selfcare.onboarding.connector.exceptions.InvalidRequestException;
import it.pagopa.selfcare.onboarding.connector.model.OnboardingResult;
import it.pagopa.selfcare.onboarding.connector.model.UploadedFile;
import it.pagopa.selfcare.onboarding.core.InstitutionService;
import it.pagopa.selfcare.onboarding.web.model.*;
import it.pagopa.selfcare.onboarding.web.model.mapper.InstitutionResourceMapper;
import it.pagopa.selfcare.onboarding.web.model.mapper.OnboardingResourceMapper;
import it.pagopa.selfcare.onboarding.web.utils.FileValidationUtils;
import it.pagopa.selfcare.onboarding.web.utils.PrincipalUtils;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.io.IOException;
import java.nio.file.Files;
import lombok.extern.slf4j.Slf4j;
import org.jboss.resteasy.reactive.RestForm;
import org.jboss.resteasy.reactive.multipart.FileUpload;
import org.apache.commons.lang3.StringUtils;
import org.owasp.encoder.Encode;

import java.security.Principal;
import java.util.List;

@Slf4j
@ApplicationScoped
@Path("/v2/institutions")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Api(tags = "institutions")
public class InstitutionV2Controller {

    private final InstitutionService institutionService;
    private final OnboardingResourceMapper onboardingResourceMapper;
    private final InstitutionResourceMapper institutionMapper;
    private static final String ONBOARDING_START = "onboarding start";
    private static final String ONBOARDING_END = "onboarding end";

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
                    @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = Problem.class))
            })
    @POST
    @Path("/onboarding")
    @Operation(summary = "${swagger.onboarding.institutions.api.onboarding.subunit}",
            description = "${swagger.onboarding.institutions.api.onboarding.subunit}", operationId = "institutionOnboarding")
    public Response onboarding(@Valid OnboardingProductDto request) {
        log.trace(ONBOARDING_START);
        log.debug("onboarding request = {}", request);
        institutionService.validateOnboardingByProductOrInstitutionTaxCode(request.getTaxCode(), request.getProductId());
        if (Boolean.TRUE.equals(request.getIsAggregator())) {
            institutionService.onboardingPaAggregator(onboardingResourceMapper.toEntity(request));
        } else {
            institutionService.onboardingProductV2(onboardingResourceMapper.toEntity(request));
        }
        log.trace(ONBOARDING_END);
        return Response.status(Response.Status.CREATED).build();
    }

    @ApiResponse(responseCode = "403",
            description = "Forbidden",
            content = {
                    @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = Problem.class))
            })
    @POST
    @Path("/company/onboarding")
    @Operation(summary = "${swagger.onboarding.institutions.api.onboarding.subunit}",
            description = "${swagger.onboarding.institutions.api.onboarding.subunit}", operationId = "institutionOnboardingCompany")
    public Response onboarding(@Valid CompanyOnboardingDto request, @Context Principal principal) {
        log.trace(ONBOARDING_START);
        log.debug("onboarding request = {}", Encode.forJava(request.toString()));
        SelfCareUser selfCareUser = PrincipalUtils.getSelfCareUser(principal);
        institutionService.onboardingCompanyV2(onboardingResourceMapper.toEntity(request), selfCareUser.getFiscalCode());
        log.trace(ONBOARDING_END);
        return Response.status(Response.Status.CREATED).build();
    }

    @ApiResponse(responseCode = "403",
            description = "Forbidden",
            content = {
                    @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = Problem.class))
            })
    @GET
    @Operation(summary = "${swagger.onboarding.institutions.api.onboarding.subunit}",
            description = "${swagger.onboarding.institutions.api.onboarding.subunit}", operationId = "v2GetInstitutionByFilters")
    public List<InstitutionResource> getInstitution(@ApiParam("${swagger.onboarding.institutions.model.productFilter}")
                                                    @QueryParam("productId")
                                                    String productId,
                                                    @ApiParam("${swagger.onboarding.institutions.model.taxCode}")
                                                    @QueryParam("taxCode")
                                                    String taxCode,
                                                    @ApiParam("${swagger.onboarding.institutions.model.origin}")
                                                    @QueryParam("origin")
                                                    String origin,
                                                    @ApiParam("${swagger.onboarding.institutions.model.originId}")
                                                    @QueryParam("originId")
                                                    String originId,
                                                    @ApiParam("${swagger.onboarding.institutions.model.subunitCode}")
                                                    @QueryParam("subunitCode")
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

    @POST
    @Path("/onboarding/aggregation/verification")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Operation(summary = "${swagger.onboarding.institutions.api.onboarding.verifyAggregatesCsv}",
            description = "${swagger.onboarding.institutions.api.onboarding.verifyAggregatesCsv}",  operationId = "verifyAggregatesCsvUsingPOST")
    public VerifyAggregatesResponse verifyAggregatesCsv(@RestForm("aggregates") FileUpload file,
                                                        @RestForm("institutionType") String institutionType,
                                                        @RestForm("productId") String productId){
        log.trace("Verify Aggregates Csv start");
        log.debug("Verify Aggregates Csv start for productId {}", productId);

        UploadedFile uploadedFile = toUploadedFile(file);
        FileValidationUtils.validateAggregatesFile(uploadedFile);
        VerifyAggregatesResponse response = onboardingResourceMapper.toVerifyAggregatesResponse(institutionService.validateAggregatesCsv(uploadedFile, productId));
        log.trace("Verify Aggregates Csv end");
        return response;
    }

    @POST
    @Path("/company/verify-manager")
    @Operation(summary = "${swagger.onboarding.institutions.api.onboarding.verifyManager}",
            description = "${swagger.onboarding.institutions.api.onboarding.verifyManager}", operationId = "verifyManagerUsingPOST")
    public VerifyManagerResponse verifyManager(
            @Valid VerifyManagerRequest request,
            @Context Principal principal
    ) {
        log.trace("verifyManager start");
        SelfCareUser selfCareUser = PrincipalUtils.getSelfCareUser(principal);

        VerifyManagerResponse response = onboardingResourceMapper.toManagerVerification(institutionService.verifyManager(selfCareUser.getFiscalCode(), request.getCompanyTaxCode()));
        log.trace("verifyManager end");
        return response;
    }

    @GET
    @Path("/onboarding/active")
    @Operation(summary = "${swagger.onboarding.institutions.api.onboarding.getActiveOnboarding}",
            description = "${swagger.onboarding.institutions.api.onboarding.getActiveOnboarding}", operationId = "getActiveOnboardingUsingGET")
    public List<InstitutionOnboardingResource> getActiveOnboarding(@QueryParam("taxCode") String taxCode,
                                                                   @QueryParam("productId") String productId,
                                                                   @QueryParam("subunitCode") String subunitCode
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

    @GET
    @Path("/onboarding/recipient-code/verification")
    @Operation(summary = "${swagger.onboarding.institutions.api.onboarding.checkRecipientCode}",
            description = "${swagger.onboarding.institutions.api.onboarding.checkRecipientCode}", operationId = "checkRecipientCodeUsingGET")
    public RecipientCodeStatus checkRecipientCode(@QueryParam("originId") String originId,
                                                  @QueryParam("recipientCode") String recipientCode) {
        log.trace("Check recipientCode start");
        log.debug("Check originId start for institution with originId {} and recipientCode {}", originId, recipientCode);
        RecipientCodeStatus response = onboardingResourceMapper.toRecipientCodeStatus(institutionService.checkRecipientCode(originId, recipientCode));
        log.trace("Check recipientCode end");
        return response;
    }

    @POST
    @Path("/onboarding/users/pg")
    @Operation(summary = "${swagger.onboarding.institutions.api.onboardingUsersPg}",
            description = "${swagger.onboarding.institutions.api.onboardingUsersPg}", operationId = "onboardingUsersPgUsingPOST")
    public void onboardingUsers(@Valid CompanyOnboardingUserDto companyOnboardingUserDto) {
        log.trace("onboardingUsersPgFromIcAndAde start");
        log.debug("onboardingUsersPgFromIcAndAde request = {}", Encode.forJava(companyOnboardingUserDto.toString()));
        institutionService.onboardingUsersPgFromIcAndAde(onboardingResourceMapper.toEntity(companyOnboardingUserDto));
        log.trace("onboardingUsersPgFromIcAndAde end");
    }

    @GET
    @Path("/onboardings")
    @Operation(summary = "${swagger.onboarding.institutions.api.onboardingInfo.summary}",
            description = "${swagger.onboarding.institutions.api.onboardingInfo.description}", operationId = "getOnboardingInfo")
    public List<OnboardingResult> getOnboardingsInfo(@QueryParam("taxCode") String inputTaxCode,
                                                     @QueryParam("status") String inputStatus) {
        log.trace("onboardingInfo start");
        String taxCode = Encode.forJava(inputTaxCode);
        String status = Encode.forJava(inputStatus);
        log.debug("onboardingInfo request = {} - {}", taxCode, status);
        List<OnboardingResult> results = institutionService.getOnboardingWithFilter(taxCode, status);
        log.trace("onboardingInfo end");
        return results;
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
