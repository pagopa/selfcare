package it.pagopa.selfcare.product.mapper;

import it.pagopa.selfcare.product.model.Product;
import it.pagopa.selfcare.product.model.RequiredDocument;
import it.pagopa.selfcare.product.model.dto.response.ProductBaseResponse;
import it.pagopa.selfcare.product.model.dto.response.ProductResponse;
import it.pagopa.selfcare.product.model.dto.response.RequiredDocumentResponse;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

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

  @Test
  public void toRequiredDocumentResponse_mapsAllFields() {
    // given
    RequiredDocument doc =
        RequiredDocument.builder()
            .id("statuto")
            .name("Statuto Ente")
            .labelKey("statuto")
            .required(true)
            .mimeType("application/pdf")
            .maxDocumentsRequired(3)
            .build();

    // when
    RequiredDocumentResponse response = productMapperResponse.toRequiredDocumentResponse(doc);

    // then
    Assertions.assertNotNull(response);
    Assertions.assertEquals("statuto", response.getId());
    Assertions.assertEquals("Statuto Ente", response.getName());
    Assertions.assertEquals("statuto", response.getLabelKey());
    Assertions.assertTrue(response.isRequired());
    Assertions.assertEquals("application/pdf", response.getMimeType());
    Assertions.assertEquals(3, response.getMaxDocumentsRequired());
  }

  @Test
  public void toRequiredDocumentResponse_appliesDefaultMaxDocumentsRequired_whenNull() {
    // given
    RequiredDocument doc =
        RequiredDocument.builder()
            .id("visura")
            .name("Visura Camerale")
            .labelKey("visura")
            .required(true)
            .mimeType("application/pdf")
            .maxDocumentsRequired(null)
            .build();

    // when
    RequiredDocumentResponse response = productMapperResponse.toRequiredDocumentResponse(doc);

    // then
    Assertions.assertNotNull(response);
    Assertions.assertEquals(1, response.getMaxDocumentsRequired());
  }

  @Test
  public void toRequiredDocumentResponse_returnsNull_whenSourceIsNull() {
    // when
    RequiredDocumentResponse response = productMapperResponse.toRequiredDocumentResponse(null);

    // then
    Assertions.assertNull(response);
  }
}
