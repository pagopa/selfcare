package it.pagopa.selfcare.onboarding.connector.rest.client;

import it.pagopa.selfcare.onboarding.generated.openapi.v1.api.BillingPortalApi;
import org.springframework.cloud.openfeign.FeignClient;

@FeignClient(name = "${rest-client.ms-onboarding-billing-api.serviceCode}", url = "${rest-client.ms-onboarding.base-url}")
public interface MsOnboardingBillingApiClient extends BillingPortalApi {
}
