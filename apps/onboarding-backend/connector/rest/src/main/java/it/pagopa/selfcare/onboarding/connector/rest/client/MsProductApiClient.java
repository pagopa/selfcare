package it.pagopa.selfcare.onboarding.connector.rest.client;

import it.pagopa.selfcare.product.generated.openapi.v1.api.ProductApi;
import org.springframework.cloud.openfeign.FeignClient;

@FeignClient(name = "${rest-client.ms-product-api.serviceCode}", url = "${rest-client.ms-product.base-url}")
public interface MsProductApiClient extends ProductApi {
}
