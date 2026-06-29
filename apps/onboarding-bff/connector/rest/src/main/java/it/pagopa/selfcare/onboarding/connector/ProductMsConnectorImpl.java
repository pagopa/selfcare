package it.pagopa.selfcare.onboarding.connector;

import it.pagopa.selfcare.onboarding.connector.api.ProductMsConnector;
import it.pagopa.selfcare.onboarding.connector.model.product.OriginResult;
import it.pagopa.selfcare.onboarding.connector.model.product.RequiredDocumentModel;
import it.pagopa.selfcare.onboarding.connector.rest.client.MsProductApiClient;
import it.pagopa.selfcare.onboarding.connector.rest.mapper.ProductMapper;
import it.pagopa.selfcare.product.generated.openapi.v1.dto.InstitutionType;
import it.pagopa.selfcare.product.generated.openapi.v1.dto.Origin;
import it.pagopa.selfcare.product.generated.openapi.v1.dto.ProductOriginResponse;
import it.pagopa.selfcare.product.generated.openapi.v1.dto.RequiredDocumentResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@Service
@Slf4j
public class ProductMsConnectorImpl implements ProductMsConnector {

    private final MsProductApiClient msProductApiClient;
    private final ProductMapper productMapper;

    public ProductMsConnectorImpl(MsProductApiClient msProductApiClient, ProductMapper productMapper) {
        this.msProductApiClient = msProductApiClient;
        this.productMapper = productMapper;
    }

    @Override
    public OriginResult getOrigins(String productId) {
        log.trace("getOrigins start");
        ResponseEntity<ProductOriginResponse> origins = msProductApiClient._getProductOriginsById(productId);
        OriginResult entryList = productMapper.toOriginResult(origins.getBody());
        log.debug("getOrigins size = {}", entryList.getOrigins().isEmpty());
        log.trace("getOrigins end");
        return entryList;
    }

    @Override
    public List<RequiredDocumentModel> getRequiredDocuments(String productId, String institutionType, String origin) {
        log.trace("getRequiredDocuments start");
        ResponseEntity<List<RequiredDocumentResponse>> response = msProductApiClient._getRequiredDocuments(
                productId,
                InstitutionType.fromValue(institutionType),
                Origin.fromValue(origin)
        );
        List<RequiredDocumentModel> result = productMapper.toRequiredDocumentModelList(
                Objects.requireNonNull(response.getBody()));
        log.debug("getRequiredDocuments size = {}", result.size());
        log.trace("getRequiredDocuments end");
        return result;
    }

    @Override
    public boolean isRequiredDocumentsEnabled(String productId, String institutionType, String origin) {
        log.trace("isRequiredDocumentsEnabled start");
        ResponseEntity<Boolean> response = msProductApiClient._isRequiredDocumentsEnabled(
                productId,
                InstitutionType.fromValue(institutionType),
                Origin.fromValue(origin)
        );
        boolean result = Boolean.TRUE.equals(response.getBody());
        log.debug("isRequiredDocumentsEnabled result = {}", result);
        log.trace("isRequiredDocumentsEnabled end");
        return result;
    }
}
