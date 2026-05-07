package it.pagopa.selfcare.dashboard.client;

import it.pagopa.selfcare.core.generated.openapi.v1.api.ManagementApi;
import it.pagopa.selfcare.dashboard.config.restclient.MsCoreRestClientConfig;
import org.springframework.cloud.openfeign.FeignClient;

@FeignClient(name = "${rest-client.ms-core-management-api.serviceCode}", url = "${rest-client.ms-core.base-url}", configuration = MsCoreRestClientConfig.class)
public interface CoreManagementApiRestClient extends ManagementApi {
}
