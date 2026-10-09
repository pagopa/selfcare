package it.pagopa.selfcare.onboarding.controller;

import static it.pagopa.selfcare.onboarding.common.ProductId.PROD_FD;
import static it.pagopa.selfcare.onboarding.common.ProductId.PROD_FD_GARANTITO;

import io.quarkus.security.Authenticated;
import io.quarkus.security.identity.SecurityIdentity;
import io.smallrye.mutiny.Uni;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import it.pagopa.selfcare.onboarding.util.LogUtils;
import it.pagopa.selfcare.onboarding.util.RequestParams;
import it.pagopa.selfcare.onboarding.exception.InvalidRequestException;
import it.pagopa.selfcare.onboarding.client.model.InstitutionLegalAddressData;
import it.pagopa.selfcare.onboarding.client.model.InstitutionOnboardingData;
import it.pagopa.selfcare.onboarding.client.model.MatchInfoResult;
import it.pagopa.selfcare.onboarding.model.VerifyType;
import it.pagopa.selfcare.onboarding.service.InstitutionService;
import it.pagopa.selfcare.onboarding.model.dto.request.*;
import it.pagopa.selfcare.onboarding.model.dto.response.*;
import it.pagopa.selfcare.onboarding.model.error.Problem;
import it.pagopa.selfcare.onboarding.mapper.*;
import it.pagopa.selfcare.onboarding.util.SecurityIdentityUtils;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.core.Response;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.owasp.encoder.Encode;

@Slf4j
@ApplicationScoped
@Authenticated
@Path("/v1/institutions")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "institutions")
@RequiredArgsConstructor
public class InstitutionController {

    @Context
    UriInfo uriInfo;

    private final InstitutionService institutionService;
    private final OnboardingMapper onboardingMapper;
    private final InstitutionMapper institutionMapper;
    private final UserMapper userMapper;
    private static final String ONBOARDING_START = "onboarding start";
    private static final String ONBOARDING_END = "onboarding end";

    @Inject
    SecurityIdentity securityIdentity;

