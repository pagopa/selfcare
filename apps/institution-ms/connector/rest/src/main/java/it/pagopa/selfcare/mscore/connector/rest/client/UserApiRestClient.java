package it.pagopa.selfcare.mscore.connector.rest.client;

import it.pagopa.selfcare.mscore.connector.rest.config.UserApiRestClientConfig;
import it.pagopa.selfcare.user.generated.openapi.v1.api.UserControllerApi;
import org.springframework.cloud.openfeign.FeignClient;

@FeignClient(name = "${rest-client.user-ms.serviceCode}", url = "${rest-client.user-ms.base-url}", configuration = UserApiRestClientConfig.class)
public interface UserApiRestClient extends UserControllerApi {
}
