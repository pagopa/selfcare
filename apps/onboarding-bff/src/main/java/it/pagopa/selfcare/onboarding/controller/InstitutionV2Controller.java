package it.pagopa.selfcare.onboarding.controller;

import io.quarkus.security.Authenticated;
import io.quarkus.security.identity.SecurityIdentity;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import it.pagopa.selfcare.onboarding.util.LogUtils;
import it.pagopa.selfcare.onboarding.exception.InvalidRequestException;
import it.pagopa.selfcare.onboarding.client.model.OnboardingResult;
import it.pagopa.selfcare.onboarding.client.model.UploadedFile;
import it.pagopa.selfcare.onboarding.service.InstitutionService;
import it.pagopa.selfcare.onboarding.controller.request.*;
import it.pagopa.selfcare.onboarding.controller.response.*;
import it.pagopa.selfcare.onboarding.model.error.Problem;
import it.pagopa.selfcare.onboarding.model.RecipientCodeStatus;
import it.pagopa.selfcare.onboarding.mapper.InstitutionMapper;
import it.pagopa.selfcare.onboarding.mapper.OnboardingMapper;
import it.pagopa.selfcare.onboarding.mapper.RegistryProxyMapper;
import it.pagopa.selfcare.onboarding.mapper.UserMapper;
import it.pagopa.selfcare.onboarding.util.FileValidationUtils;
import it.pagopa.selfcare.onboarding.util.RequestParams;
import it.pagopa.selfcare.onboarding.util.SecurityIdentityUtils;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.Objects;
import java.io.IOException;
import java.nio.file.Files;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jboss.resteasy.reactive.RestForm;
import org.jboss.resteasy.reactive.multipart.FileUpload;
import org.apache.commons.lang3.StringUtils;
import org.owasp.encoder.Encode;
import java.util.List;

@Slf4j
@ApplicationScoped
@Authenticated
@Path("/v2/institutions")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "institutions")
@RequiredArgsConstructor
public class InstitutionV2Controller {

    private final InstitutionService institutionService;
    private final OnboardingMapper onboardingMapper;
    private final InstitutionMapper institutionMapper;
    private final RegistryProxyMapper registryProxyMapper;
    private static final String ONBOARDING_START = "onboarding start";
    private static final String ONBOARDING_END = "onboarding end";

    @Inject
    SecurityIdentity securityIdentity;

    @GET
    @Path("/ipa")
    @Operation(summary = "${openapi.onboarding.institutions.api.searchIpaInstitutions.summary}",
            description = "${openapi.onboarding.institutions.api.searchIpaInstitutions.description}",
            operationId = "searchIpaInstitutionsUsingGET")
    public IpaInstitutionsSearchResource searchIpaInstitutions(
            @Parameter(description = "Search text", schema = @Schema(defaultValue = "*"))
            @QueryParam("search") String search,
            @Parameter(description = "${openapi.onboarding.institutions.api.ipaCategory}")
            @QueryParam("category") String category,
            @Parameter(schema = @Schema(type = SchemaType.INTEGER, format = "int32", defaultValue = "0"))
            @QueryParam("page") String page,
            @Parameter(schema = @Schema(type = SchemaType.INTEGER, format = "int32", defaultValue = "50"))
            @QueryParam("pageSize") String pageSize) {
        log.trace("searchIpaInstitutions start");
        String resolvedSearch = search == null || search.isEmpty() ? "*" : search;
        Integer resolvedPage = Objects.requireNonNullElse(RequestParams.optionalInt("page", page), 0);
        Integer resolvedPageSize = Objects.requireNonNullElse(RequestParams.optionalInt("pageSize", pageSize), 50);
        IpaInstitutionsSearchResource resource = registryProxyMapper.toResource(
                institutionService.searchIpaInstitutions(resolvedSearch, category, resolvedPage, resolvedPageSize));
        log.debug("searchIpaInstitutions result count = {}", resource == null ? null : resource.getCount());
        log.trace("searchIpaInstitutions end");
        return resource;
    }

    @GET
    @Path("/ipa/{taxCode}")
    @Operation(summary = "${openapi.onboarding.institutions.api.findIpaInstitutionByTaxCode.summary}",
            description = "${openapi.onboarding.institutions.api.findIpaInstitutionByTaxCode.description}",
            operationId = "findIpaInstitutionByTaxCodeUsingGET")
    public IpaInstitutionResource findIpaInstitutionByTaxCode(
            @PathParam("taxCode") String taxCode,
            @Parameter(description = "${openapi.onboarding.institutions.api.ipaCategory}")
            @QueryParam("category") String category) {
        log.trace("findIpaInstitutionByTaxCode start");
        IpaInstitutionResource resource = registryProxyMapper.toResource(
                institutionService.findIpaInstitutionByTaxCode(taxCode, category));
        log.trace("findIpaInstitutionByTaxCode end");
        return resource;
    }

