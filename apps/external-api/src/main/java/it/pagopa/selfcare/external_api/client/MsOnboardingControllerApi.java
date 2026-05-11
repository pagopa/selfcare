package it.pagopa.selfcare.external_api.client;

import it.pagopa.selfcare.external_api.client.config.MsOnboardingApiClientConfig;
import it.pagopa.selfcare.onboarding.generated.openapi.v1.api.OnboardingControllerApi;
import org.springframework.cloud.openfeign.FeignClient;


@FeignClient(name = "${rest-client.ms-onboarding-api.serviceCode}", url = "${rest-client.ms-onboarding.base-url}", configuration = MsOnboardingApiClientConfig.class)
public interface MsOnboardingControllerApi extends OnboardingControllerApi {
}
