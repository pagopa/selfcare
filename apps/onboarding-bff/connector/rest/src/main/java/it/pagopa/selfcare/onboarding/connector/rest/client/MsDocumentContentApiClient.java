package it.pagopa.selfcare.onboarding.connector.rest.client;

import it.pagopa.selfcare.document.generated.openapi.v1.api.DocumentContentControllerApi;
import it.pagopa.selfcare.onboarding.connector.rest.config.MultipartFeignConfig;
import org.springframework.cloud.openfeign.FeignClient;

@FeignClient(
        name = "${rest-client.ms-document-content-api.serviceCode}",
        url = "${rest-client.ms-document.base-url}",
        configuration = MultipartFeignConfig.class)
public interface MsDocumentContentApiClient extends DocumentContentControllerApi {
}
