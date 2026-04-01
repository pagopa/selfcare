package it.pagopa.selfcare.onboarding.connector.rest.client;

import it.pagopa.selfcare.onboarding.connector.model.institutions.Institution;
import it.pagopa.selfcare.onboarding.connector.model.onboarding.CreateInstitutionData;
import it.pagopa.selfcare.onboarding.connector.rest.model.OnboardingInstitutionRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.web.bind.annotation.RequestMethod.HEAD;

/**
 * Ms Core Rest Client
 */
@FeignClient(name = "${rest-client.ms-core.serviceCode}", url = "${rest-client.ms-core.base-url}")
public interface MsCoreRestClient {

    @PostMapping(value = "${rest-client.ms-core.onboardingOrganization.path}", consumes = APPLICATION_JSON_VALUE)
    @ResponseBody
    void onboardingOrganization(@RequestBody OnboardingInstitutionRequest request);

    @GetMapping(value = "${rest-client.ms-core.getInstitutionByExternalId.path}", produces = APPLICATION_JSON_VALUE)
    @ResponseBody
    Institution getInstitutionByExternalId(@PathVariable("externalId") String externalId);

    @PostMapping(value = "${rest-client.ms-core.createInstitutionUsingInstitutionData.path}", consumes = APPLICATION_JSON_VALUE)
    @ResponseBody
    Institution createInstitutionUsingInstitutionData(@RequestBody CreateInstitutionData request);

    @RequestMapping(method = HEAD, value = "${rest-client.ms-core.verifyOnboarding.path}")
    @ResponseBody
    void verifyOnboarding(@PathVariable("externalId") String externalInstitutionId,
                          @PathVariable("productId") String productId);

}
