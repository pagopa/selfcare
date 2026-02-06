package it.pagopa.selfcare.product.mapper;

import it.pagopa.selfcare.product.model.Product;
import it.pagopa.selfcare.product.model.dto.request.ProductCreateRequest;
import it.pagopa.selfcare.product.model.enums.ProductStatus;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ProductMapperRequestTest {

  private final ProductMapperRequest productMapperRequest = new ProductMapperRequestImpl();

  @Test
  public void toProductTest() {
    // given
    ProductCreateRequest productCreateRequest = ProductCreateRequest.builder()
        .productId("prod-test").build();

    // when
    Product product = productMapperRequest.toProduct(productCreateRequest);

    // then
    Assertions.assertNotNull(product);
    Assertions.assertEquals(productCreateRequest.getProductId(), product.getProductId());
  }

  @Test
  public void toCloneObjectTest() {
    // given
    Product postProduct = Product.builder().productId("prod-update").build();

    // when
    Product preProduct = Product.builder().productId("prod-base").status(ProductStatus.TESTING)
        .build();
    preProduct = productMapperRequest.cloneObject(preProduct, postProduct);

    // then
    Assertions.assertNotNull(preProduct);
    Assertions.assertNotEquals("prod-base", preProduct.getProductId());
    Assertions.assertNull(preProduct.getStatus());

  }

}
