package it.pagopa.selfcare.onboarding.connector.rest.client;

import org.openapi.quarkus.product_json.api.ProductApi;
import org.openapi.quarkus.product_json.model.ProductOriginResponse;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.springframework.http.ResponseEntity;

@RegisterRestClient(configKey = "product_json")
public interface MsProductApiClient extends ProductApi {

    default ResponseEntity<ProductOriginResponse> _getProductOriginsById(String productId) {
        return ResponseEntity.ok(getProductOriginsById(productId).await().indefinitely());
    }
}
