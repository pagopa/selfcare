package it.pagopa.selfcare.dashboard.client;

import it.pagopa.selfcare.dashboard.config.restclient.OnboardingRestClientConfig;
import it.pagopa.selfcare.onboarding.generated.openapi.v1.api.OnboardingControllerApi;
import org.springframework.cloud.openfeign.FeignClient;

@FeignClient(name = "${rest-client.onboarding-api.serviceCode}", url = "${rest-client.onboarding.base-url}", configuration = OnboardingRestClientConfig.class)
public interface OnboardingRestClient extends OnboardingControllerApi {
}
