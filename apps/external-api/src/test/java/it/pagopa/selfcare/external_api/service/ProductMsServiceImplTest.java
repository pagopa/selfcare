package it.pagopa.selfcare.external_api.service;

import it.pagopa.selfcare.external_api.client.MsProductApiClient;
import it.pagopa.selfcare.external_api.exception.ResourceNotFoundException;
import it.pagopa.selfcare.product.generated.openapi.v1.dto.ProductResponse;
import it.pagopa.selfcare.product.generated.openapi.v1.dto.ProductStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductMsServiceImplTest {

    @Mock
    MsProductApiClient client;

    @InjectMocks
    ProductMsServiceImpl service;

    @Test
    void getProductsCallsProductMsWithTenantOmitted() {
        List<ProductResponse> products = List.of(new ProductResponse().productId("product"));
        when(client._getProducts(true, true, null)).thenReturn(ResponseEntity.ok(products));
        List<ProductResponse> result = service.getProducts(true, true);
        assertEquals(1, result.size());
        assertSame(products.get(0), result.get(0));
        verify(client)._getProducts(true, true, null);
    }

    @Test
    void getProductsReturnsEmptyForMissingBody() {
        when(client._getProducts(false, true, null)).thenReturn(ResponseEntity.ok(null));
        assertTrue(service.getProducts(false, true).isEmpty());
    }

    @Test
    void getProductsPreservesSdkRootAndValidityFilters() {
        ProductResponse active = new ProductResponse().productId("active").status(ProductStatus.ACTIVE);
        ProductResponse suspended = new ProductResponse().productId("suspended").status(ProductStatus.SUSPEND);
        ProductResponse child = new ProductResponse().productId("child").status(ProductStatus.ACTIVE).parentId("parent");
        when(client._getProducts(true, true, null)).thenReturn(ResponseEntity.ok(List.of(active, suspended, child)));
        assertEquals(List.of(active), service.getProducts(true, true));
    }

    @Test
    void getProductRawReturnsDtoAndValidatesId() {
        ProductResponse product = new ProductResponse().productId("product");
        when(client._getProductById("product", null)).thenReturn(ResponseEntity.ok(product));
        assertSame(product, service.getProductRaw("product"));
        assertThrows(IllegalArgumentException.class, () -> service.getProductRaw(null));
        verify(client, never())._getProductById(null, null);
    }

    @Test
    void propagatesNotFound() {
        when(client._getProductById("missing", null)).thenThrow(new ResourceNotFoundException("missing"));
        assertThrows(ResourceNotFoundException.class, () -> service.getProductRaw("missing"));
    }
}



