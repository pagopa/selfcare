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
import it.pagopa.selfcare.product.util.JsonUtils;
import jakarta.inject.Inject;
import jakarta.json.Json;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotFoundException;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
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

    @InjectMock
    JsonUtils jsonUtils;

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
                .build();

        when(productRepository.findProductById("prod-test")).thenReturn(Uni.createFrom().item(current));

        when(productMapperRequest.cloneObject(eq(current), any(Product.class)))
                .thenAnswer(inv -> {
                    Product reqP = inv.getArgument(1, Product.class);
                    return current.toBuilder()
                            .alias(reqP.getAlias())
                            .status(reqP.getStatus())
                            .version(reqP.getVersion())
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

    @Test
    void patchProductByIdTest_whenMissingProductIdOnStorage_thenBadRequest() {
        // given
        var validBodyObject = Json.createObjectBuilder().add("productId", "prod-test").build();

        // when
        Throwable thrown = catchThrowable(() ->
                productService.patchProductById(" ", validBodyObject).await().indefinitely());

        // then
        assertThat(thrown).isInstanceOf(BadRequestException.class)
                .hasMessage("Missing productId");
        verifyNoInteractions(jsonUtils, productRepository);
    }

    @Test
    void patchProductByIdTest_whenParsingFails_thenBadRequest() {
        // given
        var validBodyObject = Json.createValue("broken");

        Product current = new Product();
        current.setProductId("prod-test");
        current.setVersion(2);

        when(productRepository.findProductById("prod-test"))
                .thenReturn(Uni.createFrom().item(current));

        when(jsonUtils.toMergePatch(validBodyObject)).thenThrow(new BadRequestException("Invalid merge patch document"));

        // when
        Throwable thrown = catchThrowable(() ->
                productService.patchProductById("prod-test", validBodyObject).await().indefinitely());

        // then
        assertThat(thrown).isInstanceOf(BadRequestException.class)
                .hasMessage("Invalid merge patch document");
        verifyNoInteractions(productRepository);
    }

    @Test
    void patchProductByIdTest_whenRepositoryReturnsNull_thenNotFound() {
        // given
        var validBodyObject = Json.createObjectBuilder().add("productId", "prod-test").build();
        var mergePatch = Json.createMergePatch(validBodyObject);

        when(jsonUtils.toMergePatch(validBodyObject)).thenReturn(mergePatch);
        when(productRepository.findProductById("prod-test"))
                .thenReturn(Uni.createFrom().nullItem());

        // when
        Throwable thrown = catchThrowable(() ->
                productService.patchProductById("prod-test", validBodyObject).await().indefinitely());

        // then
        assertThat(thrown).isInstanceOf(NotFoundException.class)
                .hasMessage("Product prod-test not found");
        verify(productRepository, times(1)).findProductById("prod-test");
        verifyNoMoreInteractions(productRepository);
    }

    @Test
    void patchProductByIdTest_whenPersistingBody_thenReturnResponse() {
        // given
        var validBodyObject = Json.createObjectBuilder().add("productId", "prod-test").build();
        var mergePatch = Json.createMergePatch(validBodyObject);

        Product current = new Product();
        current.setProductId("prod-test");
        current.setVersion(2);

        when(jsonUtils.toMergePatch(validBodyObject)).thenReturn(mergePatch);
        when(productRepository.findProductById("prod-test"))
                .thenReturn(Uni.createFrom().item(current));

        Product patched = new Product();
        when(jsonUtils.mergePatch(eq(mergePatch), eq(current), eq(Product.class)))
                .thenReturn(patched);

        when(productRepository.persist(any(Product.class))).thenReturn(Uni.createFrom().item(current));

        ProductResponse mapped = new ProductResponse();
        when(productMapperResponse.toProductResponse(any(Product.class))).thenReturn(mapped);

        // when
        ProductResponse out = productService.patchProductById("prod-test", validBodyObject).await().indefinitely();

        // then
        assertThat(out).isSameAs(mapped);
        verify(productRepository).findProductById("prod-test");
        verify(productRepository).persist(any(Product.class));
        verify(productMapperResponse).toProductResponse(any(Product.class));
        verifyNoMoreInteractions(productRepository, productMapperResponse);
    }

    @Test
    void deleteProductByIdTest_whenDeleting_thenBadRequest() {
        // given
        var blank = StringUtils.EMPTY;

        // when
        Throwable thrown = catchThrowable(() -> productService.deleteProductById(blank).await().indefinitely());

        // then
        assertThat(thrown)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Missing product by productId: ");
        verifyNoInteractions(productRepository, productMapperResponse);
    }

    @Test
    void deleteProductByIdTest_whenDeleting_thenNotFound() {
        // given
        String productId = "prod-test";
        when(productRepository.findProductById(productId)).thenReturn(Uni.createFrom().nullItem());

        // when
        Throwable thrown = catchThrowable(() -> productService.deleteProductById(productId).await().indefinitely());

        // then
        assertThat(thrown)
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Product prod-test not found");
        verify(productRepository, times(1)).findProductById(productId);
        verifyNoMoreInteractions(productRepository);
        verifyNoInteractions(productMapperResponse);
    }

    @Test
    void deleteProductByIdTest_whenDeleting_thenSetStatusDeleted() {
        // given
        String productId = "prod-test";
        Product current = new Product();
        current.setStatus(ProductStatus.ACTIVE);

        when(productRepository.findProductById(productId))
                .thenReturn(Uni.createFrom().item(current));

        ArgumentCaptor<Product> updatedCaptor = ArgumentCaptor.forClass(Product.class);
        when(productRepository.update(updatedCaptor.capture())).thenReturn(Uni.createFrom().item(current));

        ProductBaseResponse mapped = new ProductBaseResponse();
        when(productMapperResponse.toProductBaseResponse(any(Product.class))).thenReturn(mapped);

        // when
        ProductBaseResponse out = productService.deleteProductById(productId).await().indefinitely();

        // then
        assertThat(out).isSameAs(mapped);
        Product updated = updatedCaptor.getValue();
        assertThat(updated.getStatus()).isEqualTo(ProductStatus.DELETED);
        verify(productRepository, times(1)).findProductById(productId);
        verify(productRepository, times(1)).update(any(Product.class));
        verify(productMapperResponse, times(1)).toProductBaseResponse(any(Product.class));
        verifyNoMoreInteractions(productRepository, productMapperResponse);
    }

    @Test
    void deleteProductByIdTest_whenDeleting_thenPropagatesError() {
        // given
        String productId = "prod-test";
        Product current = new Product();
        current.setStatus(ProductStatus.ACTIVE);

        when(productRepository.findProductById(productId))
                .thenReturn(Uni.createFrom().item(current));
        when(productRepository.update(any(Product.class)))
                .thenReturn(Uni.createFrom().failure(new RuntimeException()));

        // when
        Throwable thrown = catchThrowable(() -> productService.deleteProductById(productId).await().indefinitely());

        // then
        assertThat(thrown)
                .isInstanceOf(RuntimeException.class);
        verify(productRepository, times(1)).findProductById(productId);
        verify(productRepository, times(1)).update(any(Product.class));
        verifyNoInteractions(productMapperResponse);
    }

}
