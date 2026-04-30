package it.pagopa.selfcare.user.util;

import io.quarkus.test.junit.QuarkusMock;
import io.quarkus.test.junit.QuarkusTest;
import it.pagopa.selfcare.product.entity.Product;
import it.pagopa.selfcare.product.service.ProductService;
import it.pagopa.selfcare.user.util.product.ProductIdNormalizer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@QuarkusTest
class ProductIdNormalizerTest {

    @BeforeEach
    void setup() {
        ProductIdNormalizer.clearParentIds();
        ProductIdNormalizer.addParentId("child", "parent");

        ProductService productService = Mockito.mock(ProductService.class);

        Product prod1 = new Product();
        prod1.setId("prod1");

        Product prod2 = new Product();
        prod2.setId("prod2");
        prod2.setParentId("prod1");

        Mockito.when(productService.getProducts(false, true))
                .thenReturn(List.of(prod1, prod2));

        QuarkusMock.installMockForType(productService, ProductService.class);
    }

    @Test
    void shouldReturnNullWhenProductIdIsNull() {
        assertNull(ProductIdNormalizer.normalize(null));
    }

    @Test
    void shouldReturnBlankWhenProductIdIsBlank() {
        assertEquals(" ", ProductIdNormalizer.normalize(" "));
    }

    @Test
    void shouldReturnSameIdWhenProductHasNoParent() {
        final String productId = "prod-1";
        assertEquals(productId, ProductIdNormalizer.normalize(productId));
    }

    @Test
    void shouldReturnParentIdWhenProductHasParent() {
        final String productId = "child";
        final String parentId = "parent";
        assertEquals(parentId, ProductIdNormalizer.normalize(productId));
    }

    @Test
    void shouldReturnSameIdWhenProductNotFound() {
        final String productId = "unknown";
        assertEquals(productId, ProductIdNormalizer.normalize(productId));
    }

    @Test
    void shouldAddParentId() {
        final String productId = "new-child";
        final String parentId = "new-parent";
        ProductIdNormalizer.addParentId(productId, parentId);
        assertEquals(parentId, ProductIdNormalizer.normalize(productId));
        assertEquals("parent", ProductIdNormalizer.normalize("child"));
    }

    @Test
    void shouldClearParentIds() {
        ProductIdNormalizer.clearParentIds();
        assertEquals("child", ProductIdNormalizer.normalize("child"));
    }

    @Test
    void shouldUpdateParentIds() {
        ProductIdNormalizer.updateParentIds();
        assertEquals("parent", ProductIdNormalizer.normalize("child"));
        assertEquals("prod1", ProductIdNormalizer.normalize("prod1"));
        assertEquals("prod1", ProductIdNormalizer.normalize("prod2"));
    }

}