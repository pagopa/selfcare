package it.pagopa.selfcare.mscore.connector.rest;

import it.pagopa.selfcare.product.entity.ContractTemplate;
import it.pagopa.selfcare.product.entity.Product;
import it.pagopa.selfcare.product.entity.ProductStatus;
import it.pagopa.selfcare.product.service.ProductService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.*;

@ContextConfiguration(classes = {ProductConnectorImpl.class})
@ExtendWith(SpringExtension.class)
class ProductConnectorImplTest {
    @Autowired
    private ProductConnectorImpl productConnectorImpl;

    @MockBean
    private ProductService productService;

    /**
     * Method under test: {@link ProductConnectorImpl#getProductById(String)}
     */
    @Test
    void testGetProductById() {
        Product product = dummyProduct();
        when(productService.getProduct(any())).thenReturn(product);
        assertSame(product, productConnectorImpl.getProductById("42"));
        verify(productService).getProduct(any());
    }

    @Test
    void testGetProductValid() {
        Product product = dummyProduct();
        when(productService.getProductIsValid(any())).thenReturn(product);
        assertSame(product, productConnectorImpl.getProductValidById("42"));
        verify(productService).getProductIsValid(any());
    }

    private Product dummyProduct(){
        Product product = new Product();
        Map<String, ContractTemplate> institutionContractMappings = new HashMap<>();
        ContractTemplate contractTemplate = new ContractTemplate();
        contractTemplate.setContractTemplatePath("Contract Template Path");
        contractTemplate.setContractTemplateVersion("1.0.2");
        institutionContractMappings.put("default", contractTemplate);
        product.setInstitutionContractMappings(institutionContractMappings);
        product.setId("42");
        product.setParentId("42");
        product.setRoleMappings(null);
        product.setStatus(ProductStatus.ACTIVE);
        product.setTitle("Dr");
        return product;
    }
}