    @APIResponse(responseCode = "403",
            description = "Forbidden",
            content = {
                    @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = Problem.class))
            })
    @APIResponse(responseCode = "409",
            description = "Conflict",
            content = {
                    @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = Problem.class))
            })
    @POST
    @Path("/onboarding")
    @Operation(summary = "${openapi.onboarding.institutions.api.onboarding.subunit}",
            description = "${openapi.onboarding.institutions.api.onboarding.subunit}", operationId = "institutionOnboarding")
    public Response onboarding(@Valid OnboardingProductDto request) {
        RequestParams.requiredBody(request);
        log.trace(ONBOARDING_START);
        log.debug("onboarding request = {}", LogUtils.sanitize(request));
        institutionService.validateOnboardingByProductOrInstitutionTaxCode(request.getTaxCode(), request.getProductId());
        if (Boolean.TRUE.equals(request.getIsAggregator())) {
            institutionService.onboardingPaAggregator(onboardingMapper.toEntity(request));
        } else {
            institutionService.onboardingProductV2(onboardingMapper.toEntity(request));
        }
        log.trace(ONBOARDING_END);
        return Response.status(Response.Status.CREATED).build();
    }

    @APIResponse(responseCode = "403",
            description = "Forbidden",
            content = {
                    @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = Problem.class))
            })
    @POST
    @Path("/company/onboarding")
    @Operation(summary = "${openapi.onboarding.institutions.api.onboarding.subunit}",
            description = "${openapi.onboarding.institutions.api.onboarding.subunit}", operationId = "institutionOnboardingCompany")
    public Response onboarding(@Valid CompanyOnboardingDto request) {
        RequestParams.requiredBody(request);
        log.trace(ONBOARDING_START);
        log.debug("onboarding request = {}", Encode.forJava(request.toString()));
        String fiscalCode = SecurityIdentityUtils.getFiscalCode(securityIdentity);
        institutionService.onboardingCompanyV2(onboardingMapper.toEntity(request), fiscalCode);
        log.trace(ONBOARDING_END);
        return Response.status(Response.Status.CREATED).build();
    }

    @APIResponse(responseCode = "403",
            description = "Forbidden",
            content = {
                    @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = Problem.class))
            })
    @GET
    @Operation(summary = "${openapi.onboarding.institutions.api.onboarding.subunit}",
            description = "${openapi.onboarding.institutions.api.onboarding.subunit}", operationId = "v2GetInstitutionByFilters")
    public List<InstitutionResource> getInstitution(@Parameter(description = "${openapi.onboarding.institutions.model.productFilter}")
                                                    @QueryParam("productId")
                                                    String productId,
                                                    @Parameter(description = "${openapi.onboarding.institutions.model.taxCode}")
                                                    @QueryParam("taxCode")
                                                    String taxCode,
                                                    @Parameter(description = "${openapi.onboarding.institutions.model.origin}")
                                                    @QueryParam("origin")
                                                    String origin,
                                                    @Parameter(description = "${openapi.onboarding.institutions.model.originId}")
                                                    @QueryParam("originId")
                                                    String originId,
                                                    @Parameter(description = "${openapi.onboarding.institutions.model.subunitCode}")
                                                    @QueryParam("subunitCode")
                                                    String subunitCode) {
        RequestParams.requiredQuery("productId", productId);
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
    @Operation(summary = "${openapi.onboarding.institutions.api.onboarding.verifyAggregatesCsv}",
            description = "${openapi.onboarding.institutions.api.onboarding.verifyAggregatesCsv}",  operationId = "verifyAggregatesCsvUsingPOST")
    public VerifyAggregatesResponse verifyAggregatesCsv(@RestForm("aggregates") FileUpload file,
                                                        @RestForm("institutionType") String institutionType,
                                                        @RestForm("productId") String productId,
                                                        @QueryParam("institutionType") String legacyInstitutionType,
                                                        @QueryParam("productId") String legacyProductId) {
        UploadedFile uploadedFile = toUploadedFile("aggregates", file);
        String resolvedProductId = RequestParams.requiredQuery("productId", productId != null ? productId : legacyProductId);
        log.trace("Verify Aggregates Csv start");
        log.debug("Verify Aggregates Csv start for productId {}", LogUtils.sanitize(resolvedProductId));
        FileValidationUtils.validateAggregatesFile(uploadedFile);
        VerifyAggregatesResponse response = onboardingMapper.toVerifyAggregatesResponse(institutionService.validateAggregatesCsv(uploadedFile, resolvedProductId));
        log.trace("Verify Aggregates Csv end");
        return response;
    }

    @POST
    @Path("/company/verify-manager")
    @Operation(summary = "${openapi.onboarding.institutions.api.onboarding.verifyManager}",
            description = "${openapi.onboarding.institutions.api.onboarding.verifyManager}", operationId = "verifyManagerUsingPOST")
    public VerifyManagerResponse verifyManager(
            @Valid VerifyManagerRequest request
    ) {
        RequestParams.requiredBody(request);
        log.trace("verifyManager start");
        String fiscalCode = SecurityIdentityUtils.getFiscalCode(securityIdentity);
        VerifyManagerResponse response = onboardingMapper.toManagerVerification(institutionService.verifyManager(fiscalCode, request.getCompanyTaxCode()));
        log.trace("verifyManager end");
        return response;
    }

    @GET
    @Path("/onboarding/active")
    @Operation(summary = "${openapi.onboarding.institutions.api.onboarding.getActiveOnboarding}",
            description = "${openapi.onboarding.institutions.api.onboarding.getActiveOnboarding}", operationId = "getActiveOnboardingUsingGET")
    public List<InstitutionOnboardingResource> getActiveOnboarding(@QueryParam("taxCode") String taxCode,
                                                                   @QueryParam("productId") String productId,
                                                                   @QueryParam("subunitCode") String subunitCode
    ) {
        RequestParams.requiredQuery("taxCode", taxCode);
        RequestParams.requiredQuery("productId", productId);
        log.trace("getActiveOnboarding start");
        log.debug("getActiveOnboarding taxCode = {}, productId = {}", Encode.forJava(taxCode), Encode.forJava(productId));
        if ((StringUtils.isBlank(taxCode) || StringUtils.isBlank(productId)))
            throw new InvalidRequestException("taxCode and/or productId must not be blank! ");
        List<InstitutionOnboardingResource> response = institutionService.getActiveOnboarding(taxCode, productId,subunitCode)
                .stream()
                .map(onboardingMapper::toOnboardingResource)
                .toList();
        log.debug("getActiveOnboarding result = {}", response);
        log.trace("getActiveOnboarding end");
        return response;
    }

    @GET
    @Path("/onboarding/recipient-code/verification")
    @Operation(summary = "${openapi.onboarding.institutions.api.onboarding.checkRecipientCode}",
            description = "${openapi.onboarding.institutions.api.onboarding.checkRecipientCode}", operationId = "checkRecipientCodeUsingGET")
    public RecipientCodeStatus checkRecipientCode(@QueryParam("originId") String originId,
                                                  @QueryParam("recipientCode") String recipientCode) {
        RequestParams.requiredQuery("originId", originId);
        RequestParams.requiredQuery("recipientCode", recipientCode);
        log.trace("Check recipientCode start");
        log.debug("Check originId start for institution with originId {} and recipientCode {}",
                LogUtils.sanitize(originId), LogUtils.sanitize(recipientCode));
        RecipientCodeStatus response = onboardingMapper.toRecipientCodeStatus(institutionService.checkRecipientCode(originId, recipientCode));
        log.trace("Check recipientCode end");
        return response;
    }

    @POST
    @Path("/onboarding/users/pg")
    @Operation(summary = "${openapi.onboarding.institutions.api.onboardingUsersPg}",
            description = "${openapi.onboarding.institutions.api.onboardingUsersPg}", operationId = "onboardingUsersPgUsingPOST")
    public Response onboardingUsers(@Valid CompanyOnboardingUserDto companyOnboardingUserDto) {
        log.trace("onboardingUsersPgFromIcAndAde start");
        log.debug("onboardingUsersPgFromIcAndAde request = {}", Encode.forJava(companyOnboardingUserDto.toString()));
        institutionService.onboardingUsersPgFromIcAndAde(onboardingMapper.toEntity(companyOnboardingUserDto));
        log.trace("onboardingUsersPgFromIcAndAde end");
        return Response.ok().build();
    }

    @GET
    @Path("/onboardings")
    @Operation(summary = "${openapi.onboarding.institutions.api.onboardingInfo.summary}",
            description = "${openapi.onboarding.institutions.api.onboardingInfo.description}", operationId = "getOnboardingInfo")
    public List<OnboardingResult> getOnboardingsInfo(@QueryParam("taxCode") String inputTaxCode,
                                                     @QueryParam("status") String inputStatus) {
        RequestParams.requiredQuery("taxCode", inputTaxCode);
        RequestParams.requiredQuery("status", inputStatus);
        log.trace("onboardingInfo start");
        String taxCode = Encode.forJava(inputTaxCode);
        String status = Encode.forJava(inputStatus);
        log.debug("onboardingInfo request = {} - {}", taxCode, status);
        List<OnboardingResult> results = institutionService.getOnboardingWithFilter(taxCode, status);
        log.trace("onboardingInfo end");
        return results;
    }

    @PUT
    @Path("/{onboardingId}")
    @Operation(summary = "Trigger onboarding request",
            description = "Idempotent trigger invoked after each document upload. If all mandatory documents are present, triggers orchestration to advance the onboarding from REQUEST to PENDING.",
            operationId = "triggerOnboardingRequest")
    public Response triggerOnboardingRequest(@PathParam("onboardingId") String onboardingId) {
        log.trace("triggerOnboardingRequest start");
        log.debug("triggerOnboardingRequest onboardingId = {}", Encode.forJava(onboardingId));
        institutionService.triggerOnboardingRequest(onboardingId);
        log.trace("triggerOnboardingRequest end");
        return Response.noContent().build();
    }

    private static UploadedFile toUploadedFile(String partName, FileUpload fileUpload) {
        RequestParams.requiredPart(partName, fileUpload);
        try {
            return new UploadedFile(fileUpload.fileName(), fileUpload.contentType(), Files.readAllBytes(fileUpload.uploadedFile()));
        } catch (IOException e) {
            throw new IllegalStateException("Cannot read uploaded file", e);
        }
    }

}
