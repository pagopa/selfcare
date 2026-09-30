package it.pagopa.selfcare.product.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.product.mapper.ProductMapperRequest;
import it.pagopa.selfcare.product.mapper.ProductMapperResponse;
import it.pagopa.selfcare.product.model.BackOfficeRole;
import it.pagopa.selfcare.product.model.Features;
import it.pagopa.selfcare.product.model.OriginEntry;
import it.pagopa.selfcare.product.model.Product;
import it.pagopa.selfcare.product.model.ProductMetadata;
import it.pagopa.selfcare.product.model.RequiredDocument;
import it.pagopa.selfcare.product.model.RequiredDocumentFilter;
import it.pagopa.selfcare.product.model.RoleMapping;
import it.pagopa.selfcare.product.model.WorkflowRule;
import it.pagopa.selfcare.product.model.dto.request.ProductCreateRequest;
import it.pagopa.selfcare.product.model.dto.request.ProductPatchRequest;
import it.pagopa.selfcare.product.model.dto.response.ProductBaseResponse;
import it.pagopa.selfcare.product.model.dto.response.ProductExpirationResponse;
import it.pagopa.selfcare.product.model.dto.response.ProductOriginResponse;
import it.pagopa.selfcare.product.model.dto.response.ProductResponse;
import it.pagopa.selfcare.product.model.dto.response.ProductRoleResponse;
import it.pagopa.selfcare.product.model.dto.response.RequiredDocumentResponse;
import it.pagopa.selfcare.product.model.dto.response.WorkflowTypeResponse;
import it.pagopa.selfcare.product.model.enums.InstitutionType;
import it.pagopa.selfcare.product.model.enums.Origin;
import it.pagopa.selfcare.product.model.enums.ProductStatus;
import it.pagopa.selfcare.product.model.enums.UserRole;
import it.pagopa.selfcare.product.model.enums.WorkflowType;
import it.pagopa.selfcare.product.repository.ProductRepository;
import it.pagopa.selfcare.product.util.JsonUtils;
import it.pagopa.selfcare.tenant.TenantContext;
import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotFoundException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

@QuarkusTest
class ProductServiceImplTest {

  @Inject ProductServiceImpl productService;

  @InjectMock ProductRepository productRepository;

  @InjectMock ProductMapperRequest productMapperRequest;

  @InjectMock ProductMapperResponse productMapperResponse;

  @InjectMock JsonUtils jsonUtils;

  @Inject TenantContext tenantContext;

  @BeforeEach
  void setUpTenant() {
    tenantContext.setTenantId("AR");
  }

