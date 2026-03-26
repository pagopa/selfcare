package it.pagopa.selfcare.onboarding.connector.rest.client;

import it.pagopa.selfcare.onboarding.generated.openapi.v1.api.AggregatesControllerApi;
import org.springframework.cloud.openfeign.FeignClient;

@FeignClient(name = "${rest-client.ms-onboarding-aggregates-api.serviceCode}", url = "${rest-client.ms-onboarding.base-url}")
public interface MsOnboardingAggregatesApiClient extends AggregatesControllerApi {
}
