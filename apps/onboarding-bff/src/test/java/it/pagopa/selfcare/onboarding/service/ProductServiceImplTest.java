package it.pagopa.selfcare.onboarding.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.onboarding.client.model.OriginResult;
import it.pagopa.selfcare.product.entity.Product;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openapi.quarkus.product_json.api.ProductApi;
import org.openapi.quarkus.product_json.model.ProductCreateRequestInstitutionOriginsInner;
import org.openapi.quarkus.product_json.model.ProductOriginResponse;
import org.owasp.encoder.Encode;

import java.util.List;

@ExtendWith(MockitoExtension.class)
class ProductServiceImplTest {

    @Mock
    private ProductApi productApi;

    @Mock
    private it.pagopa.selfcare.product.service.ProductService sdkProductService;

    @InjectMocks
    private ProductServiceImpl productService;

    @Test
    void getOriginsTest_success() {
        String productId = "prod-test";
        String sanitized = Encode.forJava(productId);

        ProductOriginResponse apiResponse = new ProductOriginResponse();
        ProductCreateRequestInstitutionOriginsInner origin = new ProductCreateRequestInstitutionOriginsInner();
        origin.setLabelKey("label");
        apiResponse.setOrigins(List.of(origin));

        when(productApi.getProductOriginsById(sanitized)).thenReturn(Uni.createFrom().item(apiResponse));

        OriginResult result = productService.getOrigins(productId);

        assertNotNull(result);
        assertNotNull(result.getOrigins());
        assertEquals(1, result.getOrigins().size());
        verify(productApi).getProductOriginsById(sanitized);
    }

    @Test
    void getOriginsTest_nullResponse_throwsException() {
        String productId = "test";
        String sanitized = Encode.forJava(productId);
        when(productApi.getProductOriginsById(sanitized)).thenReturn(Uni.createFrom().nullItem());

        assertThrows(NullPointerException.class, () -> productService.getOrigins(productId));
    }

    @Test
    void getProductValid_delegatesToSdk() {
        Product expected = new Product();
        expected.setId("p1");
        when(sdkProductService.getProductIsValid("p1")).thenReturn(expected);

        Product result = productService.getProductValid("p1");

        assertSame(expected, result);
    }
}
