package it.pagopa.selfcare.dashboard.client;

import it.pagopa.selfcare.dashboard.config.restclient.IamExternalRestClientConfig;
import it.pagopa.selfcare.iam.generated.openapi.v1.api.ExternalV2Api;
import org.springframework.cloud.openfeign.FeignClient;

@FeignClient(name = "${rest-client.iam-external.serviceCode}", url = "${rest-client.iam-external.base-url}", configuration = IamExternalRestClientConfig.class)
public interface IamExternalRestClient extends ExternalV2Api {
}
