package it.pagopa.selfcare.product.service;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.product.controller.request.ProductCreateRequest;
import it.pagopa.selfcare.product.controller.response.ProductBaseResponse;
import it.pagopa.selfcare.product.controller.response.ProductResponse;
import it.pagopa.selfcare.product.mapper.ProductMapperRequest;
import it.pagopa.selfcare.product.mapper.ProductMapperResponse;
import it.pagopa.selfcare.product.model.Product;
import it.pagopa.selfcare.product.model.enums.ProductStatus;
import it.pagopa.selfcare.product.repository.ProductRepository;
import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotFoundException;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@QuarkusTest
class ProductServiceImplTest {

    @Inject
    ProductServiceImpl productService;

    @InjectMock
    ProductRepository productRepository;

    @InjectMock
    ProductMapperRequest productMapperRequest;

    @InjectMock
    ProductMapperResponse productMapperResponse;

    @Test
    void createProductTest() {
        // given
        ProductCreateRequest productCreateRequest = new ProductCreateRequest();
        productCreateRequest.setProductId("prod-test");

        Product product = Product.builder()
                .productId("prod-test")
                .status(null)
                .build();

        when(productMapperRequest.toProduct(productCreateRequest)).thenReturn(product);

        when(productRepository.findProductById("prod-test")).thenReturn(Uni.createFrom().nullItem());
        when(productRepository.persist(any(Product.class))).thenReturn(Uni.createFrom().item(product));

        when(productMapperResponse.toProductBaseResponse(any(Product.class)))
                .thenAnswer(inv -> {
                    Product productMapped = inv.getArgument(0, Product.class);
                    ProductBaseResponse productBaseResponse = new ProductBaseResponse();
                    productBaseResponse.setId(productMapped.getId());
                    productBaseResponse.setProductId(productMapped.getProductId());
                    productBaseResponse.setStatus(productMapped.getStatus());
                    return productBaseResponse;
                });

        // when
        ProductBaseResponse productBaseResponse = productService.createProduct(productCreateRequest).await().indefinitely();

        // then
        assertNotNull(productBaseResponse);
        assertEquals("prod-test", productBaseResponse.getProductId());
        assertEquals(ProductStatus.TESTING, productBaseResponse.getStatus());
        assertNotNull(productBaseResponse.getId());
        ArgumentCaptor<Product> persisted = ArgumentCaptor.forClass(Product.class);
        verify(productRepository, times(1)).persist(persisted.capture());
        Product productPersisted = persisted.getValue();
        assertEquals("prod-test", productPersisted.getProductId());
        assertEquals(ProductStatus.TESTING, productPersisted.getStatus());
        assertEquals(1, productPersisted.getVersion());
        assertNotNull(productPersisted.getCreatedAt());
        assertNotNull(productPersisted.getUpdatedAt());
        assertDoesNotThrow(() -> UUID.fromString(productPersisted.getId()));
    }

    @Test
    void createProductTest_whenExistProduct_thenIncrementVersionAndPersistsClone() {
        // given
        ProductCreateRequest productCreateRequest = new ProductCreateRequest();
        productCreateRequest.setProductId("prod-test");
        productCreateRequest.setStatus(ProductStatus.ACTIVE);

        Product product = Product.builder()
                .productId("prod-test")
                .status(ProductStatus.ACTIVE)
                .build();

        when(productMapperRequest.toProduct(productCreateRequest)).thenReturn(product);

        Product current = Product.builder()
                .id(UUID.randomUUID().toString())
                .productId("prod-test")
                .status(ProductStatus.ACTIVE)
                .version(2)
                .createdAt(Instant.now().minusSeconds(3600))
                .updatedAt(Instant.now().minusSeconds(100))
                .build();

        when(productRepository.findProductById("prod-test")).thenReturn(Uni.createFrom().item(current));

        when(productMapperRequest.cloneObject(eq(current), any(Product.class)))
                .thenAnswer(inv -> {
                    Product reqP = inv.getArgument(1, Product.class);
                    return current.toBuilder()
                            .alias(reqP.getAlias())
                            .status(reqP.getStatus())
                            .version(reqP.getVersion())
                            .updatedAt(reqP.getUpdatedAt())
                            .build();
                });

        when(productRepository.persist(any(Product.class))).thenReturn(Uni.createFrom().item(current));

        when(productMapperResponse.toProductBaseResponse(any(Product.class)))
                .thenAnswer(inv -> {
                    Product p = inv.getArgument(0, Product.class);
                    ProductBaseResponse r = new ProductBaseResponse();
                    r.setId(p.getId());
                    r.setProductId(p.getProductId());
                    r.setStatus(p.getStatus());
                    return r;
                });

        // when
        ProductBaseResponse res = productService.createProduct(productCreateRequest).await().indefinitely();

        // then
        assertNotNull(res);
        assertEquals("prod-test", res.getProductId());
        assertEquals(ProductStatus.ACTIVE, res.getStatus());
        ArgumentCaptor<Product> requestArg = ArgumentCaptor.forClass(Product.class);
        verify(productMapperRequest).cloneObject(eq(current), requestArg.capture());
        assertEquals(3, requestArg.getValue().getVersion());
        verify(productRepository, times(1)).persist(any(Product.class));
    }

    @Test
    void createProductTest_throwsBadRequest_whenMissingProduct() {
        // given
        ProductCreateRequest productCreateRequest = new ProductCreateRequest();
        productCreateRequest.setProductId(StringUtils.EMPTY);

        Product product = Product.builder()
                .id(UUID.randomUUID().toString())
                .status(null)
                .build();

        when(productMapperRequest.toProduct(productCreateRequest)).thenReturn(product);

        // when
        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> productService.createProduct(productCreateRequest).await().indefinitely());

        // then
        assertTrue(ex.getMessage().contains("Missing product"));
        verify(productRepository, never()).persist(any(Product.class));
    }

    @Test
    void getProductById_ok() {
        Product product = Product.builder()
                .id("6fb47c97-73ca-4864-9b81-566a4d90efee")
                .productId("prod-test")
                .status(ProductStatus.ACTIVE)
                .version(7)
                .build();

        when(productRepository.findProductById("prod-test")).thenReturn(Uni.createFrom().item(product));

        when(productMapperResponse.toProductResponse(product))
                .thenAnswer(inv -> {
                    ProductResponse r = new ProductResponse();
                    r.setId(product.getId());
                    r.setProductId(product.getProductId());
                    r.setAlias(product.getAlias());
                    r.setStatus(product.getStatus());
                    r.setVersion(product.getVersion());
                    return r;
                });

        ProductResponse productResponse = productService.getProductById("prod-test").await().indefinitely();

        assertNotNull(productResponse);
        assertEquals("6fb47c97-73ca-4864-9b81-566a4d90efee", productResponse.getId());
        assertEquals("prod-test", productResponse.getProductId());
        assertEquals(ProductStatus.ACTIVE, productResponse.getStatus());
        assertEquals(7, productResponse.getVersion());
    }

    @Test
    void getProductByIdTest_whenThrowsException() {
        assertThrows(IllegalArgumentException.class,
                () -> productService.getProductById(StringUtils.EMPTY).await().indefinitely());
        verify(productRepository, never()).findProductById(anyString());
    }

    @Test
    void getProductById_notFound_throws404() {
        when(productRepository.findProductById("prod-test")).thenReturn(Uni.createFrom().nullItem());
        assertThrows(NotFoundException.class,
                () -> productService.getProductById("prod-test").await().indefinitely());
    }

    @Test
    void ping_ok() {
        assertEquals("OK", productService.ping().await().indefinitely());
    }
}
