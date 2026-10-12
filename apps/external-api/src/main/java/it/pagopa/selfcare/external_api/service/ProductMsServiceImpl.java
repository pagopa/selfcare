package it.pagopa.selfcare.external_api.service;

import it.pagopa.selfcare.external_api.client.MsProductApiClient;
import it.pagopa.selfcare.product.generated.openapi.v1.dto.ProductStatus;
import it.pagopa.selfcare.product.generated.openapi.v1.dto.ProductResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.util.List;
import java.util.Objects;

@Service
public class ProductMsServiceImpl implements ProductMsService {

    private static final String TENANT_ID = null;

    private final MsProductApiClient msProductApiClient;

    public ProductMsServiceImpl(MsProductApiClient msProductApiClient) {
        this.msProductApiClient = msProductApiClient;
    }

    @Override
    public List<ProductResponse> getProducts(boolean rootOnly, boolean valid) {
        ResponseEntity<List<ProductResponse>> response =
                msProductApiClient._getProducts(rootOnly, valid, TENANT_ID);
        if (response == null || response.getBody() == null) {
            return List.of();
        }
        return response.getBody().stream()
                .filter(Objects::nonNull)
                .filter(product -> !rootOnly || product.getParentId() == null)
                .filter(product -> !valid || isSdkValidStatus(product.getStatus()))
                .toList();
    }

    @Override
    public ProductResponse getProductRaw(String productId) {
        Assert.notNull(productId, "ProductId is required");
        return Objects.requireNonNull(
                msProductApiClient._getProductById(productId, TENANT_ID).getBody());
    }

    private static boolean isSdkValidStatus(ProductStatus status) {
        return status != ProductStatus.INACTIVE
                && status != ProductStatus.PHASE_OUT
                && status != ProductStatus.DELETED
                && status != ProductStatus.SUSPEND;
    }
}


