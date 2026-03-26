package it.pagopa.selfcare.onboarding.connector.rest.client;

import it.pagopa.selfcare.onboarding.generated.openapi.v1.api.InternalV1Api;
import org.springframework.cloud.openfeign.FeignClient;

@FeignClient(name = "${rest-client.ms-onboarding-internal-api.serviceCode}", url = "${rest-client.ms-onboarding.base-url}")
public interface MsOnboardingInternalApiClient extends InternalV1Api {
}