    @APIResponse(responseCode = "403",
            description = "Forbidden",
            content = {
                    @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = Problem.class))
            })
    @APIResponse(responseCode = "201", description = "Created")
    @POST
    @Path("/onboarding")
    @Operation(summary = "${openapi.onboarding.institutions.api.onboarding.subunit}",
            description = "${openapi.onboarding.institutions.api.onboarding.subunit}", operationId = "onboardingUsingPOST")
    public Uni<Response> onboarding(@Valid OnboardingProductDto request) {
        RequestParams.requiredBody(request);
        log.trace(ONBOARDING_START);
        log.debug("onboarding request = {}", LogUtils.sanitize(request));
        return institutionService.onboardingProduct(onboardingMapper.toEntity(request))
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
            description = "${openapi.onboarding.institutions.api.onboarding.subunit}", operationId = "onboardingCompanyUsingPOST")
    public Uni<Response> onboarding(@Valid CompanyOnboardingDto request) {
        RequestParams.requiredBody(request);
        log.trace(ONBOARDING_START);
        log.debug("onboarding request = {}", LogUtils.sanitize(request));
        return institutionService.onboardingProduct(onboardingMapper.toEntity(request))
                .invoke(() -> log.trace(ONBOARDING_END))
                .replaceWith(() -> Response.status(Response.Status.CREATED).build());
    }

    @GET
    @Path("/onboarding")
    @Operation(summary = "${openapi.onboarding.institutions.api.getInstitutionOnboardingInfo}",
            description = "${openapi.onboarding.institutions.api.getInstitutionOnboardingInfo}", operationId = "getInstitutionOnboardingInfoUsingGET")
    public InstitutionOnboardingInfoResource getInstitutionOnboardingInfoById(@Parameter(description = "${openapi.onboarding.institutions.model.id}", required = true)
                                                                          @QueryParam("institutionId")
                                                                          String institutionId,
                                                                          @Parameter(description = "${openapi.onboarding.product.model.id}", required = true)
                                                                          @QueryParam("productId")
                                                                          String productId) {
        institutionId = RequestParams.stringQuery(uriInfo, "institutionId", institutionId);
        productId = RequestParams.stringQuery(uriInfo, "productId", productId);
        RequestParams.requiredQuery("institutionId", institutionId);
        RequestParams.requiredQuery("productId", productId);
        log.trace("getInstitutionOnboardingInfoById start");
        log.debug("getInstitutionOnboardingInfoById institutionId = {}, productId = {}", Encode.forJava(institutionId), Encode.forJava(productId));
        InstitutionOnboardingData institutionOnboardingData = institutionService.getInstitutionOnboardingDataById(institutionId, productId);
        InstitutionOnboardingInfoResource result = institutionMapper.toResource(institutionOnboardingData);
        log.debug("getInstitutionOnboardingInfoById result = {}", result);
        log.trace("getInstitutionOnboardingInfoById end");
        return result;
    }

    @GET
    @Path("/{externalInstitutionId}/geographic-taxonomy")
    @Operation(summary = "${openapi.onboarding.institutions.api.getInstitutionGeographicTaxonomy}",
            description = "${openapi.onboarding.institutions.api.getInstitutionGeographicTaxonomy}", operationId = "getInstitutionGeographicTaxonomyUsingGET")
    public List<GeographicTaxonomyResource> getInstitutionGeographicTaxonomy(@Parameter(description = "${openapi.onboarding.institutions.model.externalId}")
                                                                             @PathParam("externalInstitutionId")
                                                                             String externalInstitutionId) {
        log.trace("getInstitutionGeographicTaxonomy start");
        log.debug("getInstitutionGeographicTaxonomy institutionId = {}", LogUtils.sanitize(externalInstitutionId));
        List<GeographicTaxonomyResource> geographicTaxonomies = institutionService.getGeographicTaxonomyList(externalInstitutionId)
                .stream()
                .map(institutionMapper::toResource)
                .toList();
        log.debug("getInstitutionGeographicTaxonomy result = {}", geographicTaxonomies);
        log.trace("getInstitutionGeographicTaxonomy end");
        return geographicTaxonomies;
    }

    @GET
    @Path("/geographic-taxonomies")
    @Operation(summary = "${openapi.onboarding.institutions.api.getInstitutionGeographicTaxonomy}",
            description = "${openapi.onboarding.institutions.api.getInstitutionGeographicTaxonomy}", operationId = "getGeographicTaxonomiesByTaxCodeAndSubunitCodeUsingGET")
    public List<GeographicTaxonomyResource> getGeographicTaxonomiesByTaxCodeAndSubunitCode(@Parameter(description = "${openapi.onboarding.institutions.model.taxCode}", required = true)
                                                                                           @QueryParam("taxCode")
                                                                                           String taxCode,
                                                                                           @Parameter(description = "${openapi.onboarding.institutions.model.subunitCode}")
                                                                                           @QueryParam("subunitCode")
                                                                                           String subunitCode) {
        taxCode = RequestParams.stringQuery(uriInfo, "taxCode", taxCode);
        subunitCode = RequestParams.stringQuery(uriInfo, "subunitCode", subunitCode);
        RequestParams.requiredQuery("taxCode", taxCode);
        log.trace("getGeographicTaxonomiesByTaxCodeAndSubunitCode start");
        log.debug("getGeographicTaxonomiesByTaxCodeAndSubunitCode taxCode = {}, subunitCode = {}",
                LogUtils.sanitize(taxCode), LogUtils.sanitize(subunitCode));
        if (StringUtils.isBlank(taxCode) || (Objects.nonNull(subunitCode) && StringUtils.isBlank(subunitCode)))
            throw new InvalidRequestException("taxCode and/or subunitCode must not be blank! ");

        List<GeographicTaxonomyResource> geographicTaxonomies = institutionService.getGeographicTaxonomyList(taxCode, subunitCode)
                .stream()
                .map(institutionMapper::toResource)
                .toList();
        log.debug("getGeographicTaxonomiesByTaxCodeAndSubunitCode result = {}", geographicTaxonomies);
        log.trace("getGeographicTaxonomiesByTaxCodeAndSubunitCode end");
        return geographicTaxonomies;
    }

    @GET
    @Path("")
    @Operation(summary = "${openapi.onboarding.institutions.api.getInstitutions}",
            description = "${openapi.onboarding.institutions.api.getInstitutions}", operationId = "getInstitutionsUsingGET")
    public Uni<List<InstitutionResource>> getInstitutions(@Parameter(description = "${openapi.onboarding.institutions.model.productFilter}")
                                                     @QueryParam("productId")
                                                     String productId) {
        productId = RequestParams.stringQuery(uriInfo, "productId", productId);
        log.trace("getInstitutions start");
        String uid = SecurityIdentityUtils.getUid(securityIdentity);

        return institutionService.getInstitutions(productId, uid)
                .map(institutions -> institutions.stream().map(institutionMapper::toResource).toList())
                .invoke(institutions -> {
                    log.debug("getInstitutions result = {}", institutions);
                    log.trace("getInstitutions end");
                });
    }


    @APIResponse(responseCode = "403",
            description = "Forbidden",
            content = {
                    @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = Problem.class))
            })
    @APIResponse(responseCode = "204", description = "No Content")
    @HEAD
    @Path("/{externalInstitutionId}/products/{productId}")
    @Operation(summary = "${openapi.onboarding.institutions.api.verifyOnboarding}",
            description = "${openapi.onboarding.institutions.api.verifyOnboarding}", operationId = "verifyOnboardingProductUsingHEAD")
    public Uni<Void> verifyOnboarding(@Parameter(description = "${openapi.onboarding.institutions.model.externalId}")
                                 @PathParam("externalInstitutionId")
                                 String externalInstitutionId,
                                 @Parameter(description = "${openapi.onboarding.product.model.id}")
                                 @PathParam("productId")
                                 String productId) {
        log.trace("verifyOnboarding start");
        log.debug("verifyOnboarding externalInstitutionId = {}, productId = {}",
                LogUtils.sanitize(externalInstitutionId), LogUtils.sanitize(productId));
        return institutionService.verifyOnboarding(externalInstitutionId, productId)
                .invoke(() -> log.trace("verifyOnboarding end"));
    }

    @APIResponse(responseCode = "403",
            description = "Forbidden",
            content = {
                    @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = Problem.class))
            })
    @APIResponse(responseCode = "204", description = "No Content")
    @HEAD
    @Path("/onboarding")
    @Operation(summary = "${openapi.onboarding.institutions.api.verifyOnboarding}",
            description = "${openapi.onboarding.institutions.api.verifyOnboarding}", operationId = "verifyOnboardingUsingHEAD")
    public Uni<Void> verifyOnboarding(@Parameter(description = "${openapi.onboarding.institutions.model.taxCode}")
                                     @QueryParam("taxCode")
                                     String taxCode,
                                 @Parameter(description = "${openapi.onboarding.institutions.model.subunitCode}")
                                     @QueryParam("subunitCode")
                                     String subunitCode,
                                 @Parameter(description = "${openapi.onboarding.product.model.id}", required = true)
                                     @QueryParam("productId")
                                     String productId,
                                 @Parameter(description = "${openapi.onboarding.institutions.model.origin}")
                                     @QueryParam("origin")
                                     String origin,
                                 @Parameter(description = "${openapi.onboarding.institutions.model.originId}")
                                     @QueryParam("originId")
                                     String originId,
                                 @Parameter(description = "${openapi.onboarding.institutions.model.vatNumber}")
                                     @QueryParam("vatNumber")
                                     Optional<String> vatNumber,
                                 @Parameter(description = "${openapi.onboarding.institutions.model.institutionType}")
                                     @QueryParam("institutionType")
                                     String institutionType,
                                 @Parameter(description = "${openapi.onboarding.institutions.model.verifyType}",
                                         schema = @Schema(implementation = VerifyType.class))
                                     @QueryParam("verifyType") String verifyType) {
        productId = RequestParams.stringQuery(uriInfo, "productId", productId);
        taxCode = RequestParams.stringQuery(uriInfo, "taxCode", taxCode);
        subunitCode = RequestParams.stringQuery(uriInfo, "subunitCode", subunitCode);
        origin = RequestParams.stringQuery(uriInfo, "origin", origin);
        originId = RequestParams.stringQuery(uriInfo, "originId", originId);
        institutionType = RequestParams.stringQuery(uriInfo, "institutionType", institutionType);
        vatNumber = Optional.ofNullable(RequestParams.stringQuery(uriInfo, "vatNumber", vatNumber.orElse(null)));
        RequestParams.requiredQuery("productId", productId);
        VerifyType type = RequestParams.optionalEnum("verifyType", verifyType, VerifyType.class);
        log.trace("verifyOnboarding start");
        Uni<Void> verification;
        if (VerifyType.EXTERNAL.equals(type) && vatNumber.isPresent() && (PROD_FD.getValue().equals(productId) || PROD_FD_GARANTITO.getValue().equals(productId))) {
            verification = institutionService.checkOrganization(productId, taxCode, vatNumber.get());
        } else {
            verification = institutionService.verifyOnboarding(productId, taxCode, origin, originId, subunitCode, institutionType);
        }
        return verification.invoke(() -> log.trace("verifyOnboarding end"));
    }

    @APIResponse(responseCode = "403",
            description = "Forbidden",
            content = {
                    @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = Problem.class))
            })
    @APIResponse(responseCode = "200", description = "OK",
            content = @Content(schema = @Schema(implementation = Void.class)))
    @GET
    @Path("/onboarding/verify")
    @Operation(summary = "${openapi.onboarding.institutions.api.verifyOnboarding}",
            description = "${openapi.onboarding.institutions.api.verifyOnboarding}", operationId = "verifyOnboardingUsingGET")
    public Uni<Response> verifyOnboardingGet(@Parameter(description = "${openapi.onboarding.institutions.model.taxCode}")
                                        @QueryParam("taxCode")
                                        String taxCode,
                                        @Parameter(description = "${openapi.onboarding.institutions.model.subunitCode}")
                                        @QueryParam("subunitCode")
                                        String subunitCode,
                                        @Parameter(description = "${openapi.onboarding.product.model.id}", required = true)
                                        @QueryParam("productId")
                                        String productId,
                                        @Parameter(description = "${openapi.onboarding.institutions.model.origin}")
                                        @QueryParam("origin")
                                        String origin,
                                        @Parameter(description = "${openapi.onboarding.institutions.model.originId}")
                                        @QueryParam("originId")
                                        String originId,
                                        @Parameter(description = "${openapi.onboarding.institutions.model.vatNumber}")
                                        @QueryParam("vatNumber")
                                        Optional<String> vatNumber,
                                        @Parameter(description = "${openapi.onboarding.institutions.model.institutionType}")
                                        @QueryParam("institutionType")
                                        String institutionType,
                                        @Parameter(description = "${openapi.onboarding.institutions.model.verifyType}",
                                                schema = @Schema(implementation = VerifyType.class))
                                        @QueryParam("verifyType") String verifyType) {
        return verifyOnboarding(taxCode, subunitCode, productId, origin, originId, vatNumber, institutionType, verifyType)
                .replaceWith(() -> Response.ok().build());
    }

    @GET
    @Path("/from-infocamere/")
    @Operation(summary = "${openapi.onboarding.institutions.api.getInstitutionsByUser}",
            description = "${openapi.onboarding.institutions.api.getInstitutionsByUser}", operationId = "getInstitutionsFromInfocamereUsingGET")
    public Uni<InstitutionResourceIC> getInstitutionsFromInfocamere() {
        log.trace("getInstitutionsFromInfocamere start");
        String fiscalCode = SecurityIdentityUtils.getFiscalCode(securityIdentity);
        return institutionService.getInstitutionsByUser(fiscalCode)
                .map(institutionMapper::toResource)
                .invoke(resource -> {
                    log.debug(LogUtils.CONFIDENTIAL_MARKER, "getInstitutionsFromInfocamere result = {}", resource);
                    log.trace("getInstitutionsFromInfocamere end");
                });
    }

    @POST
    @Path("/verification/match")
    @Operation(summary = "${openapi.onboarding.institutions.api.matchInstitutionAndUser}",
            description = "${openapi.onboarding.institutions.api.matchInstitutionAndUser}", operationId = "postVerificationMatchUsingPOST")
    public MatchInfoResultResource postVerificationMatch(
                                                         @Valid
                                                         VerificationMatchRequest verificationMatchRequest) {
        RequestParams.requiredBody(verificationMatchRequest);
        log.trace("matchInstitutionAndUser start");
        log.debug(LogUtils.CONFIDENTIAL_MARKER, "matchInstitutionAndUser userDto = {}", LogUtils.sanitize(verificationMatchRequest));
        MatchInfoResult matchInfoResult = institutionService.matchInstitutionAndUser(verificationMatchRequest.getTaxCode(),
                userMapper.toUser(verificationMatchRequest.getUserDto()));
        MatchInfoResultResource result = institutionMapper.toResource(matchInfoResult);
        log.debug("matchInstitutionAndUser result = {}", result);
        log.trace("matchInstitutionAndUser end");
        return result;
    }

    @POST
    @Path("/verification/legal-address")
    @Operation(summary = "${openapi.onboarding.institutions.api.getInstitutionLegalAddress}",
            description = "${openapi.onboarding.institutions.api.getInstitutionLegalAddress}", operationId = "postVerificationLegalAddressUsingPOST")
    public InstitutionLegalAddressResource postVerificationLegalAddress(@Valid VerificationLegalAddressRequest verificationLegalAddressRequest) {
        RequestParams.requiredBody(verificationLegalAddressRequest);
        log.trace("getInstitutionLegalAddress start");
        log.debug(LogUtils.CONFIDENTIAL_MARKER, "getInstitutionLegalAddress institutionId = {}",
                LogUtils.sanitize(verificationLegalAddressRequest.getTaxCode()));
        InstitutionLegalAddressData institutionLegalAddressData = institutionService.getInstitutionLegalAddress(verificationLegalAddressRequest.getTaxCode());
        InstitutionLegalAddressResource result = onboardingMapper.toResource(institutionLegalAddressData);
        log.debug("getInstitutionLegalAddress result = {}", result);
        log.trace("getInstitutionLegalAddress end");
        return result;
    }

    /**
     * @param externalInstitutionId
     * @deprecated [reference SELC-2815]
     */
    @Deprecated(forRemoval = true)
    @GET
    @Path("/{externalInstitutionId}/products/{productId}/onboarded-institution-info")
    @Operation(summary = "${openapi.onboarding.institutions.api.getInstitutionOnboardingInfo}",
            description = "${openapi.onboarding.institutions.api.getInstitutionOnboardingInfo}", operationId = "getInstitutionOnboardingInfoUsingGET_1", hidden = true)
    public InstitutionOnboardingInfoResource getInstitutionOnboardingInfo(@Parameter(description = "${openapi.onboarding.institutions.model.externalId}")
                                                                          @PathParam("externalInstitutionId")
                                                                          String externalInstitutionId,
                                                                          @Parameter(description = "${openapi.onboarding.product.model.id}")
                                                                          @PathParam("productId")
                                                                          String productId) {
        log.trace("getInstitutionOnBoardingInfo start");
        log.debug("getInstitutionOnBoardingInfo institutionId = {}, productId = {}",
                LogUtils.sanitize(externalInstitutionId), LogUtils.sanitize(productId));
        InstitutionOnboardingData institutionOnboardingData = institutionService.getInstitutionOnboardingData(externalInstitutionId, productId);
        InstitutionOnboardingInfoResource result = institutionMapper.toResource(institutionOnboardingData);
        log.debug("getInstitutionOnBoardingInfo result = {}", result);
        log.trace("getInstitutionOnBoardingInfo end");
        return result;
    }

}
