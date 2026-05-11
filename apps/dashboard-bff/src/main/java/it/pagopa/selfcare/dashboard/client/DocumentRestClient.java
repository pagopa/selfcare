package it.pagopa.selfcare.dashboard.client;

import it.pagopa.selfcare.dashboard.config.restclient.DocumentRestClientConfig;
import it.pagopa.selfcare.document.generated.openapi.v1.api.DocumentControllerApi;
import org.springframework.cloud.openfeign.FeignClient;

@FeignClient(name = "${rest-client.document-api.serviceCode}", url = "${rest-client.document.base-url}", configuration = DocumentRestClientConfig.class)
public interface DocumentRestClient extends DocumentControllerApi {
}
