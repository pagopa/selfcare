package it.pagopa.selfcare.external_api.client;

import it.pagopa.selfcare.external_api.client.config.MsCoreRestClientConfig;
import it.pagopa.selfcare.external_api.model.institution.*;
import it.pagopa.selfcare.external_api.model.onboarding.InstitutionSeed;
import it.pagopa.selfcare.external_api.model.onboarding.OnboardingImportInstitutionRequest;
import it.pagopa.selfcare.external_api.model.onboarding.OnboardingInfoResponse;
import it.pagopa.selfcare.external_api.model.pnpg.CreatePnPgInstitutionRequest;
import it.pagopa.selfcare.external_api.model.pnpg.InstitutionPnPgResponse;
import it.pagopa.selfcare.external_api.model.user.RelationshipState;
import org.springframework.cloud.openfeign.CollectionFormat;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.EnumSet;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.web.bind.annotation.RequestMethod.HEAD;

@FeignClient(name = "${rest-client.ms-core.serviceCode}", url = "${rest-client.ms-core.base-url}", configuration = MsCoreRestClientConfig.class)
public interface MsCoreRestClient {
    @PostMapping(value = "${rest-client.ms-core.createPnPgInstitution.path}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    InstitutionPnPgResponse createPnPgInstitution(@RequestBody CreatePnPgInstitutionRequest request);


    @GetMapping(value = "${rest-client.ms-core.onboardingInfo.path}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    OnboardingInfoResponse getInstitutionProductsInfo(@PathVariable(value = "userId") String userId);

    @GetMapping(value = "${rest-client.ms-core.onboardingInfo.path}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    OnboardingInfoResponse getInstitutionProductsInfo(@PathVariable(value = "userId") String userId,
                                                      @RequestParam(value="states") String[] states);

    @GetMapping(value = "${rest-client.ms-core.getOnBoardingInfo.path}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    @CollectionFormat(feign.CollectionFormat.CSV)
    OnBoardingInfo getOnBoardingInfo(@RequestParam(value = "institutionExternalId", required = false) String institutionExternalId,
                                     @RequestParam(value = "states", required = false) EnumSet<RelationshipState> states);


    @RequestMapping(method = HEAD, value = "${rest-client.ms-core.verifyOnboardingByExternalId.path}")
    @ResponseBody
    ResponseEntity<Void> verifyOnboarding(@PathVariable("externalId") String externalInstitutionId,
                                          @PathVariable("productId") String productId);

    @GetMapping(value = "${rest-client.ms-core.getInstitutionByExternalId.path}", produces = APPLICATION_JSON_VALUE)
    @ResponseBody
    Institution getInstitutionByExternalId(@PathVariable("externalId") String externalId);

    @PostMapping(value = "${rest-client.ms-core.createInstitutionUsingExternalId.path}", produces = APPLICATION_JSON_VALUE)
    @ResponseBody
    Institution createInstitutionUsingExternalId(@PathVariable("externalId") String externalId);

    @PostMapping(value = "${rest-client.ms-core.createInstitutionRaw.path}", produces = APPLICATION_JSON_VALUE)
    @ResponseBody
    Institution createInstitutionRaw(@PathVariable("externalId") String externalId,
                                     @RequestBody InstitutionSeed institutionSeed);

    @PostMapping(value = "${rest-client.ms-core.onboardingOrganization.path}", consumes = APPLICATION_JSON_VALUE)
    @ResponseBody
    void onboardingOrganization(@RequestBody OnboardingImportInstitutionRequest request);

    @GetMapping(value = "${rest-client.ms-core.getInstitution.path}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    Institution getInstitution(@PathVariable(value = "id") String id);

    @PostMapping(value = "${rest-client.ms-core.createInstitutionFromAnac.path}", produces = APPLICATION_JSON_VALUE)
    @ResponseBody
    InstitutionResponse createInstitutionFromANAC(@RequestBody InstitutionSeed institutionSeed);

    @PostMapping(value = "${rest-client.ms-core.createInstitutionFromIvass.path}", produces = APPLICATION_JSON_VALUE)
    @ResponseBody
    InstitutionResponse createInstitutionFromIVASS(@RequestBody InstitutionSeed institutionSeed);


    @GetMapping(value = "${rest-client.ms-core.findByGeoTaxonomies.path}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    @CollectionFormat(feign.CollectionFormat.CSV)
    Institutions getInstitutionsByGeoTaxonomies(@RequestParam(value = "geoTaxonomies") String geoTaxonomies,
                                                @RequestParam(value = "searchMode", required = false) SearchMode searchMode);
}