  @Test
  void createProductTest() {
    // given
    ProductCreateRequest productCreateRequest = new ProductCreateRequest();
    productCreateRequest.setTenantId("AR");
    productCreateRequest.setProductId("prod-test");

    Product product = Product.builder().productId("prod-test").status(null).build();

    when(productMapperRequest.toProduct(productCreateRequest)).thenReturn(product);

    when(productRepository.findProductById("prod-test")).thenReturn(Uni.createFrom().nullItem());
    when(productRepository.persist(any(Product.class))).thenReturn(Uni.createFrom().item(product));

    when(productMapperResponse.toProductBaseResponse(any(Product.class)))
        .thenAnswer(
            inv -> {
              Product productMapped = inv.getArgument(0, Product.class);
              ProductBaseResponse productBaseResponse = new ProductBaseResponse();
              productBaseResponse.setId(productMapped.getId());
              productBaseResponse.setProductId(productMapped.getProductId());
              productBaseResponse.setStatus(productMapped.getStatus());
              return productBaseResponse;
            });

    // when
    ProductBaseResponse productBaseResponse =
        productService.createProduct(productCreateRequest, "createdBy").await().indefinitely();

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
    productCreateRequest.setTenantId("AR");
    productCreateRequest.setProductId("prod-test");
    productCreateRequest.setStatus(ProductStatus.ACTIVE);

    Product product = Product.builder().productId("prod-test").status(ProductStatus.ACTIVE).build();

    when(productMapperRequest.toProduct(productCreateRequest)).thenReturn(product);

    Product current =
        Product.builder()
            .id(UUID.randomUUID().toString())
            .productId("prod-test")
            .status(ProductStatus.ACTIVE)
            .version(2)
            .metadata(ProductMetadata.builder().createdAt(Instant.now().minusSeconds(3600)).build())
            .build();

    when(productRepository.findProductById("prod-test")).thenReturn(Uni.createFrom().item(current));

    when(productMapperRequest.cloneObject(eq(current), any(Product.class)))
        .thenAnswer(
            inv -> {
              Product reqP = inv.getArgument(1, Product.class);
              return current.toBuilder()
                  .alias(reqP.getAlias())
                  .status(reqP.getStatus())
                  .version(reqP.getVersion())
                  .build();
            });

    when(productRepository.persist(any(Product.class))).thenReturn(Uni.createFrom().item(current));

    when(productMapperResponse.toProductBaseResponse(any(Product.class)))
        .thenAnswer(
            inv -> {
              Product p = inv.getArgument(0, Product.class);
              ProductBaseResponse r = new ProductBaseResponse();
              r.setId(p.getId());
              r.setProductId(p.getProductId());
              r.setStatus(p.getStatus());
              return r;
            });

    // when
    ProductBaseResponse res =
        productService.createProduct(productCreateRequest, "createdBy").await().indefinitely();

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
  void createProductTest_throwsBadRequest_whenMissingTenant() {
    // given
    ProductCreateRequest productCreateRequest = new ProductCreateRequest();

    Product product = Product.builder().id(UUID.randomUUID().toString()).status(null).build();

    when(productMapperRequest.toProduct(productCreateRequest)).thenReturn(product);

    // when
    BadRequestException ex =
        assertThrows(
            BadRequestException.class,
            () ->
                productService
                    .createProduct(productCreateRequest, "createdBy")
                    .await()
                    .indefinitely());

    // then
    assertTrue(ex.getMessage().contains("Missing tenantId"));
    verify(productRepository, never()).persist(any(Product.class));
  }

  @Test
  void createProductTest_throwsBadRequest_whenMissingProduct() {
    // given
    ProductCreateRequest productCreateRequest = new ProductCreateRequest();
    productCreateRequest.setTenantId("AR");
    Product product = Product.builder().id(UUID.randomUUID().toString()).status(null).build();

    when(productMapperRequest.toProduct(productCreateRequest)).thenReturn(product);

    // when
    BadRequestException ex =
        assertThrows(
            BadRequestException.class,
            () ->
                productService
                    .createProduct(productCreateRequest, "createdBy")
                    .await()
                    .indefinitely());

    // then
    assertTrue(ex.getMessage().contains("Invalid productId"));
    verify(productRepository, never()).persist(any(Product.class));
  }

  @Test
  void getProductById_ok() {
    // given
    Product product =
        Product.builder()
            .id("6fb47c97-73ca-4864-9b81-566a4d90efee")
            .productId("prod-test")
            .status(ProductStatus.ACTIVE)
            .version(7)
            .build();

    when(productRepository.findProductById("prod-test")).thenReturn(Uni.createFrom().item(product));

    when(productMapperResponse.toProductResponse(product))
        .thenAnswer(
            inv -> {
              ProductResponse r = new ProductResponse();
              r.setId(product.getId());
              r.setProductId(product.getProductId());
              r.setAlias(product.getAlias());
              r.setStatus(product.getStatus());
              r.setVersion(product.getVersion());
              return r;
            });

    // when
    ProductResponse productResponse =
        productService.getProduct("AR", "prod-test").await().indefinitely();

    // then
    assertNotNull(productResponse);
    assertEquals("6fb47c97-73ca-4864-9b81-566a4d90efee", productResponse.getId());
    assertEquals("prod-test", productResponse.getProductId());
    assertEquals(ProductStatus.ACTIVE, productResponse.getStatus());
    assertEquals(7, productResponse.getVersion());
  }

  @Test
  void getProductByIdTest_whenThrowsException() {
    assertThrows(
        IllegalArgumentException.class,
        () -> productService.getProduct("AR", StringUtils.EMPTY).await().indefinitely());
    verify(productRepository, never()).findProductById(anyString());
  }

  @Test
  void getProductById_notFound_throws404() {
    when(productRepository.findProductById("prod-test")).thenReturn(Uni.createFrom().nullItem());
    assertThrows(
        NotFoundException.class,
        () -> productService.getProduct("AR", "prod-test").await().indefinitely());
  }

  @Test
  void ping_ok() {
    assertEquals("OK", productService.ping().await().indefinitely());
  }

  @Test
  void patchProductByIdTest_whenMissingPatchRequest_thenBadRequest() {
    // when
    Throwable thrown =
        catchThrowable(
            () ->
                productService
                    .patchProductById("AR", "prod-test", "createdBy", null)
                    .await()
                    .indefinitely());

    // then
    assertThat(thrown)
        .isInstanceOf(BadRequestException.class)
        .hasMessage("Missing request patch object into body");
  }

  @Test
  void patchProductByIdTest_whenMissingProductIdOnStorage_thenBadRequest() {
    // given
    var patchRequest = ProductPatchRequest.builder().build();

    // when
    Throwable thrown =
        catchThrowable(
            () ->
                productService
                    .patchProductById("AR", " ", "createdBy", patchRequest)
                    .await()
                    .indefinitely());

    // then
    assertThat(thrown).isInstanceOf(BadRequestException.class).hasMessage("Missing productId");
    verifyNoInteractions(jsonUtils, productRepository);
  }

  @Test
  void patchProductByIdTest_whenRepositoryReturnsNull_thenNotFound() {
    // given
    var patchRequest = ProductPatchRequest.builder().build();

    when(productRepository.findProductById("prod-test")).thenReturn(Uni.createFrom().nullItem());

    // when
    Throwable thrown =
        catchThrowable(
            () ->
                productService
                    .patchProductById("AR", "prod-test", "createdBy", patchRequest)
                    .await()
                    .indefinitely());

    // then
    assertThat(thrown)
        .isInstanceOf(NotFoundException.class)
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

    when(productRepository.findProductById("prod-test")).thenReturn(Uni.createFrom().item(current));
    current.setDescription("update description");

    when(productMapperRequest.toPatch(patchRequest, current)).thenReturn(current);

    when(productRepository.persist(any(Product.class))).thenReturn(Uni.createFrom().item(current));

    when(productMapperResponse.toProductResponse(current))
        .thenAnswer(
            inv -> {
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
    ProductResponse out =
        productService
            .patchProductById("AR", "prod-test", "createdBy", patchRequest)
            .await()
            .indefinitely();

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
    Throwable thrown =
        catchThrowable(() -> productService.deleteProduct("AR", blank).await().indefinitely());

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
    Throwable thrown =
        catchThrowable(() -> productService.deleteProduct("AR", productId).await().indefinitely());

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

    when(productRepository.findProductById(productId)).thenReturn(Uni.createFrom().item(current));

    ArgumentCaptor<Product> updatedCaptor = ArgumentCaptor.forClass(Product.class);
    when(productRepository.update(updatedCaptor.capture()))
        .thenReturn(Uni.createFrom().item(current));

    ProductBaseResponse mapped = new ProductBaseResponse();
    when(productMapperResponse.toProductBaseResponse(any(Product.class))).thenReturn(mapped);

    // when
    ProductBaseResponse out = productService.deleteProduct("AR", productId).await().indefinitely();

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

    when(productRepository.findProductById(productId)).thenReturn(Uni.createFrom().item(current));
    when(productRepository.update(any(Product.class)))
        .thenReturn(Uni.createFrom().failure(new RuntimeException()));

    // when
    Throwable thrown =
        catchThrowable(() -> productService.deleteProduct("AR", productId).await().indefinitely());

    // then
    assertThat(thrown).isInstanceOf(RuntimeException.class);
    verify(productRepository, times(1)).findProductById(productId);
    verify(productRepository, times(1)).update(any(Product.class));
    verifyNoInteractions(productMapperResponse);
  }

  @Test
  void getProductOriginsById_ok() {
    // given
    Product product =
        Product.builder()
            .id("6fb47c97-73ca-4864-9b81-566a4d90efee")
            .productId("prod-test")
            .status(ProductStatus.ACTIVE)
            .institutionOrigins(
                List.of(
                    OriginEntry.builder()
                        .institutionType(InstitutionType.PA)
                        .labelKey("pa")
                        .origin(Origin.IPA)
                        .build()))
            .version(7)
            .build();

    when(productRepository.findProductById("prod-test")).thenReturn(Uni.createFrom().item(product));

    when(productMapperResponse.toProductOriginResponse(product))
        .thenAnswer(
            inv ->
                ProductOriginResponse.builder().origins(product.getInstitutionOrigins()).build());

    // when
    ProductOriginResponse productOriginResponse =
        productService.getProductOrigins("AR", "prod-test").await().indefinitely();

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
    assertThrows(
        IllegalArgumentException.class,
        () -> productService.getProductOrigins("AR", StringUtils.EMPTY).await().indefinitely());

    // then
    verify(productRepository, never()).findProductById(anyString());
  }

  @Test
  void getProductOriginsById_notFound_throws404() {
    // given
    when(productRepository.findProductById("prod-test")).thenReturn(Uni.createFrom().nullItem());

    // when
    assertThrows(
        NotFoundException.class,
        () -> productService.getProductOrigins("AR", "prod-test").await().indefinitely());
  }

  // -------------------------------------------------------------------------
  // getWorkflowType
  // -------------------------------------------------------------------------

  @Test
  void getWorkflowType_ok_exactOriginMatch() {
    // given
    Product product =
        Product.builder()
            .productId("prod-test")
            .workflowRules(
                List.of(
                    WorkflowRule.builder()
                        .institutionType(InstitutionType.PA)
                        .origin(Origin.IPA)
                        .workflowType(WorkflowType.CONTRACT_REGISTRATION)
                        .build(),
                    WorkflowRule.builder()
                        .institutionType(InstitutionType.GSP)
                        .origin(Origin.SELC)
                        .workflowType(WorkflowType.FOR_APPROVE)
                        .build()))
            .build();

    when(productRepository.findProductById("prod-test")).thenReturn(Uni.createFrom().item(product));

    // when
    WorkflowTypeResponse response =
        productService
            .getWorkflowType("AR", "prod-test", InstitutionType.PA, Origin.IPA)
            .await()
            .indefinitely();

    // then
    assertNotNull(response);
    assertEquals(WorkflowType.CONTRACT_REGISTRATION, response.getWorkflowType());
    verify(productRepository, times(1)).findProductById("prod-test");
  }

  @Test
  void getWorkflowType_ok_secondRuleMatchedWhenFirstDoesNotMatch() {
    // given
    Product product =
        Product.builder()
            .productId("prod-test")
            .workflowRules(
                List.of(
                    WorkflowRule.builder()
                        .institutionType(InstitutionType.PA)
                        .origin(Origin.IPA)
                        .workflowType(WorkflowType.CONTRACT_REGISTRATION)
                        .build(),
                    WorkflowRule.builder()
                        .institutionType(InstitutionType.GSP)
                        .origin(Origin.SELC)
                        .workflowType(WorkflowType.FOR_APPROVE)
                        .build()))
            .build();

    when(productRepository.findProductById("prod-test")).thenReturn(Uni.createFrom().item(product));

    // when
    WorkflowTypeResponse response =
        productService
            .getWorkflowType("AR", "prod-test", InstitutionType.GSP, Origin.SELC)
            .await()
            .indefinitely();

    // then
    assertNotNull(response);
    assertEquals(WorkflowType.FOR_APPROVE, response.getWorkflowType());
  }

  @Test
  void getWorkflowType_throwsIllegalArgument_whenProductIdIsBlank() {
    // when
    Throwable thrown =
        catchThrowable(
            () ->
                productService
                    .getWorkflowType("AR", "  ", InstitutionType.PA, Origin.IPA)
                    .await()
                    .indefinitely());

    // then
    assertThat(thrown).isInstanceOf(IllegalArgumentException.class).hasMessage("Missing productId");
    verify(productRepository, never()).findProductById(anyString());
  }

  @Test
  void getWorkflowType_throwsIllegalArgument_whenInstitutionTypeIsNull() {
    // when
    Throwable thrown =
        catchThrowable(
            () ->
                productService
                    .getWorkflowType("AR", "prod-test", null, Origin.IPA)
                    .await()
                    .indefinitely());

    // then
    assertThat(thrown)
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Missing institutionType");
    verify(productRepository, never()).findProductById(anyString());
  }

  @Test
  void getWorkflowType_throwsIllegalArgument_whenOriginIsNull() {
    // when
    Throwable thrown =
        catchThrowable(
            () ->
                productService
                    .getWorkflowType("AR", "prod-test", InstitutionType.PA, null)
                    .await()
                    .indefinitely());

    // then
    assertThat(thrown).isInstanceOf(IllegalArgumentException.class).hasMessage("Missing origin");
    verify(productRepository, never()).findProductById(anyString());
  }

  @Test
  void getWorkflowType_throwsNotFound_whenProductDoesNotExist() {
    // given
    when(productRepository.findProductById("prod-missing")).thenReturn(Uni.createFrom().nullItem());

    // when
    Throwable thrown =
        catchThrowable(
            () ->
                productService
                    .getWorkflowType("AR", "prod-missing", InstitutionType.PA, Origin.IPA)
                    .await()
                    .indefinitely());

    // then
    assertThat(thrown).isInstanceOf(NotFoundException.class).hasMessageContaining("prod-missing");
  }

  @Test
  void getWorkflowType_throwsNotFound_whenWorkflowRulesIsEmpty() {
    // given
    Product product = Product.builder().productId("prod-test").workflowRules(List.of()).build();

    when(productRepository.findProductById("prod-test")).thenReturn(Uni.createFrom().item(product));

    // when
    Throwable thrown =
        catchThrowable(
            () ->
                productService
                    .getWorkflowType("AR", "prod-test", InstitutionType.PA, Origin.IPA)
                    .await()
                    .indefinitely());

    // then
    assertThat(thrown)
        .isInstanceOf(NotFoundException.class)
        .hasMessageContaining("No workflowRules configured");
  }

  @Test
  void getWorkflowType_throwsNotFound_whenNoRuleMatchesInstitutionTypeAndOrigin() {
    // given - rule per PA/IPA, si cerca PA/SELC â†’ nessun match
    Product product =
        Product.builder()
            .productId("prod-test")
            .workflowRules(
                List.of(
                    WorkflowRule.builder()
                        .institutionType(InstitutionType.PA)
                        .origin(Origin.IPA)
                        .workflowType(WorkflowType.CONTRACT_REGISTRATION)
                        .build()))
            .build();

    when(productRepository.findProductById("prod-test")).thenReturn(Uni.createFrom().item(product));

    // when
    Throwable thrown =
        catchThrowable(
            () ->
                productService
                    .getWorkflowType("AR", "prod-test", InstitutionType.PA, Origin.SELC)
                    .await()
                    .indefinitely());

    // then
    assertThat(thrown)
        .isInstanceOf(NotFoundException.class)
        .hasMessageContaining("PA")
        .hasMessageContaining("SELC");
  }

  @Test
  void createProductTest_whenChildProduct_thenSetsRequiresParentOnboardingAndValidatesParent() {
    // given
    ProductCreateRequest request = new ProductCreateRequest();
    request.setTenantId("AR");
    request.setProductId("prod-io-premium");
    request.setParentId("prod-io");

    Product product = Product.builder().productId("prod-io-premium").parentId("prod-io").build();

    Product parent = Product.builder().productId("prod-io").build();

    when(productMapperRequest.toProduct(request)).thenReturn(product);
    when(productRepository.findProductById("prod-io")).thenReturn(Uni.createFrom().item(parent));
    when(productRepository.findProductById("prod-io-premium"))
        .thenReturn(Uni.createFrom().nullItem());
    when(productRepository.persist(any(Product.class))).thenReturn(Uni.createFrom().item(product));
    when(productMapperResponse.toProductBaseResponse(any(Product.class)))
        .thenAnswer(
            inv -> {
              Product mapped = inv.getArgument(0, Product.class);
              ProductBaseResponse response = new ProductBaseResponse();
              response.setProductId(mapped.getProductId());
              return response;
            });

    // when
    productService.createProduct(request, "createdBy").await().indefinitely();

    // then
    ArgumentCaptor<Product> persisted = ArgumentCaptor.forClass(Product.class);
    verify(productRepository).persist(persisted.capture());
    assertThat(persisted.getValue().getParentId()).isEqualTo("prod-io");
    assertThat(persisted.getValue().getFeatures().isRequiresParentOnboarding()).isTrue();
  }

  @Test
  void createProductTest_whenParentNotFound_thenBadRequest() {
    // given
    ProductCreateRequest request = new ProductCreateRequest();
    request.setTenantId("AR");
    request.setProductId("prod-io-premium");
    request.setParentId("prod-io");

    Product product = Product.builder().productId("prod-io-premium").parentId("prod-io").build();

    when(productMapperRequest.toProduct(request)).thenReturn(product);
    when(productRepository.findProductById("prod-io")).thenReturn(Uni.createFrom().nullItem());

    // when
    Throwable thrown =
        catchThrowable(
            () -> productService.createProduct(request, "createdBy").await().indefinitely());

    // then
    assertThat(thrown)
        .isInstanceOf(BadRequestException.class)
        .hasMessage("Parent product prod-io not found");
    verify(productRepository, never()).persist(any(Product.class));
  }

  @Test
  void createProductTest_whenParentIdEqualsProductId_thenBadRequest() {
    // given
    ProductCreateRequest request = new ProductCreateRequest();
    request.setTenantId("AR");
    request.setProductId("prod-io");
    request.setParentId("prod-io");

    Product product = Product.builder().productId("prod-io").parentId("prod-io").build();

    when(productMapperRequest.toProduct(request)).thenReturn(product);

    // when
    Throwable thrown =
        catchThrowable(
            () -> productService.createProduct(request, "createdBy").await().indefinitely());

    // then
    assertThat(thrown)
        .isInstanceOf(BadRequestException.class)
        .hasMessage("parentId cannot be equal to productId");
    verify(productRepository, never()).findProductById(anyString());
  }

  @Test
  void patchProductByIdTest_whenSettingParentId_thenAppliesDefaultsAndValidatesParent() {
    // given
    ProductPatchRequest patchRequest = ProductPatchRequest.builder().parentId("prod-io").build();

    Product current = new Product();
    current.setProductId("prod-io-premium");
    current.setVersion(2);

    Product parent = Product.builder().productId("prod-io").build();

    when(productRepository.findProductById("prod-io-premium"))
        .thenReturn(Uni.createFrom().item(current));
    when(productRepository.findProductById("prod-io")).thenReturn(Uni.createFrom().item(parent));

    Product patched = new Product();
    patched.setProductId("prod-io-premium");
    patched.setParentId("prod-io");
    patched.setVersion(2);

    when(productMapperRequest.toPatch(patchRequest, current)).thenReturn(patched);
    when(productRepository.persist(any(Product.class))).thenReturn(Uni.createFrom().item(patched));
    when(productMapperResponse.toProductResponse(any(Product.class)))
        .thenReturn(new ProductResponse());

    // when
    productService
        .patchProductById("AR", "prod-io-premium", "createdBy", patchRequest)
        .await()
        .indefinitely();

    // then
    ArgumentCaptor<Product> persisted = ArgumentCaptor.forClass(Product.class);
    verify(productRepository).persist(persisted.capture());
    assertThat(persisted.getValue().getParentId()).isEqualTo("prod-io");
    assertThat(persisted.getValue().getFeatures().isRequiresParentOnboarding()).isTrue();
  }

  @Test
  void patchProductByIdTest_whenClearingParentId_thenRemovesRequiresParentOnboarding() {
    // given
    ProductPatchRequest patchRequest = ProductPatchRequest.builder().parentId("").build();

    Product current = new Product();
    current.setProductId("prod-io-premium");
    current.setVersion(2);
    current.setFeatures(Features.builder().requiresParentOnboarding(true).build());

    when(productRepository.findProductById("prod-io-premium"))
        .thenReturn(Uni.createFrom().item(current));

    Product patched = new Product();
    patched.setProductId("prod-io-premium");
    patched.setParentId("");
    patched.setVersion(2);
    patched.setFeatures(Features.builder().requiresParentOnboarding(true).build());

    when(productMapperRequest.toPatch(patchRequest, current)).thenReturn(patched);
    when(productRepository.persist(any(Product.class))).thenReturn(Uni.createFrom().item(patched));
    when(productMapperResponse.toProductResponse(any(Product.class)))
        .thenReturn(new ProductResponse());

    // when
    productService
        .patchProductById("AR", "prod-io-premium", "createdBy", patchRequest)
        .await()
        .indefinitely();

    // then
    ArgumentCaptor<Product> persisted = ArgumentCaptor.forClass(Product.class);
    verify(productRepository).persist(persisted.capture());
    assertThat(persisted.getValue().getParentId()).isEmpty();
    assertThat(persisted.getValue().getFeatures().isRequiresParentOnboarding()).isFalse();
    verify(productRepository, never()).findProductById("");
  }

  // -------------------------------------------------------------------------
  // isRequiredDocumentsEnabled
  // -------------------------------------------------------------------------

  @Test
  void isRequiredDocumentsEnabled_returnsTrue_whenFilterMatchesInstitutionTypeAndOrigin() {
    // given
    Product product =
        Product.builder()
            .productId("prod-test")
            .requiredDocuments(
                List.of(
                    RequiredDocument.builder()
                        .id("doc-1")
                        .name("Allegato A")
                        .filter(
                            RequiredDocumentFilter.builder()
                                .institutionType(List.of(InstitutionType.GSP))
                                .origin(List.of(Origin.SELC))
                                .build())
                        .build()))
            .build();

    when(productRepository.findProductById("prod-test")).thenReturn(Uni.createFrom().item(product));

    // when
    Boolean enabled =
        productService
            .isRequiredDocumentsEnabled("AR", "prod-test", InstitutionType.GSP, Origin.SELC)
            .await()
            .indefinitely();

    // then
    assertTrue(enabled);
    verify(productRepository, times(1)).findProductById("prod-test");
  }

  @Test
  void isRequiredDocumentsEnabled_returnsFalse_whenFilterDoesNotMatch() {
    // given
    Product product =
        Product.builder()
            .productId("prod-test")
            .requiredDocuments(
                List.of(
                    RequiredDocument.builder()
                        .id("doc-1")
                        .name("Allegato A")
                        .filter(
                            RequiredDocumentFilter.builder()
                                .institutionType(List.of(InstitutionType.GSP))
                                .origin(List.of(Origin.SELC))
                                .build())
                        .build()))
            .build();

    when(productRepository.findProductById("prod-test")).thenReturn(Uni.createFrom().item(product));

    // when
    Boolean enabled =
        productService
            .isRequiredDocumentsEnabled("AR", "prod-test", InstitutionType.PA, Origin.IPA)
            .await()
            .indefinitely();

    // then
    assertFalse(enabled);
  }

  @Test
  void isRequiredDocumentsEnabled_returnsFalse_whenRequiredDocumentsIsNull() {
    // given
    Product product = Product.builder().productId("prod-test").requiredDocuments(null).build();

    when(productRepository.findProductById("prod-test")).thenReturn(Uni.createFrom().item(product));

    // when
    Boolean enabled =
        productService
            .isRequiredDocumentsEnabled("AR", "prod-test", InstitutionType.PA, Origin.IPA)
            .await()
            .indefinitely();

    // then
    assertFalse(enabled);
  }

  @Test
  void isRequiredDocumentsEnabled_returnsFalse_whenRequiredDocumentsIsEmpty() {
    // given
    Product product = Product.builder().productId("prod-test").requiredDocuments(List.of()).build();

    when(productRepository.findProductById("prod-test")).thenReturn(Uni.createFrom().item(product));

    // when
    Boolean enabled =
        productService
            .isRequiredDocumentsEnabled("AR", "prod-test", InstitutionType.PA, Origin.IPA)
            .await()
            .indefinitely();

    // then
    assertFalse(enabled);
  }

  @Test
  void isRequiredDocumentsEnabled_returnsFalse_whenFilterIsNull() {
    // given
    Product product =
        Product.builder()
            .productId("prod-test")
            .requiredDocuments(
                List.of(
                    RequiredDocument.builder().id("doc-1").name("Allegato A").filter(null).build()))
            .build();

    when(productRepository.findProductById("prod-test")).thenReturn(Uni.createFrom().item(product));

    // when
    Boolean enabled =
        productService
            .isRequiredDocumentsEnabled("AR", "prod-test", InstitutionType.PA, Origin.IPA)
            .await()
            .indefinitely();

    // then
    assertFalse(enabled);
  }

  @Test
  void isRequiredDocumentsEnabled_returnsTrue_whenOneOfMultipleDocumentsMatches() {
    // given
    Product product =
        Product.builder()
            .productId("prod-test")
            .requiredDocuments(
                List.of(
                    RequiredDocument.builder()
                        .id("doc-1")
                        .filter(
                            RequiredDocumentFilter.builder()
                                .institutionType(List.of(InstitutionType.GSP))
                                .origin(List.of(Origin.SELC))
                                .build())
                        .build(),
                    RequiredDocument.builder()
                        .id("doc-2")
                        .filter(
                            RequiredDocumentFilter.builder()
                                .institutionType(List.of(InstitutionType.PA))
                                .origin(List.of(Origin.IPA))
                                .build())
                        .build()))
            .build();

    when(productRepository.findProductById("prod-test")).thenReturn(Uni.createFrom().item(product));

    // when
    Boolean enabled =
        productService
            .isRequiredDocumentsEnabled("AR", "prod-test", InstitutionType.PA, Origin.IPA)
            .await()
            .indefinitely();

    // then
    assertTrue(enabled);
  }

  @Test
  void isRequiredDocumentsEnabled_throwsNotFound_whenProductDoesNotExist() {
    // given
    when(productRepository.findProductById("prod-missing")).thenReturn(Uni.createFrom().nullItem());

    // when
    Throwable thrown =
        catchThrowable(
            () ->
                productService
                    .isRequiredDocumentsEnabled(
                        "AR", "prod-missing", InstitutionType.PA, Origin.IPA)
                    .await()
                    .indefinitely());

    // then
    assertThat(thrown).isInstanceOf(NotFoundException.class).hasMessageContaining("prod-missing");
  }

  @Test
  void isRequiredDocumentsEnabled_throwsIllegalArgument_whenProductIdIsBlank() {
    // when
    Throwable thrown =
        catchThrowable(
            () ->
                productService
                    .isRequiredDocumentsEnabled("AR", "  ", InstitutionType.PA, Origin.IPA)
                    .await()
                    .indefinitely());

    // then
    assertThat(thrown).isInstanceOf(IllegalArgumentException.class).hasMessage("Missing productId");
    verify(productRepository, never()).findProductById(anyString());
  }

  @Test
  void isRequiredDocumentsEnabled_throwsIllegalArgument_whenInstitutionTypeIsNull() {
    // when
    Throwable thrown =
        catchThrowable(
            () ->
                productService
                    .isRequiredDocumentsEnabled("AR", "prod-test", null, Origin.IPA)
                    .await()
                    .indefinitely());

    // then
    assertThat(thrown)
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Missing institutionType");
    verify(productRepository, never()).findProductById(anyString());
  }

  @Test
  void isRequiredDocumentsEnabled_throwsIllegalArgument_whenOriginIsNull() {
    // when
    Throwable thrown =
        catchThrowable(
            () ->
                productService
                    .isRequiredDocumentsEnabled("AR", "prod-test", InstitutionType.PA, null)
                    .await()
                    .indefinitely());

    // then
    assertThat(thrown).isInstanceOf(IllegalArgumentException.class).hasMessage("Missing origin");
    verify(productRepository, never()).findProductById(anyString());
  }

  @Test
  void isRequiredDocumentsEnabled_returnsFalse_whenOriginMatchesButInstitutionTypeDoesNot() {
    // given
    Product product =
        Product.builder()
            .productId("prod-test")
            .requiredDocuments(
                List.of(
                    RequiredDocument.builder()
                        .id("doc-1")
                        .filter(
                            RequiredDocumentFilter.builder()
                                .institutionType(List.of(InstitutionType.GSP))
                                .origin(List.of(Origin.IPA))
                                .build())
                        .build()))
            .build();

    when(productRepository.findProductById("prod-test")).thenReturn(Uni.createFrom().item(product));

    // when â€” origin IPA matches but institutionType PA does not match GSP
    Boolean enabled =
        productService
            .isRequiredDocumentsEnabled("AR", "prod-test", InstitutionType.PA, Origin.IPA)
            .await()
            .indefinitely();

    // then
    assertFalse(enabled);
  }

  // -------------------------------------------------------------------------
  // getRequiredDocuments
  // -------------------------------------------------------------------------

  @Test
  void getRequiredDocuments_returnsFilteredAndMappedList() {
    // given
    Product product =
        Product.builder()
            .productId("prod-test")
            .requiredDocuments(
                List.of(
                    RequiredDocument.builder()
                        .id("statuto")
                        .name("Statuto")
                        .labelKey("statuto")
                        .required(true)
                        .mimeType("application/pdf")
                        .filter(
                            RequiredDocumentFilter.builder()
                                .institutionType(List.of(InstitutionType.GSP))
                                .origin(List.of(Origin.SELC))
                                .build())
                        .build(),
                    RequiredDocument.builder()
                        .id("attestazione")
                        .name("Attestazione")
                        .labelKey("attestazione")
                        .required(true)
                        .mimeType("application/pdf")
                        .maxDocumentsRequired(3)
                        .filter(
                            RequiredDocumentFilter.builder()
                                .institutionType(List.of(InstitutionType.GSP))
                                .origin(List.of(Origin.SELC))
                                .build())
                        .build(),
                    RequiredDocument.builder()
                        .id("other")
                        .name("Other")
                        .filter(
                            RequiredDocumentFilter.builder()
                                .institutionType(List.of(InstitutionType.PA))
                                .origin(List.of(Origin.IPA))
                                .build())
                        .build()))
            .build();

    when(productRepository.findProductById("prod-test")).thenReturn(Uni.createFrom().item(product));

    RequiredDocumentResponse mappedStatuto =
        RequiredDocumentResponse.builder()
            .id("statuto")
            .name("Statuto")
            .labelKey("statuto")
            .required(true)
            .mimeType("application/pdf")
            .maxDocumentsRequired(1)
            .build();
    RequiredDocumentResponse mappedAttestazione =
        RequiredDocumentResponse.builder()
            .id("attestazione")
            .name("Attestazione")
            .labelKey("attestazione")
            .required(true)
            .mimeType("application/pdf")
            .maxDocumentsRequired(3)
            .build();
    when(productMapperResponse.toRequiredDocumentResponse(any(RequiredDocument.class)))
        .thenAnswer(
            inv -> {
              RequiredDocument d = inv.getArgument(0, RequiredDocument.class);
              return "statuto".equals(d.getId()) ? mappedStatuto : mappedAttestazione;
            });

    // when
    List<RequiredDocumentResponse> result =
        productService
            .getRequiredDocuments("AR", "prod-test", InstitutionType.GSP, Origin.SELC)
            .await()
            .indefinitely();

    // then
    assertNotNull(result);
    assertEquals(2, result.size());
    assertEquals("statuto", result.get(0).getId());
    assertEquals(1, result.get(0).getMaxDocumentsRequired());
    assertEquals("attestazione", result.get(1).getId());
    assertEquals(3, result.get(1).getMaxDocumentsRequired());
    verify(productMapperResponse, times(2)).toRequiredDocumentResponse(any(RequiredDocument.class));
  }

  @Test
  void getRequiredDocuments_returnsEmptyList_whenNoDocumentMatchesContext() {
    // given
    Product product =
        Product.builder()
            .productId("prod-test")
            .requiredDocuments(
                List.of(
                    RequiredDocument.builder()
                        .id("doc-1")
                        .filter(
                            RequiredDocumentFilter.builder()
                                .institutionType(List.of(InstitutionType.GSP))
                                .origin(List.of(Origin.SELC))
                                .build())
                        .build()))
            .build();

    when(productRepository.findProductById("prod-test")).thenReturn(Uni.createFrom().item(product));

    // when
    List<RequiredDocumentResponse> result =
        productService
            .getRequiredDocuments("AR", "prod-test", InstitutionType.PA, Origin.IPA)
            .await()
            .indefinitely();

    // then
    assertNotNull(result);
    assertTrue(result.isEmpty());
    verify(productMapperResponse, never()).toRequiredDocumentResponse(any(RequiredDocument.class));
  }

  @Test
  void getRequiredDocuments_returnsEmptyList_whenRequiredDocumentsIsNull() {
    // given
    Product product = Product.builder().productId("prod-test").requiredDocuments(null).build();
    when(productRepository.findProductById("prod-test")).thenReturn(Uni.createFrom().item(product));

    // when
    List<RequiredDocumentResponse> result =
        productService
            .getRequiredDocuments("AR", "prod-test", InstitutionType.PA, Origin.IPA)
            .await()
            .indefinitely();

    // then
    assertNotNull(result);
    assertTrue(result.isEmpty());
  }

  @Test
  void getRequiredDocuments_throwsNotFound_whenProductDoesNotExist() {
    // given
    when(productRepository.findProductById("prod-missing")).thenReturn(Uni.createFrom().nullItem());

    // when
    Throwable thrown =
        catchThrowable(
            () ->
                productService
                    .getRequiredDocuments("AR", "prod-missing", InstitutionType.PA, Origin.IPA)
                    .await()
                    .indefinitely());

    // then
    assertThat(thrown).isInstanceOf(NotFoundException.class).hasMessageContaining("prod-missing");
  }

  @Test
  void getRequiredDocuments_throwsIllegalArgument_whenProductIdIsBlank() {
    // when
    Throwable thrown =
        catchThrowable(
            () ->
                productService
                    .getRequiredDocuments("AR", "  ", InstitutionType.PA, Origin.IPA)
                    .await()
                    .indefinitely());

    // then
    assertThat(thrown).isInstanceOf(IllegalArgumentException.class).hasMessage("Missing productId");
    verify(productRepository, never()).findProductById(anyString());
  }

  @Test
  void getRequiredDocuments_throwsIllegalArgument_whenInstitutionTypeIsNull() {
    // when
    Throwable thrown =
        catchThrowable(
            () ->
                productService
                    .getRequiredDocuments("AR", "prod-test", null, Origin.IPA)
                    .await()
                    .indefinitely());

    // then
    assertThat(thrown)
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Missing institutionType");
  }

  @Test
  void getRequiredDocuments_throwsIllegalArgument_whenOriginIsNull() {
    // when
    Throwable thrown =
        catchThrowable(
            () ->
                productService
                    .getRequiredDocuments("AR", "prod-test", InstitutionType.PA, null)
                    .await()
                    .indefinitely());

    // then
    assertThat(thrown).isInstanceOf(IllegalArgumentException.class).hasMessage("Missing origin");
  }

  // -------------------------------------------------------------------------
  // getValidProductById
  // -------------------------------------------------------------------------

  @Test
  void getValidProductById_ok_whenProductActiveAndNoParent() {
    // given
    Product product = Product.builder().productId("prod-test").status(ProductStatus.ACTIVE).build();

    when(productRepository.findProductById("prod-test")).thenReturn(Uni.createFrom().item(product));

    ProductResponse mapped = new ProductResponse();
    mapped.setProductId("prod-test");
    mapped.setStatus(ProductStatus.ACTIVE);
    when(productMapperResponse.toProductResponse(product)).thenReturn(mapped);

    // when
    ProductResponse out = productService.getValidProduct("AR", "prod-test").await().indefinitely();

    // then
    assertNotNull(out);
    assertEquals("prod-test", out.getProductId());
    assertEquals(ProductStatus.ACTIVE, out.getStatus());
    verify(productRepository, times(1)).findProductById("prod-test");
  }

  @Test
  void getValidProductById_ok_whenParentIsValid() {
    // given
    Product product =
        Product.builder()
            .productId("prod-child")
            .parentId("prod-parent")
            .status(ProductStatus.ACTIVE)
            .build();
    Product parent =
        Product.builder().productId("prod-parent").status(ProductStatus.ACTIVE).build();

    when(productRepository.findProductById("prod-child"))
        .thenReturn(Uni.createFrom().item(product));
    when(productRepository.findProductById("prod-parent"))
        .thenReturn(Uni.createFrom().item(parent));

    ProductResponse mapped = new ProductResponse();
    mapped.setProductId("prod-child");
    when(productMapperResponse.toProductResponse(product)).thenReturn(mapped);

    // when
    ProductResponse out = productService.getValidProduct("AR", "prod-child").await().indefinitely();

    // then
    assertNotNull(out);
    assertEquals("prod-child", out.getProductId());
    verify(productRepository, times(1)).findProductById("prod-parent");
  }

  @Test
  void getValidProductById_throwsIllegalArgument_whenProductIdIsBlank() {
    // when
    Throwable thrown =
        catchThrowable(
            () -> productService.getValidProduct("AR", StringUtils.EMPTY).await().indefinitely());

    // then
    assertThat(thrown).isInstanceOf(IllegalArgumentException.class);
    verify(productRepository, never()).findProductById(anyString());
  }

  @Test
  void getValidProductById_throwsNotFound_whenProductDoesNotExist() {
    // given
    when(productRepository.findProductById("prod-missing")).thenReturn(Uni.createFrom().nullItem());

    // when
    Throwable thrown =
        catchThrowable(
            () -> productService.getValidProduct("AR", "prod-missing").await().indefinitely());

    // then
    assertThat(thrown).isInstanceOf(NotFoundException.class).hasMessageContaining("prod-missing");
    verify(productMapperResponse, never()).toProductResponse(any(Product.class));
  }

  @Test
  void getValidProductById_throwsNotFound_whenProductIsNotValid() {
    // given
    Product product =
        Product.builder().productId("prod-test").status(ProductStatus.PHASE_OUT).build();

    when(productRepository.findProductById("prod-test")).thenReturn(Uni.createFrom().item(product));

    // when
    Throwable thrown =
        catchThrowable(
            () -> productService.getValidProduct("AR", "prod-test").await().indefinitely());

    // then
    assertThat(thrown).isInstanceOf(NotFoundException.class);
    verify(productMapperResponse, never()).toProductResponse(any(Product.class));
  }

  @Test
  void getValidProductById_throwsNotFound_whenParentIsNotValid() {
    // given
    Product product =
        Product.builder()
            .productId("prod-child")
            .parentId("prod-parent")
            .status(ProductStatus.ACTIVE)
            .build();
    Product parent =
        Product.builder().productId("prod-parent").status(ProductStatus.INACTIVE).build();

    when(productRepository.findProductById("prod-child"))
        .thenReturn(Uni.createFrom().item(product));
    when(productRepository.findProductById("prod-parent"))
        .thenReturn(Uni.createFrom().item(parent));

    // when
    Throwable thrown =
        catchThrowable(
            () -> productService.getValidProduct("AR", "prod-child").await().indefinitely());

    // then
    assertThat(thrown).isInstanceOf(NotFoundException.class);
    verify(productMapperResponse, never()).toProductResponse(any(Product.class));
  }

  // -------------------------------------------------------------------------
  // getProductExpirationDays
  // -------------------------------------------------------------------------

  @Test
  void getProductExpirationDays_returnsConfiguredValue() {
    // given
    Product product = Product.builder().productId("prod-test").status(ProductStatus.ACTIVE).build();

    when(productRepository.findProductById("prod-test")).thenReturn(Uni.createFrom().item(product));

    ProductResponse mapped = new ProductResponse();
    mapped.setProductId("prod-test");
    mapped.setFeatures(Features.builder().expirationDays(60).build());
    when(productMapperResponse.toProductResponse(product)).thenReturn(mapped);

    // when
    ProductExpirationResponse out =
        productService.getProductExpirationDays("AR", "prod-test").await().indefinitely();

    // then
    assertNotNull(out);
    assertEquals(60, out.getExpirationDays());
  }

  @Test
  void getProductExpirationDays_returnsDefault_whenFeaturesIsNull() {
    // given
    Product product = Product.builder().productId("prod-test").status(ProductStatus.ACTIVE).build();

    when(productRepository.findProductById("prod-test")).thenReturn(Uni.createFrom().item(product));

    ProductResponse mapped = new ProductResponse();
    mapped.setProductId("prod-test");
    mapped.setFeatures(null);
    when(productMapperResponse.toProductResponse(product)).thenReturn(mapped);

    // when
    ProductExpirationResponse out =
        productService.getProductExpirationDays("AR", "prod-test").await().indefinitely();

    // then
    assertNotNull(out);
    assertEquals(30, out.getExpirationDays());
  }

  @Test
  void getProductExpirationDays_throwsNotFound_whenProductIsNotValid() {
    // given
    Product product =
        Product.builder().productId("prod-test").status(ProductStatus.INACTIVE).build();

    when(productRepository.findProductById("prod-test")).thenReturn(Uni.createFrom().item(product));

    // when
    Throwable thrown =
        catchThrowable(
            () ->
                productService.getProductExpirationDays("AR", "prod-test").await().indefinitely());

    // then
    assertThat(thrown).isInstanceOf(NotFoundException.class);
  }

  // -------------------------------------------------------------------------
  // getProducts
  // -------------------------------------------------------------------------

  @Test
  void getProducts_returnsAll_whenRootOnlyFalseAndValidFalse() {
    // given
    Product root = Product.builder().productId("prod-a").status(ProductStatus.ACTIVE).build();
    Product child =
        Product.builder()
            .productId("prod-a-premium")
            .parentId("prod-a")
            .status(ProductStatus.ACTIVE)
            .build();
    Product phaseOut =
        Product.builder().productId("prod-b").status(ProductStatus.PHASE_OUT).build();

    when(productRepository.findLatestVersionForEachProduct())
        .thenReturn(Uni.createFrom().item(List.of(root, child, phaseOut)));
    mockToProductResponseEcho();

    // when
    List<ProductResponse> out =
        productService.getProducts("AR", false, false).await().indefinitely();

    // then
    assertEquals(3, out.size());
  }

  @Test
  void getProducts_excludesChildren_whenRootOnlyTrue() {
    // given
    Product root = Product.builder().productId("prod-a").status(ProductStatus.ACTIVE).build();
    Product child =
        Product.builder()
            .productId("prod-a-premium")
            .parentId("prod-a")
            .status(ProductStatus.ACTIVE)
            .build();

    when(productRepository.findLatestVersionForEachProduct())
        .thenReturn(Uni.createFrom().item(List.of(root, child)));
    mockToProductResponseEcho();

    // when
    List<ProductResponse> out =
        productService.getProducts("AR", true, false).await().indefinitely();

    // then
    assertEquals(1, out.size());
    assertEquals("prod-a", out.get(0).getProductId());
  }

  @Test
  void getProducts_excludesNotValid_whenValidTrue() {
    // given
    Product root = Product.builder().productId("prod-a").status(ProductStatus.ACTIVE).build();
    Product inactive = Product.builder().productId("prod-b").status(ProductStatus.INACTIVE).build();
    Product phaseOut =
        Product.builder().productId("prod-c").status(ProductStatus.PHASE_OUT).build();

    when(productRepository.findLatestVersionForEachProduct())
        .thenReturn(Uni.createFrom().item(List.of(root, inactive, phaseOut)));
    mockToProductResponseEcho();

    // when
    List<ProductResponse> out =
        productService.getProducts("AR", false, true).await().indefinitely();

    // then
    assertEquals(1, out.size());
    assertEquals("prod-a", out.get(0).getProductId());
  }

  @Test
  void getProducts_appliesBothFilters_whenRootOnlyTrueAndValidTrue() {
    // given
    Product root = Product.builder().productId("prod-a").status(ProductStatus.ACTIVE).build();
    Product child =
        Product.builder()
            .productId("prod-a-premium")
            .parentId("prod-a")
            .status(ProductStatus.ACTIVE)
            .build();
    Product phaseOut =
        Product.builder().productId("prod-b").status(ProductStatus.PHASE_OUT).build();

    when(productRepository.findLatestVersionForEachProduct())
        .thenReturn(Uni.createFrom().item(List.of(root, child, phaseOut)));
    mockToProductResponseEcho();

    // when
    List<ProductResponse> out = productService.getProducts("AR", true, true).await().indefinitely();

    // then
    assertEquals(1, out.size());
    assertEquals("prod-a", out.get(0).getProductId());
  }

  private void mockToProductResponseEcho() {
    when(productMapperResponse.toProductResponse(any(Product.class)))
        .thenAnswer(
            inv -> {
              Product p = inv.getArgument(0, Product.class);
              ProductResponse r = new ProductResponse();
              r.setProductId(p.getProductId());
              r.setParentId(p.getParentId());
              r.setStatus(p.getStatus());
              return r;
            });
  }

  @Test
  void getValidProductById_throwsNotFound_whenProductIsDeleted() {
    // given
    Product product =
        Product.builder().productId("prod-test").status(ProductStatus.DELETED).build();

    when(productRepository.findProductById("prod-test")).thenReturn(Uni.createFrom().item(product));

    // when
    Throwable thrown =
        catchThrowable(
            () -> productService.getValidProduct("AR", "prod-test").await().indefinitely());

    // then
    assertThat(thrown).isInstanceOf(NotFoundException.class);
    verify(productMapperResponse, never()).toProductResponse(any(Product.class));
  }

  @Test
  void getProducts_excludesDeleted_whenValidTrue() {
    // given
    Product root = Product.builder().productId("prod-a").status(ProductStatus.ACTIVE).build();
    Product deleted = Product.builder().productId("prod-b").status(ProductStatus.DELETED).build();

    when(productRepository.findLatestVersionForEachProduct())
        .thenReturn(Uni.createFrom().item(List.of(root, deleted)));
    mockToProductResponseEcho();

    // when
    List<ProductResponse> out =
        productService.getProducts("AR", false, true).await().indefinitely();

    // then
    assertEquals(1, out.size());
    assertEquals("prod-a", out.get(0).getProductId());
  }

  // -------------------------------------------------------------------------
  // validateProductRole
  // -------------------------------------------------------------------------

  @Test
  void validateProductRole_ok_returnsMatchingRole() {
    // given
    Product product =
        Product.builder()
            .productId("prod-test")
            .roleMappings(
                List.of(
                    RoleMapping.builder()
                        .role(UserRole.MANAGER.name())
                        .backOfficeRoles(
                            List.of(
                                BackOfficeRole.builder().code("admin").label("Admin").build(),
                                BackOfficeRole.builder().code("ref").label("Referente").build()))
                        .build()))
            .build();

    when(productRepository.findProductById("prod-test")).thenReturn(Uni.createFrom().item(product));
    when(productMapperResponse.toProductRoleResponse(any(BackOfficeRole.class)))
        .thenAnswer(
            inv -> {
              BackOfficeRole b = inv.getArgument(0, BackOfficeRole.class);
              return ProductRoleResponse.builder().code(b.getCode()).label(b.getLabel()).build();
            });

    // when
    ProductRoleResponse out =
        productService
            .validateProductRole("AR", "prod-test", UserRole.MANAGER, "ref")
            .await()
            .indefinitely();

    // then
    assertNotNull(out);
    assertEquals("ref", out.getCode());
    assertEquals("Referente", out.getLabel());
  }

  @Test
  void validateProductRole_throwsNotFound_whenRoleHasNoMappings() {
    // given
    Product product =
        Product.builder()
            .productId("prod-test")
            .roleMappings(
                List.of(
                    RoleMapping.builder()
                        .role(UserRole.MANAGER.name())
                        .backOfficeRoles(List.of(BackOfficeRole.builder().code("admin").build()))
                        .build()))
            .build();

    when(productRepository.findProductById("prod-test")).thenReturn(Uni.createFrom().item(product));

    // when
    Throwable thrown =
        catchThrowable(
            () ->
                productService
                    .validateProductRole("AR", "prod-test", UserRole.DELEGATE, "admin")
                    .await()
                    .indefinitely());

    // then
    assertThat(thrown).isInstanceOf(NotFoundException.class).hasMessageContaining("DELEGATE");
  }

  @Test
  void validateProductRole_throwsNotFound_whenProductRoleNotFound() {
    // given
    Product product =
        Product.builder()
            .productId("prod-test")
            .roleMappings(
                List.of(
                    RoleMapping.builder()
                        .role(UserRole.MANAGER.name())
                        .backOfficeRoles(List.of(BackOfficeRole.builder().code("admin").build()))
                        .build()))
            .build();

    when(productRepository.findProductById("prod-test")).thenReturn(Uni.createFrom().item(product));

    // when
    Throwable thrown =
        catchThrowable(
            () ->
                productService
                    .validateProductRole("AR", "prod-test", UserRole.MANAGER, "not-existing")
                    .await()
                    .indefinitely());

    // then
    assertThat(thrown).isInstanceOf(NotFoundException.class).hasMessageContaining("not-existing");
    verify(productMapperResponse, never()).toProductRoleResponse(any(BackOfficeRole.class));
  }

  @Test
  void validateProductRole_throwsNotFound_whenProductDoesNotExist() {
    // given
    when(productRepository.findProductById("prod-missing")).thenReturn(Uni.createFrom().nullItem());

    // when
    Throwable thrown =
        catchThrowable(
            () ->
                productService
                    .validateProductRole("AR", "prod-missing", UserRole.MANAGER, "admin")
                    .await()
                    .indefinitely());

    // then
    assertThat(thrown).isInstanceOf(NotFoundException.class).hasMessageContaining("prod-missing");
  }

  @Test
  void validateProductRole_throwsBadRequest_whenProductIdIsBlank() {
    // when
    Throwable thrown =
        catchThrowable(
            () ->
                productService
                    .validateProductRole("AR", "  ", UserRole.MANAGER, "admin")
                    .await()
                    .indefinitely());

    // then
    assertThat(thrown).isInstanceOf(BadRequestException.class).hasMessage("Missing productId");
    verify(productRepository, never()).findProductById(anyString());
  }

  @Test
  void validateProductRole_throwsBadRequest_whenRoleIsNull() {
    // when
    Throwable thrown =
        catchThrowable(
            () ->
                productService
                    .validateProductRole("AR", "prod-test", null, "admin")
                    .await()
                    .indefinitely());

    // then
    assertThat(thrown).isInstanceOf(BadRequestException.class).hasMessage("Missing role");
    verify(productRepository, never()).findProductById(anyString());
  }

  @Test
  void validateProductRole_throwsBadRequest_whenProductRoleIsBlank() {
    // when
    Throwable thrown =
        catchThrowable(
            () ->
                productService
                    .validateProductRole("AR", "prod-test", UserRole.MANAGER, "  ")
                    .await()
                    .indefinitely());

    // then
    assertThat(thrown).isInstanceOf(BadRequestException.class).hasMessage("Missing productRole");
    verify(productRepository, never()).findProductById(anyString());
  }
}
