package it.pagopa.selfcare.onboarding.connector;

import it.pagopa.selfcare.onboarding.connector.api.ProductMsConnector;
import it.pagopa.selfcare.onboarding.connector.model.product.OriginResult;
import it.pagopa.selfcare.onboarding.connector.model.product.RequiredDocumentModel;
import it.pagopa.selfcare.onboarding.connector.model.product.Product;
import it.pagopa.selfcare.onboarding.connector.rest.client.MsProductApiClient;
import it.pagopa.selfcare.onboarding.connector.rest.mapper.ProductMapper;
import it.pagopa.selfcare.product.generated.openapi.v1.dto.InstitutionType;
import it.pagopa.selfcare.product.generated.openapi.v1.dto.Origin;
import it.pagopa.selfcare.product.generated.openapi.v1.dto.ProductOriginResponse;
import it.pagopa.selfcare.product.generated.openapi.v1.dto.RequiredDocumentResponse;
import it.pagopa.selfcare.product.generated.openapi.v1.dto.ProductResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@Service
@Slf4j
public class ProductMsConnectorImpl implements ProductMsConnector {

    private final MsProductApiClient msProductApiClient;
    private final ProductMapper productMapper;

    @Value("${onboarding-bff.product.tenant-id:AR}")
    private String productTenantId = "AR";

    static final String HEADER_REQUIRED_DOCUMENTS_ENABLED = "X-Required-Documents-Enabled";

    public ProductMsConnectorImpl(MsProductApiClient msProductApiClient, ProductMapper productMapper) {
        this.msProductApiClient = msProductApiClient;
        this.productMapper = productMapper;
    }

    @Override
    public OriginResult getOrigins(String tenantId, String productId) {
        log.trace("getOrigins start");
        ResponseEntity<ProductOriginResponse> origins = msProductApiClient._getProductOriginsById(productId, tenantId);
        OriginResult entryList = productMapper.toOriginResult(origins.getBody());
        log.debug("getOrigins size = {}", entryList.getOrigins().isEmpty());
        log.trace("getOrigins end");
        return entryList;
    }

    @Override
    public List<RequiredDocumentModel> getRequiredDocuments(String tenantId, String productId, String institutionType, String origin) {
        log.trace("getRequiredDocuments start");
        ResponseEntity<List<RequiredDocumentResponse>> response = msProductApiClient._getRequiredDocuments(
                productId,
                InstitutionType.fromValue(institutionType),
                Origin.fromValue(origin),
                tenantId
        );
        List<RequiredDocumentModel> result = productMapper.toRequiredDocumentModelList(
                Objects.requireNonNull(response.getBody()));
        log.debug("getRequiredDocuments size = {}", result.size());
        log.trace("getRequiredDocuments end");
        return result;
    }

    @Override
    public boolean isRequiredDocumentsEnabled(String tenantId, String productId, String institutionType, String origin) {
        log.trace("isRequiredDocumentsEnabled start");
        ResponseEntity<Void> response = msProductApiClient._isRequiredDocumentsEnabled(
                productId,
                InstitutionType.fromValue(institutionType),
                Origin.fromValue(origin),
                tenantId
        );
        boolean result = Boolean.parseBoolean(response.getHeaders().getFirst(HEADER_REQUIRED_DOCUMENTS_ENABLED));
        log.debug(
            "isRequiredDocumentsEnabled given productId = {}, institutionType = {}, origin = {} result = {}",
            productId,
            institutionType,
            origin,
            result);
        log.trace("isRequiredDocumentsEnabled end");
        return result;
    }

    @Override
    public Product getProduct(String productId) {
        ResponseEntity<ProductResponse> response = msProductApiClient._getProductById(productId, productTenantId);
        return productMapper.toProduct(Objects.requireNonNull(response.getBody()));
    }

    @Override
    public Product getValidProduct(String productId) {
        ResponseEntity<ProductResponse> response = msProductApiClient._getValidProductById(productId, productTenantId);
        return productMapper.toProduct(Objects.requireNonNull(response.getBody()));
    }

    @Override
    public List<Product> getProducts(boolean rootOnly) {
        ResponseEntity<List<ProductResponse>> response = msProductApiClient._getProducts(rootOnly, true, productTenantId);
        return Objects.requireNonNull(response.getBody()).stream().map(productMapper::toProduct).toList();
    }

    @Override
    public boolean isProductEnabled(String productId) {
        return getValidProduct(productId).isEnabled();
    }

    @Override
    public boolean isAllowedByInstitutionTaxCode(String productId, String institutionTaxCode) {
        List<String> allowedInstitutionTaxCodes = getValidProduct(productId).getAllowedInstitutionTaxCode();
        return allowedInstitutionTaxCodes != null && allowedInstitutionTaxCodes.stream()
                .anyMatch(allowedTaxCode -> allowedTaxCode.equalsIgnoreCase(institutionTaxCode));
    }
}
