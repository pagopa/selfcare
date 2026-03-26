package it.pagopa.selfcare.onboarding.connector.rest.client;

import it.pagopa.selfcare.onboarding.generated.openapi.v1.api.SupportApi;
import org.springframework.cloud.openfeign.FeignClient;


@FeignClient(name = "${rest-client.ms-onboarding-support-api.serviceCode}", url = "${rest-client.ms-onboarding.base-url}")
public interface MsOnboardingSupportApiClient extends SupportApi {
}
