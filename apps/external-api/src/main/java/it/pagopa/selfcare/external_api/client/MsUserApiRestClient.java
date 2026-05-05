package it.pagopa.selfcare.external_api.client;

import it.pagopa.selfcare.external_api.client.config.MsUserApiClientConfig;
import it.pagopa.selfcare.user.generated.openapi.v1.api.UserApi;
import org.springframework.cloud.openfeign.FeignClient;

@FeignClient(name = "${rest-client.ms-user-api.serviceCode}", url = "${rest-client.ms-user-api.base-url}", configuration = MsUserApiClientConfig.class)
public interface MsUserApiRestClient extends UserApi {
}
