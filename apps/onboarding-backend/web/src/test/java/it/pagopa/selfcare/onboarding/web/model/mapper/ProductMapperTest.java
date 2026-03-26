package it.pagopa.selfcare.onboarding.web.model.mapper;

import it.pagopa.selfcare.commons.utils.TestUtils;
import it.pagopa.selfcare.product.entity.Product;
import it.pagopa.selfcare.onboarding.web.model.ProductResource;
import it.pagopa.selfcare.product.entity.ProductStatus;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ProductMapperTest {

    private final ProductMapper productMapper = new ProductMapperImpl();
    @Test
    void toResource_notNull() {
        //given
        Product product = new Product();
        product.setId("id");
        product.setTitle("title");
        product.setLogo("logo");
        product.setLogoBgColor("logoBgColor");
        product.setStatus(ProductStatus.ACTIVE);
        // when
        ProductResource productResource = productMapper.toResource(product);
        // then
        assertEquals(product.getId(), productResource.getId());
        assertEquals(product.getTitle(), productResource.getTitle());
        assertEquals(product.getStatus(), productResource.getStatus());
        assertEquals(product.getLogo(), productResource.getLogo());
        assertEquals(product.getLogoBgColor(), productResource.getLogoBgColor());
        TestUtils.reflectionEqualsByName(productResource, product);
    }

    @Test
    void toResource_null() {
        // given
        // when
        ProductResource productResource = productMapper.toResource(null);
        // then
        assertNull(productResource);
    }


}