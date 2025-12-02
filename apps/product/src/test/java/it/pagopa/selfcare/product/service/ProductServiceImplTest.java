package it.pagopa.selfcare.product.service;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.product.model.dto.request.ProductCreateRequest;
import it.pagopa.selfcare.product.model.dto.request.ProductPatchRequest;
import it.pagopa.selfcare.product.model.dto.response.ProductBaseResponse;
import it.pagopa.selfcare.product.model.dto.response.ProductOriginResponse;
import it.pagopa.selfcare.product.model.dto.response.ProductResponse;
import it.pagopa.selfcare.product.mapper.ProductMapperRequest;
import it.pagopa.selfcare.product.mapper.ProductMapperResponse;
import it.pagopa.selfcare.product.model.OriginEntry;
import it.pagopa.selfcare.product.model.Product;
import it.pagopa.selfcare.product.model.ProductMetadata;
import it.pagopa.selfcare.product.model.enums.InstitutionType;
import it.pagopa.selfcare.product.model.enums.Origin;
import it.pagopa.selfcare.product.model.enums.ProductStatus;
import it.pagopa.selfcare.product.repository.ProductRepository;
import it.pagopa.selfcare.product.util.JsonUtils;
import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotFoundException;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.util.List;
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
        assertNotNull(productPersisted.getMetadata().getCreatedAt());
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
                .metadata(ProductMetadata.builder().createdAt(Instant.now().minusSeconds(3600)).build())
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
        // given
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

        // when
        ProductResponse productResponse = productService.getProductById("prod-test").await().indefinitely();

        // then
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
    void patchProductByIdTest_whenMissingPatchRequest_thenBadRequest() {
        // when
        Throwable thrown = catchThrowable(() ->
                productService.patchProductById("prod-test", null).await().indefinitely());

        // then
        assertThat(thrown).isInstanceOf(BadRequestException.class)
                .hasMessage("Missing request patch object into body");
    }

    @Test
    void patchProductByIdTest_whenMissingProductIdOnStorage_thenBadRequest() {
        // given
        var patchRequest = ProductPatchRequest.builder().build();

        // when
        Throwable thrown = catchThrowable(() ->
                productService.patchProductById(" ", patchRequest).await().indefinitely());

        // then
        assertThat(thrown).isInstanceOf(BadRequestException.class)
                .hasMessage("Missing productId");
        verifyNoInteractions(jsonUtils, productRepository);
    }

    @Test
    void patchProductByIdTest_whenRepositoryReturnsNull_thenNotFound() {
        // given
        var patchRequest = ProductPatchRequest.builder().build();

        when(productRepository.findProductById("prod-test"))
                .thenReturn(Uni.createFrom().nullItem());

        // when
        Throwable thrown = catchThrowable(() ->
                productService.patchProductById("prod-test", patchRequest).await().indefinitely());

        // then
        assertThat(thrown).isInstanceOf(NotFoundException.class)
                .hasMessage("Product prod-test not found");
        verify(productRepository, times(1)).findProductById("prod-test");
        verifyNoMoreInteractions(productRepository);
    }

    @Test
    void patchProductByIdTest_whenPersistingBody_thenReturnResponse() {
        // given
        var patchRequest = ProductPatchRequest.builder().description("update description").build();

        Product current = new Product();
        current.setProductId("prod-test");
        current.setVersion(2);

        when(productRepository.findProductById("prod-test"))
                .thenReturn(Uni.createFrom().item(current));
        current.setDescription("update description");

        when(productMapperRequest.toPatch(patchRequest, current)).thenReturn(current);

        when(productRepository.persist(any(Product.class))).thenReturn(Uni.createFrom().item(current));

        when(productMapperResponse.toProductResponse(current))
                .thenAnswer(inv -> {
                    ProductResponse r = new ProductResponse();
                    r.setId(current.getId());
                    r.setProductId(current.getProductId());
                    r.setDescription(current.getDescription());
                    r.setAlias(current.getAlias());
                    r.setStatus(current.getStatus());
                    r.setVersion(current.getVersion());
                    return r;
                });

        // when
        ProductResponse out = productService.patchProductById("prod-test", patchRequest).await().indefinitely();

        // then
        verify(productRepository).findProductById("prod-test");
        assertEquals(out.getDescription(), "update description");
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

    @Test
    void getProductOriginsById_ok() {
        // given
        Product product = Product.builder()
                .id("6fb47c97-73ca-4864-9b81-566a4d90efee")
                .productId("prod-test")
                .status(ProductStatus.ACTIVE)
                .institutionOrigins(List.of(OriginEntry.builder().institutionType(InstitutionType.PA).labelKey("pa").origin(Origin.IPA).build()))
                .version(7)
                .build();

        when(productRepository.findProductById("prod-test"))
                .thenReturn(Uni.createFrom().item(product));

        when(productMapperResponse.toProductOriginResponse(product))
                .thenAnswer(inv -> ProductOriginResponse.builder().origins(product.getInstitutionOrigins()).build());

        // when
        ProductOriginResponse productOriginResponse =
                productService.getProductOriginsById("prod-test").await().indefinitely();

        // then
        assertNotNull(productOriginResponse);
        List<OriginEntry> origins = productOriginResponse.getOrigins();
        assertEquals(1, origins.size());
        assertEquals("PA", origins.get(0).getInstitutionType().name());
        assertEquals("pa", origins.get(0).getLabelKey());
        assertEquals("IPA", origins.get(0).getOrigin().name());
    }

    @Test
    void getProductOriginsById_whenThrowsException() {
        // when
        assertThrows(IllegalArgumentException.class,
                () -> productService.getProductOriginsById(StringUtils.EMPTY).await().indefinitely());

        // then
        verify(productRepository, never()).findProductById(anyString());
    }

    @Test
    void getProductOriginsById_notFound_throws404() {
        // given
        when(productRepository.findProductById("prod-test"))
                .thenReturn(Uni.createFrom().nullItem());

        // when
        assertThrows(NotFoundException.class,
                () -> productService.getProductOriginsById("prod-test").await().indefinitely());
    }


}
