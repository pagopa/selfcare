package it.pagopa.selfcare.onboarding.core;

import it.pagopa.selfcare.onboarding.connector.api.ProductMsConnector;
import it.pagopa.selfcare.onboarding.connector.model.product.OriginResult;
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

}
