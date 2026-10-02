package it.pagopa.selfcare.onboarding.core;

import it.pagopa.selfcare.onboarding.connector.api.ProductMsConnector;
import it.pagopa.selfcare.onboarding.connector.model.product.OriginResult;
import it.pagopa.selfcare.onboarding.connector.model.product.Product;
import it.pagopa.selfcare.onboarding.connector.model.product.ProductStatus;
import it.pagopa.selfcare.onboarding.connector.model.product.RequiredDocumentModel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.owasp.encoder.Encode;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceImplTest {

    @Mock
    private ProductMsConnector productMsConnector;

    @InjectMocks
    private ProductServiceImpl productService;

    @Test
    void getOriginsTest_success() {
        // given
        String tenantId = "AR";
        String productId = "prod-test";
        String sanitized = Encode.forJava(productId);

        OriginResult originResult = new OriginResult();
        originResult.setOrigins(List.of());

        when(productMsConnector.getOrigins(tenantId, sanitized)).thenReturn(originResult);

        // when
        OriginResult result = productService.getOrigins(tenantId, productId);

        // then
        assertNotNull(result);
        assertEquals(originResult, result);

        verify(productMsConnector, times(1)).getOrigins(tenantId, sanitized);
        verifyNoMoreInteractions(productMsConnector);
    }

    @Test
    void getOriginsTest_handlesSpecialCharacters() {
        // given
        String tenantId = "AR";
        String rawProductId = "<error>";
        String sanitized = Encode.forJava(rawProductId);

        OriginResult originResult = new OriginResult();
        originResult.setOrigins(List.of());

        when(productMsConnector.getOrigins(tenantId, sanitized)).thenReturn(originResult);

        // when
        OriginResult result = productService.getOrigins(tenantId, rawProductId);

        // then
        assertNotNull(result);
        verify(productMsConnector).getOrigins(tenantId, sanitized);
    }

    @Test
    void getOriginsTest_nullOriginsList_throwsException() {
        // given
        String tenantId = "AR";
        String productId = "test";
        String sanitized = Encode.forJava(productId);

        OriginResult originResult = new OriginResult();
        when(productMsConnector.getOrigins(tenantId, sanitized)).thenReturn(originResult);

        // then
        assertThrows(NullPointerException.class, () -> productService.getOrigins(tenantId, productId));
    }

    @Test
    void getRequiredDocuments_success() {
        // given
        String tenantId = "AR";
        String productId = "prod-test";
        String institutionType = "PA";
        String origin = "IPA";

        RequiredDocumentModel doc = new RequiredDocumentModel();
        doc.setId("doc-1");
        List<RequiredDocumentModel> expected = List.of(doc);

        when(productMsConnector.getRequiredDocuments(
                tenantId, Encode.forJava(productId), Encode.forJava(institutionType), Encode.forJava(origin)))
                .thenReturn(expected);

        // when
        List<RequiredDocumentModel> result = productService.getRequiredDocuments(tenantId, productId, institutionType, origin);

        // then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("doc-1", result.get(0).getId());

        verify(productMsConnector, times(1)).getRequiredDocuments(
                tenantId, Encode.forJava(productId), Encode.forJava(institutionType), Encode.forJava(origin));
        verifyNoMoreInteractions(productMsConnector);
    }

    @Test
    void getRequiredDocuments_empty() {
        // given
        String tenantId = "AR";
        String productId = "prod-test";
        String institutionType = "PA";
        String origin = "IPA";

        when(productMsConnector.getRequiredDocuments(
                tenantId, Encode.forJava(productId), Encode.forJava(institutionType), Encode.forJava(origin)))
                .thenReturn(List.of());

        // when
        List<RequiredDocumentModel> result = productService.getRequiredDocuments(tenantId, productId, institutionType, origin);

        // then
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void isRequiredDocumentsEnabled_returnsTrue() {
        // given
        String tenantId = "AR";
        String productId = "prod-test";
        String institutionType = "PA";
        String origin = "IPA";

        when(productMsConnector.isRequiredDocumentsEnabled(
                tenantId, Encode.forJava(productId), Encode.forJava(institutionType), Encode.forJava(origin)))
                .thenReturn(true);

        // when
        boolean result = productService.isRequiredDocumentsEnabled(tenantId, productId, institutionType, origin);

        // then
        assertTrue(result);

        verify(productMsConnector, times(1)).isRequiredDocumentsEnabled(
                tenantId, Encode.forJava(productId), Encode.forJava(institutionType), Encode.forJava(origin));
        verifyNoMoreInteractions(productMsConnector);
    }

    @Test
    void isRequiredDocumentsEnabled_returnsFalse() {
        // given
        String tenantId = "AR";
        String productId = "prod-test";
        String institutionType = "PA";
        String origin = "IPA";

        when(productMsConnector.isRequiredDocumentsEnabled(
                tenantId, Encode.forJava(productId), Encode.forJava(institutionType), Encode.forJava(origin)))
                .thenReturn(false);

        // when
        boolean result = productService.isRequiredDocumentsEnabled(tenantId, productId, institutionType, origin);

        // then
        assertFalse(result);
    }

    @Test
    void getProducts_returnsOnlyActiveAndEnabledProducts() {
        Product enabledActiveProduct = new Product();
        enabledActiveProduct.setId("enabled-active");
        enabledActiveProduct.setStatus(ProductStatus.ACTIVE);
        enabledActiveProduct.setEnabled(true);

        Product disabledActiveProduct = new Product();
        disabledActiveProduct.setId("disabled-active");
        disabledActiveProduct.setStatus(ProductStatus.ACTIVE);
        disabledActiveProduct.setEnabled(false);

        Product enabledTestingProduct = new Product();
        enabledTestingProduct.setId("enabled-testing");
        enabledTestingProduct.setStatus(ProductStatus.TESTING);
        enabledTestingProduct.setEnabled(true);

        when(productMsConnector.getProducts(false)).thenReturn(
                List.of(enabledActiveProduct, disabledActiveProduct, enabledTestingProduct));

        List<Product> result = productService.getProducts(false);

        assertEquals(List.of(enabledActiveProduct), result);
        verify(productMsConnector).getProducts(false);
        verifyNoMoreInteractions(productMsConnector);
    }

}
