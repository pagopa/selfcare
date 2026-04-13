package it.pagopa.selfcare.onboarding.service;

import it.pagopa.selfcare.onboarding.client.model.product.OriginResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openapi.quarkus.product_json.api.ProductApi;
import org.openapi.quarkus.product_json.model.ProductCreateRequestInstitutionOriginsInner;
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

    @InjectMocks
    private ProductServiceImpl productService;

    @Test
    void getOriginsTest_success() {
        // given
        String productId = "prod-test";
        String sanitized = Encode.forJava(productId);

        ProductOriginResponse apiResponse = new ProductOriginResponse();
        apiResponse.setOrigins(List.of(new ProductCreateRequestInstitutionOriginsInner()));
        OriginResult originResult = new OriginResult();
        originResult.setOrigins(List.of(new it.pagopa.selfcare.onboarding.client.model.product.OriginEntry()));

        when(productApi.getProductOriginsById(sanitized)).thenReturn(Uni.createFrom().item(apiResponse));

        // when
        OriginResult result = productService.getOrigins(productId);

        // then
        assertNotNull(result);
        assertNotNull(result.getOrigins());
        assertEquals(1, result.getOrigins().size());

        verify(productApi, times(1)).getProductOriginsById(sanitized);
        verifyNoMoreInteractions(productApi);
    }

    @Test
    void getOriginsTest_handlesSpecialCharacters() {
        // given
        String rawProductId = "<error>";
        String sanitized = Encode.forJava(rawProductId);

        ProductOriginResponse apiResponse = new ProductOriginResponse();
        apiResponse.setOrigins(List.of(new ProductCreateRequestInstitutionOriginsInner()));
        OriginResult originResult = new OriginResult();
        originResult.setOrigins(List.of(new it.pagopa.selfcare.onboarding.client.model.product.OriginEntry()));

        when(productApi.getProductOriginsById(sanitized)).thenReturn(Uni.createFrom().item(apiResponse));

        // when
        OriginResult result = productService.getOrigins(rawProductId);

        // then
        assertNotNull(result);
        verify(productApi).getProductOriginsById(sanitized);
    }

    @Test
    void getOriginsTest_nullOriginsList_throwsException() {
        // given
        String productId = "test";
        String sanitized = Encode.forJava(productId);

        ProductOriginResponse apiResponse = null;
        when(productApi.getProductOriginsById(sanitized)).thenReturn(Uni.createFrom().item(apiResponse));

        // then
        assertThrows(NullPointerException.class, () -> productService.getOrigins(productId));
    }

}
