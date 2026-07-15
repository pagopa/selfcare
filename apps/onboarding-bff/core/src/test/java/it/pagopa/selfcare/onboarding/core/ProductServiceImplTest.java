package it.pagopa.selfcare.onboarding.core;

import it.pagopa.selfcare.onboarding.connector.api.ProductMsConnector;
import it.pagopa.selfcare.onboarding.connector.model.product.OriginResult;
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
        String productId = "prod-test";
        String sanitized = Encode.forJava(productId);

        OriginResult originResult = new OriginResult();
        originResult.setOrigins(List.of());

        when(productMsConnector.getOrigins(sanitized)).thenReturn(originResult);

        // when
        OriginResult result = productService.getOrigins(productId);

        // then
        assertNotNull(result);
        assertEquals(originResult, result);

        verify(productMsConnector, times(1)).getOrigins(sanitized);
        verifyNoMoreInteractions(productMsConnector);
    }

    @Test
    void getOriginsTest_handlesSpecialCharacters() {
        // given
        String rawProductId = "<error>";
        String sanitized = Encode.forJava(rawProductId);

        OriginResult originResult = new OriginResult();
        originResult.setOrigins(List.of());

        when(productMsConnector.getOrigins(sanitized)).thenReturn(originResult);

        // when
        OriginResult result = productService.getOrigins(rawProductId);

        // then
        assertNotNull(result);
        verify(productMsConnector).getOrigins(sanitized);
    }

    @Test
    void getOriginsTest_nullOriginsList_throwsException() {
        // given
        String productId = "test";
        String sanitized = Encode.forJava(productId);

        OriginResult originResult = new OriginResult();
        when(productMsConnector.getOrigins(sanitized)).thenReturn(originResult);

        // then
        assertThrows(NullPointerException.class, () -> productService.getOrigins(productId));
    }

    @Test
    void getRequiredDocuments_success() {
        // given
        String productId = "prod-test";
        String institutionType = "PA";
        String origin = "IPA";

        RequiredDocumentModel doc = new RequiredDocumentModel();
        doc.setId("doc-1");
        List<RequiredDocumentModel> expected = List.of(doc);

        when(productMsConnector.getRequiredDocuments(
                Encode.forJava(productId), Encode.forJava(institutionType), Encode.forJava(origin)))
                .thenReturn(expected);

        // when
        List<RequiredDocumentModel> result = productService.getRequiredDocuments(productId, institutionType, origin);

        // then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("doc-1", result.get(0).getId());

        verify(productMsConnector, times(1)).getRequiredDocuments(
                Encode.forJava(productId), Encode.forJava(institutionType), Encode.forJava(origin));
        verifyNoMoreInteractions(productMsConnector);
    }

    @Test
    void getRequiredDocuments_empty() {
        // given
        String productId = "prod-test";
        String institutionType = "PA";
        String origin = "IPA";

        when(productMsConnector.getRequiredDocuments(
                Encode.forJava(productId), Encode.forJava(institutionType), Encode.forJava(origin)))
                .thenReturn(List.of());

        // when
        List<RequiredDocumentModel> result = productService.getRequiredDocuments(productId, institutionType, origin);

        // then
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void isRequiredDocumentsEnabled_returnsTrue() {
        // given
        String productId = "prod-test";
        String institutionType = "PA";
        String origin = "IPA";

        when(productMsConnector.isRequiredDocumentsEnabled(
                Encode.forJava(productId), Encode.forJava(institutionType), Encode.forJava(origin)))
                .thenReturn(true);

        // when
        boolean result = productService.isRequiredDocumentsEnabled(productId, institutionType, origin);

        // then
        assertTrue(result);

        verify(productMsConnector, times(1)).isRequiredDocumentsEnabled(
                Encode.forJava(productId), Encode.forJava(institutionType), Encode.forJava(origin));
        verifyNoMoreInteractions(productMsConnector);
    }

    @Test
    void isRequiredDocumentsEnabled_returnsFalse() {
        // given
        String productId = "prod-test";
        String institutionType = "PA";
        String origin = "IPA";

        when(productMsConnector.isRequiredDocumentsEnabled(
                Encode.forJava(productId), Encode.forJava(institutionType), Encode.forJava(origin)))
                .thenReturn(false);

        // when
        boolean result = productService.isRequiredDocumentsEnabled(productId, institutionType, origin);

        // then
        assertFalse(result);
    }

}
