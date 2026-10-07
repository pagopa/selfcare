package it.pagopa.selfcare.external_api.client;

import it.pagopa.selfcare.external_api.client.config.MsProductApiClientConfig;
import it.pagopa.selfcare.product.generated.openapi.v1.api.ProductApi;
import org.springframework.cloud.openfeign.FeignClient;

@FeignClient(
        name = "${rest-client.ms-product-api.serviceCode}",
        url = "${rest-client.ms-product-api.base-url}",
        configuration = MsProductApiClientConfig.class)
public interface MsProductApiClient extends ProductApi {
}

