package it.pagopa.selfcare.product.mapper;

import io.quarkus.test.junit.QuarkusTest;
import it.pagopa.selfcare.product.model.dto.response.ProductBaseResponse;
import it.pagopa.selfcare.product.model.dto.response.ProductResponse;
import it.pagopa.selfcare.product.model.Product;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;

@QuarkusTest
public class ProductMapperResponseTest {

    private final ProductMapperResponse productMapperResponse = new ProductMapperResponseImpl();

    @Test
    public void toProductResponseTest() {
        // given
        Product product = Product.builder().productId("prod-test").build();

        // when
        ProductResponse productResponse = productMapperResponse.toProductResponse(product);

        // then
        Assertions.assertNotNull(productResponse);
        Assertions.assertEquals(product.getProductId(), productResponse.getProductId());
    }

    @Test
    public void toProductBaseResponseTest() {
        // given
        Product product = Product.builder().productId("prod-test").build();

        // when
        ProductBaseResponse productResponse = productMapperResponse.toProductBaseResponse(product);

        // then
        Assertions.assertNotNull(productResponse);
        Assertions.assertEquals(product.getProductId(), productResponse.getProductId());
    }

}