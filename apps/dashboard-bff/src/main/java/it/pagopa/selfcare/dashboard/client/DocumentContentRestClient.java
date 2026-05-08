package it.pagopa.selfcare.dashboard.client;

import it.pagopa.selfcare.dashboard.config.restclient.DocumentRestClientConfig;
import it.pagopa.selfcare.document.generated.openapi.v1.api.DocumentContentControllerApi;
import org.springframework.cloud.openfeign.FeignClient;

@FeignClient(name = "${rest-client.document-content-api.serviceCode}", url = "${rest-client.document.base-url}", configuration = DocumentRestClientConfig.class)
public interface DocumentContentRestClient extends DocumentContentControllerApi {
}
