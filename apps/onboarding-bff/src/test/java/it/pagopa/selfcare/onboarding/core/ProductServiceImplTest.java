package it.pagopa.selfcare.onboarding.core;

import it.pagopa.selfcare.onboarding.connector.model.product.OriginResult;
import it.pagopa.selfcare.onboarding.connector.rest.mapper.ProductMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openapi.quarkus.product_json.api.ProductApi;
import org.openapi.quarkus.product_json.model.ProductOriginResponse;
import org.owasp.encoder.Encode;
import io.smallrye.mutiny.Uni;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceImplTest {

    @Mock
    private ProductApi productApi;

    @Mock
    private ProductMapper productMapper;

    @InjectMocks
    private ProductServiceImpl productService;

    @Test
    void getOriginsTest_success() {
        // given
        String productId = "prod-test";
        String sanitized = Encode.forJava(productId);

        ProductOriginResponse apiResponse = new ProductOriginResponse();
        OriginResult originResult = new OriginResult();
        originResult.setOrigins(List.of());

        when(productApi.getProductOriginsById(sanitized)).thenReturn(Uni.createFrom().item(apiResponse));
        when(productMapper.toOriginResult(apiResponse)).thenReturn(originResult);

        // when
        OriginResult result = productService.getOrigins(productId);

        // then
        assertNotNull(result);
        assertEquals(originResult, result);

        verify(productApi, times(1)).getProductOriginsById(sanitized);
        verify(productMapper, times(1)).toOriginResult(apiResponse);
        verifyNoMoreInteractions(productApi, productMapper);
    }

    @Test
    void getOriginsTest_handlesSpecialCharacters() {
        // given
        String rawProductId = "<error>";
        String sanitized = Encode.forJava(rawProductId);

        ProductOriginResponse apiResponse = new ProductOriginResponse();
        OriginResult originResult = new OriginResult();
        originResult.setOrigins(List.of());

        when(productApi.getProductOriginsById(sanitized)).thenReturn(Uni.createFrom().item(apiResponse));
        when(productMapper.toOriginResult(apiResponse)).thenReturn(originResult);

        // when
        OriginResult result = productService.getOrigins(rawProductId);

        // then
        assertNotNull(result);
        verify(productApi).getProductOriginsById(sanitized);
        verify(productMapper).toOriginResult(apiResponse);
    }

    @Test
    void getOriginsTest_nullOriginsList_throwsException() {
        // given
        String productId = "test";
        String sanitized = Encode.forJava(productId);

        ProductOriginResponse apiResponse = new ProductOriginResponse();
        OriginResult originResult = new OriginResult();
        when(productApi.getProductOriginsById(sanitized)).thenReturn(Uni.createFrom().item(apiResponse));
        when(productMapper.toOriginResult(apiResponse)).thenReturn(originResult);

        // then
        assertThrows(NullPointerException.class, () -> productService.getOrigins(productId));
    }

}
