package it.pagopa.selfcare.mscore.connector.rest.client;


import it.pagopa.selfcare.mscore.connector.rest.config.UserRegistryRestClientConfig;
import it.pagopa.selfcare.user_registry.generated.openapi.v1.api.UserApi;
import org.springframework.cloud.openfeign.FeignClient;


@FeignClient(name = "${rest-client.user-registry.serviceCode}", url = "${rest-client.user-registry.base-url}", configuration = UserRegistryRestClientConfig.class)
public interface UserRegistryRestClient extends UserApi {
}
