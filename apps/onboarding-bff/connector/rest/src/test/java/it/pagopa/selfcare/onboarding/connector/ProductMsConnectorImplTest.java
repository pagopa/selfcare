package it.pagopa.selfcare.onboarding.connector;

import it.pagopa.selfcare.onboarding.connector.model.product.OriginResult;
import it.pagopa.selfcare.onboarding.connector.model.product.Product;
import it.pagopa.selfcare.onboarding.connector.model.product.RequiredDocumentModel;
import it.pagopa.selfcare.onboarding.connector.rest.client.MsProductApiClient;
import it.pagopa.selfcare.onboarding.connector.rest.mapper.ProductMapper;
import it.pagopa.selfcare.product.generated.openapi.v1.dto.InstitutionType;
import it.pagopa.selfcare.product.generated.openapi.v1.dto.Origin;
import it.pagopa.selfcare.product.generated.openapi.v1.dto.ProductOriginResponse;
import it.pagopa.selfcare.product.generated.openapi.v1.dto.ProductResponse;
import it.pagopa.selfcare.product.generated.openapi.v1.dto.RequiredDocumentResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ContextConfiguration;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ContextConfiguration(classes = {ProductMsConnectorImplTest.class})
@ExtendWith(MockitoExtension.class)
class ProductMsConnectorImplTest {

    @Mock
    private MsProductApiClient msProductApiClientMock;

    @Mock
    private ProductMapper productMapperMock;

    @InjectMocks
    private ProductMsConnectorImpl productMsConnector;

    @Test
    void getOriginsTest_success() {
        // given
        String tenantId = "AR";
        String productId = "product-test";

        ProductOriginResponse productOriginResponse = new ProductOriginResponse();
        OriginResult mappedResult = new OriginResult();
        mappedResult.setOrigins(List.of());

        when(msProductApiClientMock._getProductOriginsById(tenantId, productId))
                .thenReturn(ResponseEntity.ok(productOriginResponse));
        when(productMapperMock.toOriginResult(productOriginResponse))
                .thenReturn(mappedResult);

        // when
        OriginResult result = productMsConnector.getOrigins(tenantId, productId);

        // then
        assertNotNull(result);
        assertSame(mappedResult, result);

        verify(msProductApiClientMock, times(1))._getProductOriginsById(tenantId, productId);
        verify(productMapperMock, times(1)).toOriginResult(productOriginResponse);
        verifyNoMoreInteractions(msProductApiClientMock, productMapperMock);
    }

    @Test
    void getOriginsTest_nullBodyHandled() {
        // given
        String tenantId = "AR";
        String productId = "product-test";

        when(msProductApiClientMock._getProductOriginsById(tenantId, productId))
                .thenReturn(ResponseEntity.ok(null));

        OriginResult mappedResult = new OriginResult();
        mappedResult.setOrigins(List.of());

        when(productMapperMock.toOriginResult(null)).thenReturn(mappedResult);

        // when
        OriginResult result = productMsConnector.getOrigins(tenantId, productId);

        // then
        assertNotNull(result);
        assertSame(mappedResult, result);

        verify(msProductApiClientMock, times(1))._getProductOriginsById(tenantId, productId);
        verify(productMapperMock, times(1)).toOriginResult(null);
        verifyNoMoreInteractions(msProductApiClientMock, productMapperMock);
    }

    @Test
    void getRequiredDocuments_success() {
        // given
        String tenantId = "AR";
        String productId = "prod-test";
        String institutionType = "PA";
        String origin = "IPA";

        RequiredDocumentResponse dto = new RequiredDocumentResponse();
        dto.setId("doc-1");
        dto.setName("Statuto");
        dto.setRequired(true);

        RequiredDocumentModel model = new RequiredDocumentModel();
        model.setId("doc-1");
        model.setName("Statuto");
        model.setRequired(true);

        when(msProductApiClientMock._getRequiredDocuments(productId, tenantId, InstitutionType.PA, Origin.IPA))
                .thenReturn(ResponseEntity.ok(List.of(dto)));
        when(productMapperMock.toRequiredDocumentModelList(List.of(dto)))
                .thenReturn(List.of(model));

        // when
        List<RequiredDocumentModel> result = productMsConnector.getRequiredDocuments(tenantId, productId, institutionType, origin);

        // then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("doc-1", result.get(0).getId());

        verify(msProductApiClientMock, times(1))._getRequiredDocuments(productId, tenantId, InstitutionType.PA, Origin.IPA);
        verify(productMapperMock, times(1)).toRequiredDocumentModelList(List.of(dto));
        verifyNoMoreInteractions(msProductApiClientMock, productMapperMock);
    }

