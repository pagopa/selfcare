package it.pagopa.selfcare.onboarding.connector.rest.client;

import it.pagopa.selfcare.commons.base.security.PartyRole;
import it.pagopa.selfcare.core.generated.openapi.v1.api.OnboardingApi;
import it.pagopa.selfcare.onboarding.connector.model.RelationshipInfo;
import it.pagopa.selfcare.onboarding.connector.model.RelationshipState;
import it.pagopa.selfcare.onboarding.connector.model.RelationshipsResponse;
import it.pagopa.selfcare.onboarding.connector.rest.model.*;
import org.springframework.cloud.openfeign.CollectionFormat;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.EnumSet;
import java.util.Set;

import static feign.CollectionFormat.CSV;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.web.bind.annotation.RequestMethod.HEAD;

/**
 * Party Process Rest Client
 */
@FeignClient(name = "${rest-client.party-process.serviceCode}", url = "${rest-client.party-process.base-url}")
public interface PartyProcessRestClient extends OnboardingApi {

    @GetMapping(value = "${rest-client.party-process.getUserInstitutionRelationships.path}", produces = APPLICATION_JSON_VALUE)
    @ResponseBody
    @CollectionFormat(CSV)
    RelationshipsResponse getUserInstitutionRelationships(@PathVariable("externalId") String institutionId,
                                                          @RequestParam(value = "roles", required = false) EnumSet<PartyRole> roles,
                                                          @RequestParam(value = "states", required = false) EnumSet<RelationshipState> states,
                                                          @RequestParam(value = "products", required = false) Set<String> productIds,
                                                          @RequestParam(value = "productRoles", required = false) Set<String> productRoles,
                                                          @RequestParam(value = "personId", required = false) String personId);

    @PostMapping(value = "${rest-client.party-process.onboardingOrganization.path}", consumes = APPLICATION_JSON_VALUE)
    @ResponseBody
    void onboardingOrganization(@RequestBody OnboardingInstitutionRequest request);
    @GetMapping(value = "${rest-client.party-process.getOnboardings.path}", produces = APPLICATION_JSON_VALUE)
    @ResponseBody
    OnboardingsResponse getOnboardings(@PathVariable(value = "institutionId") String institutionId,
                                       @RequestParam(value = "productId", required = false) String productId);

    @GetMapping(value = "${rest-client.party-process.getInstitutionByExternalId.path}", produces = APPLICATION_JSON_VALUE)
    @ResponseBody
    InstitutionResponse getInstitutionByExternalId(@PathVariable("externalId") String externalId);

    @GetMapping(value = "${rest-client.party-process.getInstitutions.path}", produces = APPLICATION_JSON_VALUE)
    @ResponseBody
    InstitutionsResponse getInstitutions(@RequestParam("taxCode") String taxCode,
                                        @RequestParam(value = "subunitCode", required = false) String subunitCode);

    @PostMapping(value = "${rest-client.party-process.createInstitutionFromIpa.path}", produces = APPLICATION_JSON_VALUE)
    @ResponseBody
    InstitutionResponse createInstitutionFromIpa(@RequestBody InstitutionFromIpaPost institutionFromIpaPost);

    @PostMapping(value = "${rest-client.party-process.createInstitutionFromAnac.path}", produces = APPLICATION_JSON_VALUE)
    @ResponseBody
    InstitutionResponse createInstitutionFromANAC(@RequestBody InstitutionSeed institutionSeed);

    @PostMapping(value = "${rest-client.party-process.createInstitutionFromIvass.path}", produces = APPLICATION_JSON_VALUE)
    @ResponseBody
    InstitutionResponse createInstitutionFromIVASS(@RequestBody InstitutionSeed institutionSeed);

    @PostMapping(value = "${rest-client.party-process.createInstitutionUsingExternalId.path}", produces = APPLICATION_JSON_VALUE)
    @ResponseBody
    InstitutionResponse createInstitutionUsingExternalId(@PathVariable("externalId") String externalId);

    @PostMapping(value = "${rest-client.party-process.createInstitutionFromInfocamere.path}", produces = APPLICATION_JSON_VALUE)
    @ResponseBody
    InstitutionResponse createInstitutionFromInfocamere(@RequestBody InstitutionSeed institutionSeed);

    @PostMapping(value = "${rest-client.party-process.createInstitution.path}", produces = APPLICATION_JSON_VALUE)
    @ResponseBody
    InstitutionResponse createInstitution(@RequestBody InstitutionSeed institutionSeed);

    @GetMapping(value = "${rest-client.party-process.getInstitutionManager.path}", produces = APPLICATION_JSON_VALUE)
    @ResponseBody
    RelationshipInfo getInstitutionManager(@PathVariable("externalId") String externalId,
                                           @PathVariable("productId") String productId);

    @GetMapping(value = "${rest-client.party-process.getInstitutionBillingData.path}", produces = APPLICATION_JSON_VALUE)
    @ResponseBody
    BillingDataResponse getInstitutionBillingData(@PathVariable("externalId") String externalId,
                                                  @PathVariable("productId") String productId);

    @RequestMapping(method = HEAD, value = "${rest-client.party-process.verifyOnboardingByExternalId.path}")
    @ResponseBody
    void verifyOnboarding(@PathVariable("externalId") String externalInstitutionId,
                          @PathVariable("productId") String productId);

    @RequestMapping(method = HEAD, value = "${rest-client.party-process.verifyOnboarding.path}")
    @ResponseBody
    void verifyOnboarding(@RequestParam("taxCode") String taxCode,
                          @RequestParam("subunitCode") String subunitCode,
                          @RequestParam("productId") String productId);

    @GetMapping(value = "${rest-client.party-process.getInstitutionById.path}", produces = APPLICATION_JSON_VALUE)
    @ResponseBody
    InstitutionResponse getInstitutionById(@PathVariable("institutionId") String institutionId);

}
