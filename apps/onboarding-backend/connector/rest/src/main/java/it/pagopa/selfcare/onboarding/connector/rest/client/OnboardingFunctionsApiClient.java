package it.pagopa.selfcare.onboarding.connector.rest.client;


import it.pagopa.selfcare.onboarding_functions.generated.openapi.v1.api.OrganizationApi;
import org.springframework.cloud.openfeign.FeignClient;

@FeignClient(name = "${rest-client.onboarding-functions-api.serviceCode}", url = "${rest-client.onboarding-functions-api.baseUrl}")
public interface OnboardingFunctionsApiClient extends OrganizationApi {
}