    @Test
    void getRequiredDocuments_emptyList() {
        // given
        String tenantId = "AR";
        String productId = "prod-test";
        String institutionType = "PA";
        String origin = "IPA";

        when(msProductApiClientMock._getRequiredDocuments(productId, tenantId, InstitutionType.PA, Origin.IPA))
                .thenReturn(ResponseEntity.ok(List.of()));
        when(productMapperMock.toRequiredDocumentModelList(List.of()))
                .thenReturn(List.of());

        // when
        List<RequiredDocumentModel> result = productMsConnector.getRequiredDocuments(tenantId, productId, institutionType, origin);

        // then
        assertNotNull(result);
        assertTrue(result.isEmpty());

        verify(msProductApiClientMock, times(1))._getRequiredDocuments(productId, tenantId, InstitutionType.PA, Origin.IPA);
    }

    @Test
    void isRequiredDocumentsEnabled_returnsTrue() {
        // given
        String tenantId = "AR";
        String productId = "prod-test";
        String institutionType = "PA";
        String origin = "IPA";

        when(msProductApiClientMock._isRequiredDocumentsEnabled(productId, tenantId, InstitutionType.PA, Origin.IPA))
                .thenReturn(responseWithFlag("true"));

        // when
        boolean result = productMsConnector.isRequiredDocumentsEnabled(tenantId, productId, institutionType, origin);

        // then
        assertTrue(result);

        verify(msProductApiClientMock, times(1))._isRequiredDocumentsEnabled(productId, tenantId, InstitutionType.PA, Origin.IPA);
        verifyNoMoreInteractions(msProductApiClientMock, productMapperMock);
    }

    @Test
    void isRequiredDocumentsEnabled_returnsFalse() {
        // given
        String tenantId = "AR";
        String productId = "prod-test";
        String institutionType = "PA";
        String origin = "IPA";

        when(msProductApiClientMock._isRequiredDocumentsEnabled(productId, tenantId, InstitutionType.PA, Origin.IPA))
                .thenReturn(responseWithFlag("false"));

        // when
        boolean result = productMsConnector.isRequiredDocumentsEnabled(tenantId, productId, institutionType, origin);

        // then
        assertFalse(result);

        verify(msProductApiClientMock, times(1))._isRequiredDocumentsEnabled(productId, tenantId, InstitutionType.PA, Origin.IPA);
    }

    @Test
    void isRequiredDocumentsEnabled_missingHeaderReturnsFalse() {
        // given
        String tenantId = "AR";
        String productId = "prod-test";
        String institutionType = "PA";
        String origin = "IPA";

        when(msProductApiClientMock._isRequiredDocumentsEnabled(productId, tenantId, InstitutionType.PA, Origin.IPA))
                .thenReturn(ResponseEntity.ok().build());

        // when
        boolean result = productMsConnector.isRequiredDocumentsEnabled(tenantId, productId, institutionType, origin);

        // then
        assertFalse(result);

        verify(msProductApiClientMock, times(1))._isRequiredDocumentsEnabled(productId, tenantId, InstitutionType.PA, Origin.IPA);
    }

    @Test
    void getProduct_mapsResponseAndUsesConfiguredTenant() {
        String productId = "prod-test";
        ProductResponse response = new ProductResponse();
        Product product = new Product();
        when(msProductApiClientMock._getProductById(productId, "AR")).thenReturn(ResponseEntity.ok(response));
        when(productMapperMock.toProduct(response)).thenReturn(product);

        assertSame(product, productMsConnector.getProduct(productId));

        verify(msProductApiClientMock)._getProductById(productId, "AR");
        verify(productMapperMock).toProduct(response);
        verifyNoMoreInteractions(msProductApiClientMock, productMapperMock);
    }

    @Test
    void getValidProduct_usesValidEndpoint() {
        String productId = "prod-test";
        ProductResponse response = new ProductResponse();
        Product product = new Product();
        when(msProductApiClientMock._getValidProductById(productId, "AR")).thenReturn(ResponseEntity.ok(response));
        when(productMapperMock.toProduct(response)).thenReturn(product);

        assertSame(product, productMsConnector.getValidProduct(productId));

        verify(msProductApiClientMock)._getValidProductById(productId, "AR");
        verify(productMapperMock).toProduct(response);
    }

