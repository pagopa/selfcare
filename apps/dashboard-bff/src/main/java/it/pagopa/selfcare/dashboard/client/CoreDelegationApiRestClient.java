package it.pagopa.selfcare.dashboard.client;

import it.pagopa.selfcare.core.generated.openapi.v1.api.DelegationApi;
import it.pagopa.selfcare.dashboard.config.restclient.MsCoreRestClientConfig;
import org.springframework.cloud.openfeign.FeignClient;
@FeignClient(name = "${rest-client.ms-core-delegation-api.serviceCode}", url = "${rest-client.ms-core.base-url}", configuration = MsCoreRestClientConfig.class)
public interface CoreDelegationApiRestClient extends DelegationApi {
}
