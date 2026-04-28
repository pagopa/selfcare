package it.pagopa.selfcare.onboarding.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import it.pagopa.selfcare.onboarding.controller.response.ProductResource;
import it.pagopa.selfcare.product.entity.Product;
import it.pagopa.selfcare.product.entity.ProductStatus;
import org.junit.jupiter.api.Test;

class ProductMapperTest {

    private final InstitutionMapper institutionMapper = new InstitutionMapperImpl();

    @Test
    void toResource_notNull() {
        Product product = new Product();
        product.setId("id");
        product.setTitle("title");
        product.setLogo("logo");
        product.setLogoBgColor("logoBgColor");
        product.setStatus(ProductStatus.ACTIVE);

        ProductResource productResource = institutionMapper.toResource(product);

        assertEquals(product.getId(), productResource.getId());
        assertEquals(product.getTitle(), productResource.getTitle());
        assertEquals(product.getStatus(), productResource.getStatus());
        assertEquals(product.getLogo(), productResource.getLogo());
        assertEquals(product.getLogoBgColor(), productResource.getLogoBgColor());
    }

    @Test
    void toResource_null() {
        assertNull(institutionMapper.toResource((Product) null));
    }
}