    @Test
    void getProducts_requestsValidProductsAndMapsThem() {
        ProductResponse response = new ProductResponse();
        Product product = new Product();
        when(msProductApiClientMock._getProducts("AR", false, true)).thenReturn(ResponseEntity.ok(List.of(response)));
        when(productMapperMock.toProduct(response)).thenReturn(product);

        assertEquals(List.of(product), productMsConnector.getProducts(false));

        verify(msProductApiClientMock)._getProducts("AR", false, true);
        verify(productMapperMock).toProduct(response);
    }

    @Test
    void isProductEnabled_readsFeatureFromValidProduct() {
        Product product = new Product();
        product.setEnabled(true);
        ProductResponse response = new ProductResponse();
        when(msProductApiClientMock._getValidProductById("prod-test", "AR")).thenReturn(ResponseEntity.ok(response));
        when(productMapperMock.toProduct(response)).thenReturn(product);

        assertTrue(productMsConnector.isProductEnabled("prod-test"));
    }

    @Test
    void isAllowedByInstitutionTaxCode_ignoresCase() {
        Product product = new Product();
        product.setAllowedInstitutionTaxCode(List.of("ABC123"));
        ProductResponse response = new ProductResponse();
        when(msProductApiClientMock._getValidProductById("prod-test", "AR")).thenReturn(ResponseEntity.ok(response));
        when(productMapperMock.toProduct(response)).thenReturn(product);

        assertTrue(productMsConnector.isAllowedByInstitutionTaxCode("prod-test", "abc123"));
    }

    @Test
    void isAllowedByInstitutionTaxCode_returnsFalseWhenTaxCodeNotInList() {
        Product product = new Product();
        product.setAllowedInstitutionTaxCode(List.of("ABC123"));
        ProductResponse response = new ProductResponse();
        when(msProductApiClientMock._getValidProductById("prod-test", "AR")).thenReturn(ResponseEntity.ok(response));
        when(productMapperMock.toProduct(response)).thenReturn(product);

        assertFalse(productMsConnector.isAllowedByInstitutionTaxCode("prod-test", "XYZ999"));
    }

    @Test
    void isAllowedByInstitutionTaxCode_returnsFalseWhenListIsNull() {
        Product product = new Product();
        ProductResponse response = new ProductResponse();
        when(msProductApiClientMock._getValidProductById("prod-test", "AR")).thenReturn(ResponseEntity.ok(response));
        when(productMapperMock.toProduct(response)).thenReturn(product);

        assertFalse(productMsConnector.isAllowedByInstitutionTaxCode("prod-test", "ABC123"));
    }

    @Test
    void isProductEnabled_returnsFalseWhenProductDisabled() {
        Product product = new Product();
        product.setEnabled(false);
        ProductResponse response = new ProductResponse();
        when(msProductApiClientMock._getValidProductById("prod-test", "AR")).thenReturn(ResponseEntity.ok(response));
        when(productMapperMock.toProduct(response)).thenReturn(product);

        assertFalse(productMsConnector.isProductEnabled("prod-test"));
    }

    @Test
    void getProducts_rootOnlyIsPropagated() {
        when(msProductApiClientMock._getProducts("AR", true, true)).thenReturn(ResponseEntity.ok(List.of()));

        assertTrue(productMsConnector.getProducts(true).isEmpty());

        verify(msProductApiClientMock)._getProducts("AR", true, true);
        verifyNoInteractions(productMapperMock);
    }

    @Test
    void getProducts_nullBodyThrows() {
        when(msProductApiClientMock._getProducts("AR", false, true)).thenReturn(ResponseEntity.ok(null));

        assertThrows(NullPointerException.class, () -> productMsConnector.getProducts(false));
    }

    @Test
    void getProduct_nullBodyThrows() {
        when(msProductApiClientMock._getProductById("prod-test", "AR")).thenReturn(ResponseEntity.ok(null));

        assertThrows(NullPointerException.class, () -> productMsConnector.getProduct("prod-test"));
        verifyNoInteractions(productMapperMock);
    }

    @Test
    void getValidProduct_nullBodyThrows() {
        when(msProductApiClientMock._getValidProductById("prod-test", "AR")).thenReturn(ResponseEntity.ok(null));

        assertThrows(NullPointerException.class, () -> productMsConnector.getValidProduct("prod-test"));
        verifyNoInteractions(productMapperMock);
    }

    private static ResponseEntity<Void> responseWithFlag(String value) {
        return ResponseEntity.ok()
                .header(ProductMsConnectorImpl.HEADER_REQUIRED_DOCUMENTS_ENABLED, value)
                .build();
    }

}
