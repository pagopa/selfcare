package it.pagopa.selfcare.product.repository;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.mongodb.MongoTestResource;
import it.pagopa.selfcare.product.model.Product;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
@QuarkusTestResource(MongoTestResource.class)
class ProductRepositoryTest {

    @Inject
    ProductRepository productRepository;

    @BeforeEach
    void clean() {
        productRepository.deleteAll().await().indefinitely();
    }

    @Test
    void findProductByIdTest() {
        productRepository.persist(Product.builder().id(UUID.randomUUID().toString()).productId("prod-test").version(1).build())
                .await().indefinitely();
        productRepository.persist(Product.builder().id(UUID.randomUUID().toString()).productId("prod-test").version(3).build())
                .await().indefinitely();
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
}
