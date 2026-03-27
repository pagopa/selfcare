package it.pagopa.selfcare.onboarding.connector.rest.client;

import it.pagopa.selfcare.onboarding.generated.openapi.v1.api.OnboardingControllerApi;
import org.springframework.cloud.openfeign.FeignClient;


@FeignClient(name = "${rest-client.ms-onboarding-api.serviceCode}", url = "${rest-client.ms-onboarding.base-url}")
public interface MsOnboardingApiClient extends OnboardingControllerApi {
}
