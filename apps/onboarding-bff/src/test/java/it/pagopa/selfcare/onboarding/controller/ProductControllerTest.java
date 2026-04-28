package it.pagopa.selfcare.onboarding.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import it.pagopa.selfcare.onboarding.controller.response.ProductResource;
import it.pagopa.selfcare.onboarding.mapper.InstitutionMapper;
import it.pagopa.selfcare.onboarding.service.ProductService;
import it.pagopa.selfcare.product.entity.ContractTemplate;
import it.pagopa.selfcare.product.entity.Product;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProductControllerTest {

    @Mock
    ProductService productService;
    @Mock
    InstitutionMapper institutionMapper;

    @InjectMocks
    ProductController productController;

    @Test
    void getProduct_returnsMappedResource() {
        Product product = new Product();
        product.setId("productId");
        ProductResource expected = new ProductResource();
        expected.setId("productId");

        when(productService.getProduct("productId", null)).thenReturn(product);
        when(institutionMapper.toResource(product)).thenReturn(expected);

        ProductResource result = productController.getProduct("productId", Optional.empty());

        assertSame(expected, result);
    }

    @Test
    void getProducts_returnsMappedList() {
        Product product = new Product();
        product.setId("id");
        ProductResource expected = new ProductResource();
        expected.setId("id");

        when(productService.getProducts(true)).thenReturn(List.of(product));
        when(institutionMapper.toResource(product)).thenReturn(expected);

        List<ProductResource> result = productController.getProducts();

        assertEquals(1, result.size());
        assertEquals("id", result.get(0).getId());
    }

    @Test
    void getProductsAdmin_filtersAndMapsResults() {
        Product product = new Product();
        product.setId("id");
        var mappings = new HashMap<String, ContractTemplate>();
        ContractTemplate contractTemplate = new ContractTemplate();
        contractTemplate.setContractTemplatePath("template/path");
        mappings.put(Product.CONTRACT_TYPE_DEFAULT, contractTemplate);
        product.setUserContractMappings(mappings);

        ProductResource expected = new ProductResource();
        expected.setId("id");

        when(productService.getProducts(false)).thenReturn(List.of(product));
        when(institutionMapper.toResource(product)).thenReturn(expected);

        List<ProductResource> result = productController.getProductsAdmin();

        verify(productService).getProducts(false);
        assertEquals(1, result.size());
        assertSame(expected, result.get(0));
    }
}
