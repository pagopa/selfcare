package it.pagopa.selfcare.dashboard.client;

import it.pagopa.selfcare.dashboard.config.restclient.IamRestClientConfig;
import it.pagopa.selfcare.iam.generated.openapi.v1.api.IamApi;
import org.springframework.cloud.openfeign.FeignClient;

@FeignClient(name = "${rest-client.iam.serviceCode}", url = "${rest-client.iam.base-url}", configuration = IamRestClientConfig.class)
public interface IamRestClient extends IamApi {
}
