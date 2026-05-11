package it.pagopa.selfcare.external_api.client;

import it.pagopa.selfcare.document.generated.openapi.v1.api.DocumentControllerApi;
import it.pagopa.selfcare.external_api.client.config.MsDocumentApiClientConfig;
import org.springframework.cloud.openfeign.FeignClient;

@FeignClient(
        name = "${rest-client.ms-document-api.serviceCode}",
        url = "${rest-client.ms-document.base-url}",
        configuration = MsDocumentApiClientConfig.class
)
public interface MsDocumentApiClient extends DocumentControllerApi {
}
