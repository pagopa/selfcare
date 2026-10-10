package it.pagopa.selfcare.onboarding.controller;

import io.quarkus.security.Authenticated;
import io.quarkus.security.identity.SecurityIdentity;
import io.smallrye.mutiny.Uni;
import io.smallrye.common.annotation.Blocking;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.enums.ParameterIn;
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import it.pagopa.selfcare.onboarding.util.LogUtils;
import it.pagopa.selfcare.onboarding.exception.InvalidRequestException;
import it.pagopa.selfcare.onboarding.client.model.OnboardingResult;
import it.pagopa.selfcare.onboarding.client.model.UploadedFile;
import it.pagopa.selfcare.onboarding.service.InstitutionService;
import it.pagopa.selfcare.onboarding.model.dto.request.*;
import it.pagopa.selfcare.onboarding.model.dto.response.*;
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
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.UriInfo;
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

    @Context
    UriInfo uriInfo;

    @GET
    @Path("/ipa")
    @Operation(summary = "${openapi.onboarding.institutions.api.searchIpaInstitutions.summary}",
            description = "${openapi.onboarding.institutions.api.searchIpaInstitutions.description}",
            operationId = "searchIpaInstitutionsUsingGET")
    public Uni<IpaInstitutionsSearchResource> searchIpaInstitutions(
            @Parameter(schema = @Schema(type = SchemaType.STRING, defaultValue = "*"))
            @QueryParam("search") String search,
            @Parameter(description = "${openapi.onboarding.institutions.api.ipaCategory}")
            @QueryParam("category") String category,
            @Parameter(schema = @Schema(type = SchemaType.INTEGER, format = "int32", defaultValue = "0"))
            @QueryParam("page") String page,
            @Parameter(schema = @Schema(type = SchemaType.INTEGER, format = "int32", defaultValue = "50"))
            @QueryParam("pageSize") String pageSize) {
        search = RequestParams.stringQuery(uriInfo, "search", search);
        category = RequestParams.stringQuery(uriInfo, "category", category);
        log.trace("searchIpaInstitutions start");
        String resolvedSearch = search == null || search.isEmpty() ? "*" : search;
        Integer resolvedPage = Objects.requireNonNullElse(RequestParams.optionalInt("page", page), 0);
        Integer resolvedPageSize = Objects.requireNonNullElse(RequestParams.optionalInt("pageSize", pageSize), 50);
        return institutionService.searchIpaInstitutions(resolvedSearch, category, resolvedPage, resolvedPageSize)
                .map(registryProxyMapper::toResource)
                .invoke(resource -> {
                    log.debug("searchIpaInstitutions result count = {}", resource == null ? null : resource.getCount());
                    log.trace("searchIpaInstitutions end");
                });
    }

    @GET
    @Path("/ipa/{taxCode}")
    @Operation(summary = "${openapi.onboarding.institutions.api.findIpaInstitutionByTaxCode.summary}",
            description = "${openapi.onboarding.institutions.api.findIpaInstitutionByTaxCode.description}",
            operationId = "findIpaInstitutionByTaxCodeUsingGET")
    public Uni<IpaInstitutionResource> findIpaInstitutionByTaxCode(
            @PathParam("taxCode") String taxCode,
            @Parameter(description = "${openapi.onboarding.institutions.api.ipaCategory}")
            @QueryParam("category") String category) {
        category = RequestParams.stringQuery(uriInfo, "category", category);
        log.trace("findIpaInstitutionByTaxCode start");
        return institutionService.findIpaInstitutionByTaxCode(taxCode, category)
                .map(registryProxyMapper::toResource)
                .invoke(resource -> log.trace("findIpaInstitutionByTaxCode end"));
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
    @APIResponse(responseCode = "201", description = "Created")
    @POST
    @Path("/onboarding")
    @Operation(summary = "${openapi.onboarding.institutions.api.onboarding.subunit}",
            description = "${openapi.onboarding.institutions.api.onboarding.subunit}", operationId = "institutionOnboarding")
    public Uni<Response> onboarding(@Valid OnboardingProductDto request) {
        RequestParams.requiredBody(request);
        log.trace(ONBOARDING_START);
        log.debug("onboarding request = {}", LogUtils.sanitize(request));
        return institutionService.validateOnboardingByProductOrInstitutionTaxCode(request.getTaxCode(), request.getProductId())
                .chain(() -> Boolean.TRUE.equals(request.getIsAggregator())
                        ? institutionService.onboardingPaAggregator(onboardingMapper.toEntity(request))
                        : institutionService.onboardingProductV2(onboardingMapper.toEntity(request)))
                .invoke(() -> log.trace(ONBOARDING_END))
                .replaceWith(() -> Response.status(Response.Status.CREATED).build());
    }

    @APIResponse(responseCode = "403",
            description = "Forbidden",
            content = {
                    @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = Problem.class))
            })
    @APIResponse(responseCode = "201", description = "Created")
    @POST
    @Path("/company/onboarding")
    @Operation(summary = "${openapi.onboarding.institutions.api.onboarding.subunit}",
            description = "${openapi.onboarding.institutions.api.onboarding.subunit}", operationId = "institutionOnboardingCompany")
    public Uni<Response> onboarding(@Valid CompanyOnboardingDto request) {
        RequestParams.requiredBody(request);
        log.trace(ONBOARDING_START);
        log.debug("onboarding request = {}", Encode.forJava(request.toString()));
        String fiscalCode = SecurityIdentityUtils.getFiscalCode(securityIdentity);
        return institutionService.onboardingCompanyV2(onboardingMapper.toEntity(request), fiscalCode)
                .invoke(() -> log.trace(ONBOARDING_END))
                .replaceWith(() -> Response.status(Response.Status.CREATED).build());
    }

    @APIResponse(responseCode = "403",
            description = "Forbidden",
            content = {
                    @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = Problem.class))
            })
    @APIResponse(responseCode = "200", description = "OK")
    @GET
    @Operation(summary = "${openapi.onboarding.institutions.api.onboarding.subunit}",
            description = "${openapi.onboarding.institutions.api.onboarding.subunit}", operationId = "v2GetInstitutionByFilters")
    public Uni<List<InstitutionResource>> getInstitution(@Parameter(description = "${openapi.onboarding.institutions.model.productFilter}", required = true)
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
        productId = RequestParams.stringQuery(uriInfo, "productId", productId);
        taxCode = RequestParams.stringQuery(uriInfo, "taxCode", taxCode);
        origin = RequestParams.stringQuery(uriInfo, "origin", origin);
        originId = RequestParams.stringQuery(uriInfo, "originId", originId);
        subunitCode = RequestParams.stringQuery(uriInfo, "subunitCode", subunitCode);
        RequestParams.requiredQuery("productId", productId);
        log.trace("getInstitution start");
        return institutionService.getByFilters(productId, taxCode, origin, originId, subunitCode)
                .map(institutions -> institutions.stream().map(institutionMapper::toResource).toList())
                .invoke(result -> {
                    log.debug(LogUtils.CONFIDENTIAL_MARKER, "getInstitution result = {}", result);
                    log.trace("getInstitution end");
                });
    }

    @POST
    @Path("/onboarding/aggregation/verification")
    @Blocking
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @RequestBody(required = false, content = @Content(mediaType = MediaType.MULTIPART_FORM_DATA,
            schema = @Schema(requiredProperties = "aggregates")))
    @Parameter(name = "institutionType", in = ParameterIn.QUERY, schema = @Schema(type = SchemaType.STRING))
    @Parameter(name = "productId", in = ParameterIn.QUERY, required = true, schema = @Schema(type = SchemaType.STRING))
    @Operation(summary = "${openapi.onboarding.institutions.api.onboarding.verifyAggregatesCsv}",
            description = "${openapi.onboarding.institutions.api.onboarding.verifyAggregatesCsv}",  operationId = "verifyAggregatesCsvUsingPOST")
    public Uni<VerifyAggregatesResponse> verifyAggregatesCsv(@RestForm("aggregates") FileUpload file,
                                                        @Schema(hidden = true) @RestForm("institutionType") String institutionType,
                                                        @Schema(hidden = true) @RestForm("productId") String productId,
                                                        @QueryParam("institutionType") String legacyInstitutionType,
                                                        @QueryParam("productId") String legacyProductId) {
        legacyInstitutionType = RequestParams.stringQuery(uriInfo, "institutionType", legacyInstitutionType);
        legacyProductId = RequestParams.stringQuery(uriInfo, "productId", legacyProductId);
        UploadedFile uploadedFile = toUploadedFile("aggregates", file);
        String resolvedProductId = RequestParams.requiredQuery("productId", productId != null ? productId : legacyProductId);
        log.trace("Verify Aggregates Csv start");
        log.debug("Verify Aggregates Csv start for productId {}", LogUtils.sanitize(resolvedProductId));
        FileValidationUtils.validateAggregatesFile(uploadedFile);
        return institutionService.validateAggregatesCsv(uploadedFile, resolvedProductId)
                .map(onboardingMapper::toVerifyAggregatesResponse)
                .invoke(response -> log.trace("Verify Aggregates Csv end"));
    }

    @POST
    @Path("/company/verify-manager")
    @Operation(summary = "${openapi.onboarding.institutions.api.onboarding.verifyManager}",
            description = "${openapi.onboarding.institutions.api.onboarding.verifyManager}", operationId = "verifyManagerUsingPOST")
    public Uni<VerifyManagerResponse> verifyManager(
            @Valid VerifyManagerRequest request
    ) {
        RequestParams.requiredBody(request);
        log.trace("verifyManager start");
        String fiscalCode = SecurityIdentityUtils.getFiscalCode(securityIdentity);
        return institutionService.verifyManager(fiscalCode, request.getCompanyTaxCode())
                .map(onboardingMapper::toManagerVerification)
                .invoke(response -> log.trace("verifyManager end"));
    }

    @GET
    @Path("/onboarding/active")
    @Operation(summary = "${openapi.onboarding.institutions.api.onboarding.getActiveOnboarding}",
            description = "${openapi.onboarding.institutions.api.onboarding.getActiveOnboarding}", operationId = "getActiveOnboardingUsingGET")
    public Uni<List<InstitutionOnboardingResource>> getActiveOnboarding(@Parameter(required = true) @QueryParam("taxCode") String taxCode,
                                                                   @Parameter(required = true) @QueryParam("productId") String productId,
                                                                   @QueryParam("subunitCode") String subunitCode
    ) {
        taxCode = RequestParams.stringQuery(uriInfo, "taxCode", taxCode);
        productId = RequestParams.stringQuery(uriInfo, "productId", productId);
        subunitCode = RequestParams.stringQuery(uriInfo, "subunitCode", subunitCode);
        RequestParams.requiredQuery("taxCode", taxCode);
        RequestParams.requiredQuery("productId", productId);
        log.trace("getActiveOnboarding start");
        log.debug("getActiveOnboarding taxCode = {}, productId = {}", Encode.forJava(taxCode), Encode.forJava(productId));
        if ((StringUtils.isBlank(taxCode) || StringUtils.isBlank(productId)))
            throw new InvalidRequestException("taxCode and/or productId must not be blank! ");
        return institutionService.getActiveOnboarding(taxCode, productId, subunitCode)
                .map(institutions -> institutions.stream().map(onboardingMapper::toOnboardingResource).toList())
                .invoke(response -> {
                    log.debug("getActiveOnboarding result = {}", response);
                    log.trace("getActiveOnboarding end");
                });
    }

    @GET
    @Path("/onboarding/recipient-code/verification")
    @Operation(summary = "${openapi.onboarding.institutions.api.onboarding.checkRecipientCode}",
            description = "${openapi.onboarding.institutions.api.onboarding.checkRecipientCode}", operationId = "checkRecipientCodeUsingGET")
    public Uni<RecipientCodeStatus> checkRecipientCode(@Parameter(required = true) @QueryParam("originId") String originId,
                                                  @Parameter(required = true) @QueryParam("recipientCode") String recipientCode) {
        originId = RequestParams.stringQuery(uriInfo, "originId", originId);
        recipientCode = RequestParams.stringQuery(uriInfo, "recipientCode", recipientCode);
        RequestParams.requiredQuery("originId", originId);
        RequestParams.requiredQuery("recipientCode", recipientCode);
        log.trace("Check recipientCode start");
        log.debug("Check originId start for institution with originId {} and recipientCode {}",
                LogUtils.sanitize(originId), LogUtils.sanitize(recipientCode));
        return institutionService.checkRecipientCode(originId, recipientCode)
                .map(onboardingMapper::toRecipientCodeStatus)
                .invoke(response -> log.trace("Check recipientCode end"));
    }

    @POST
    @Path("/onboarding/users/pg")
    @APIResponse(responseCode = "200", description = "OK",
            content = @Content(schema = @Schema(implementation = Void.class)))
    @Operation(summary = "${openapi.onboarding.institutions.api.onboardingUsersPg}",
            description = "${openapi.onboarding.institutions.api.onboardingUsersPg}", operationId = "onboardingUsersPgUsingPOST")
    public Uni<Response> onboardingUsers(@RequestBody(required = false) @Valid CompanyOnboardingUserDto companyOnboardingUserDto) {
        log.trace("onboardingUsersPgFromIcAndAde start");
        log.debug("onboardingUsersPgFromIcAndAde request = {}", Encode.forJava(companyOnboardingUserDto.toString()));
        return institutionService.onboardingUsersPgFromIcAndAde(onboardingMapper.toEntity(companyOnboardingUserDto))
                .invoke(() -> log.trace("onboardingUsersPgFromIcAndAde end"))
                .replaceWith(() -> Response.ok().build());
    }

    @GET
    @Path("/onboardings")
    @Operation(summary = "${openapi.onboarding.institutions.api.onboardingInfo.summary}",
            description = "${openapi.onboarding.institutions.api.onboardingInfo.description}", operationId = "getOnboardingInfo")
    public Uni<List<OnboardingResult>> getOnboardingsInfo(@Parameter(required = true) @QueryParam("taxCode") String inputTaxCode,
                                                     @Parameter(required = true) @QueryParam("status") String inputStatus) {
        inputTaxCode = RequestParams.stringQuery(uriInfo, "taxCode", inputTaxCode);
        inputStatus = RequestParams.stringQuery(uriInfo, "status", inputStatus);
        RequestParams.requiredQuery("taxCode", inputTaxCode);
        RequestParams.requiredQuery("status", inputStatus);
        log.trace("onboardingInfo start");
        String taxCode = Encode.forJava(inputTaxCode);
        String status = Encode.forJava(inputStatus);
        log.debug("onboardingInfo request = {} - {}", taxCode, status);
        return institutionService.getOnboardingWithFilter(taxCode, status)
                .invoke(results -> log.trace("onboardingInfo end"));
    }

    @APIResponse(responseCode = "204", description = "No Content")
    @PUT
    @Path("/{onboardingId}")
    @Operation(summary = "Trigger onboarding request",
            description = "Idempotent trigger invoked after each document upload. If all mandatory documents are present, triggers orchestration to advance the onboarding from REQUEST to PENDING.",
            operationId = "triggerOnboardingRequest")
    public Uni<Response> triggerOnboardingRequest(@Parameter(description = "The onboarding id")
                                             @PathParam("onboardingId") String onboardingId) {
        log.trace("triggerOnboardingRequest start");
        log.debug("triggerOnboardingRequest onboardingId = {}", Encode.forJava(onboardingId));
        return institutionService.triggerOnboardingRequest(onboardingId)
                .invoke(() -> log.trace("triggerOnboardingRequest end"))
                .replaceWith(() -> Response.noContent().build());
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
