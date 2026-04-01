package it.pagopa.selfcare.onboarding.connector.rest.client;


import it.pagopa.selfcare.user.generated.openapi.v1.api.UserControllerApi;
import org.springframework.cloud.openfeign.FeignClient;

@FeignClient(name = "${rest-client.ms-user-api.serviceCode}", url = "${rest-client.ms-user.base-url}")
public interface MsUserApiClient extends UserControllerApi {
}
