package it.pagopa.selfcare.onboarding.core;

import it.pagopa.selfcare.onboarding.common.InstitutionType;
import it.pagopa.selfcare.onboarding.connector.api.ProductMsConnector;
import it.pagopa.selfcare.onboarding.connector.exceptions.ResourceNotFoundException;
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
    void getProducts_returnsOnlyActiveProducts() {
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

        assertEquals(List.of(enabledActiveProduct, disabledActiveProduct), result);
        verify(productMsConnector).getProducts(false);
        verifyNoMoreInteractions(productMsConnector);
    }

    @Test
    void getProducts_rootOnlyReturnsEmptyListWhenNoProducts() {
        when(productMsConnector.getProducts(true)).thenReturn(List.of());

        assertTrue(productService.getProducts(true).isEmpty());
        verify(productMsConnector).getProducts(true);
    }

    @Test
    void getProduct_delegatesToConnectorWithSanitizedId() {
        Product product = new Product();
        product.setId("prod-io");
        when(productMsConnector.getProduct(Encode.forJava("prod-io"))).thenReturn(product);

        Product result = productService.getProduct("prod-io", InstitutionType.PA);

        assertSame(product, result);
        verify(productMsConnector).getProduct(Encode.forJava("prod-io"));
        verifyNoMoreInteractions(productMsConnector);
    }

    @Test
    void getProduct_sanitizesSpecialCharacters() {
        String rawProductId = "prod\"io\n";
        Product product = new Product();
        when(productMsConnector.getProduct(Encode.forJava(rawProductId))).thenReturn(product);

        assertSame(product, productService.getProduct(rawProductId, null));
        verify(productMsConnector).getProduct(Encode.forJava(rawProductId));
    }

    @Test
    void getProduct_propagatesNotFound() {
        when(productMsConnector.getProduct("missing")).thenThrow(new ResourceNotFoundException("not found"));

        assertThrows(ResourceNotFoundException.class, () -> productService.getProduct("missing", null));
    }

    @Test
    void getProductValid_delegatesToConnector() {
        Product product = new Product();
        when(productMsConnector.getValidProduct(Encode.forJava("prod-io"))).thenReturn(product);

        assertSame(product, productService.getProductValid("prod-io"));
        verify(productMsConnector).getValidProduct(Encode.forJava("prod-io"));
        verifyNoMoreInteractions(productMsConnector);
    }

    @Test
    void isProductEnabled_delegatesToConnector() {
        when(productMsConnector.isProductEnabled("prod-io")).thenReturn(true);
        when(productMsConnector.isProductEnabled("prod-disabled")).thenReturn(false);

        assertTrue(productService.isProductEnabled("prod-io"));
        assertFalse(productService.isProductEnabled("prod-disabled"));
    }

    @Test
    void verifyAllowedByInstitutionTaxCode_delegatesToConnector() {
        when(productMsConnector.isAllowedByInstitutionTaxCode("prod-io", "ABC123")).thenReturn(true);
        when(productMsConnector.isAllowedByInstitutionTaxCode("prod-io", "XYZ999")).thenReturn(false);

        assertTrue(productService.verifyAllowedByInstitutionTaxCode("prod-io", "ABC123"));
        assertFalse(productService.verifyAllowedByInstitutionTaxCode("prod-io", "XYZ999"));
    }

}
