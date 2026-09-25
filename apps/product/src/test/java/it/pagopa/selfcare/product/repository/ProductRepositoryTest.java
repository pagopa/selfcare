package it.pagopa.selfcare.product.repository;

import static org.junit.jupiter.api.Assertions.*;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import it.pagopa.selfcare.product.model.Product;
import it.pagopa.selfcare.product.testsupport.TenantMongoTestResource;
import it.pagopa.selfcare.tenant.TenantContext;
import jakarta.inject.Inject;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@QuarkusTest
@QuarkusTestResource(value = TenantMongoTestResource.class, restrictToAnnotatedClass = true)
class ProductRepositoryTest {

  @Inject ProductRepository productRepository;
  @Inject TenantContext tenantContext;

  @BeforeEach
  void clean() {
    tenantContext.setTenantId("AR");
    productRepository.deleteAll().await().indefinitely();
  }

  @Test
  void findProductByIdTest() {
    productRepository
        .persist(
            Product.builder()
                .id(UUID.randomUUID().toString())
                .tenantId("AR")
                .productId("prod-test")
                .version(1)
                .build())
        .await()
        .indefinitely();
    productRepository
        .persist(
            Product.builder()
                .id(UUID.randomUUID().toString())
                .tenantId("AR")
                .productId("prod-test")
                .version(3)
                .build())
        .await()
        .indefinitely();
    long count = productRepository.count("productId", "prod-test").await().indefinitely();

    // when
    Product result = productRepository.findProductById("prod-test").await().indefinitely();

    // then
    assertNotNull(result);
    assertEquals("prod-test", result.getProductId());
    assertEquals(3, result.getVersion());
    assertEquals(2, count);
  }

  @Test
  void findProductById_whenNotFound() {
    // when
    Product result = productRepository.findProductById("prod-test").await().indefinitely();

    // then
    assertNull(result);
  }

  @Test
  void findProductById_doesNotReturnAnotherTenant() {
    productRepository
        .persist(Product.builder().tenantId("PNPG").productId("prod-test").version(99).build())
        .await()
        .indefinitely();

    assertNull(productRepository.findProductById("prod-test").await().indefinitely());
  }

  @Test
  void findLatestVersionForEachProduct_returnsOnlyLatestVersionPerProduct() {
    productRepository
        .persist(
            List.of(
                Product.builder().tenantId("AR").productId("prod-a").version(1).build(),
                Product.builder().tenantId("AR").productId("prod-a").version(3).build(),
                Product.builder().tenantId("AR").productId("prod-b").version(2).build(),
                Product.builder().tenantId("PNPG").productId("prod-a").version(99).build()))
        .await()
        .indefinitely();

    List<Product> result =
        productRepository.findLatestVersionForEachProduct().await().indefinitely();

    assertEquals(2, result.size());
    assertTrue(
        result.stream()
            .anyMatch(
                product -> "prod-a".equals(product.getProductId()) && product.getVersion() == 3));
    assertTrue(
        result.stream()
            .anyMatch(
                product -> "prod-b".equals(product.getProductId()) && product.getVersion() == 2));
  }
}
