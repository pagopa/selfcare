package it.pagopa.selfcare.onboarding.connector;

import it.pagopa.selfcare.onboarding.common.InstitutionType;
import it.pagopa.selfcare.product.entity.ContractTemplate;
import it.pagopa.selfcare.product.entity.Product;
import it.pagopa.selfcare.product.entity.ProductStatus;
import it.pagopa.selfcare.product.service.ProductService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
class ProductsConnectorImplTest {
    @InjectMocks
    private ProductsConnectorImpl productConnector;
    @Mock
    private ProductService productService;

    @Test
    void getProductByInstitutionType() {
        final Product product = dummyProduct();
        ContractTemplate contractTemplate = new ContractTemplate();
        contractTemplate.setContractTemplatePath("contractTemplatePath");
        contractTemplate.setContractTemplateVersion("contractTemplateVersion");
        final Map<String, ContractTemplate> contractStorageMap = Map.of(InstitutionType.PA.name(),contractTemplate);
        product.setInstitutionContractMappings(contractStorageMap);
        final String productId = "productId";
        when(productService.getProduct(anyString())).thenReturn(product);

        Product result = productConnector.getProduct(productId, InstitutionType.PA);

        assertEquals(product, result);

        verify(productService, times(1)).getProduct(productId);

    }

    @Test
    void getProduct_institutionTypeNotPresent(){
        final Product product = dummyProduct();
        ContractTemplate contractTemplate = new ContractTemplate();
        contractTemplate.setContractTemplatePath("contractTemplatePath");
        contractTemplate.setContractTemplateVersion("contractTemplateVersion");
        final Map<String, ContractTemplate> contractStorageMap = Map.of(InstitutionType.PA.name(),contractTemplate);
        product.setInstitutionContractMappings(contractStorageMap);
        final String productId = "productId";
        when(productService.getProduct(anyString())).thenReturn(product);

        Product result = productConnector.getProduct(productId, InstitutionType.SA);

        assertEquals(product, result);

        verify(productService, times(1)).getProduct(productId);

    }

    @Test
    void testGetProductValid() {
        Product product = dummyProduct();
        when(productService.getProductIsValid(any())).thenReturn(product);
        assertSame(product, productConnector.getProductValid("42"));
        verify(productService).getProductIsValid(any());
    }

    @Test
    void getProducts() {
        Product product = dummyProduct();
        List<Product> products = List.of(product);
        when(productService.getProducts(anyBoolean(), anyBoolean())).thenReturn(products);
        assertSame(products, productConnector.getProducts(true));
        verify(productService).getProducts(anyBoolean(), anyBoolean());
    }

    private Product dummyProduct(){
        Product product = new Product();
        ContractTemplate contractTemplate = new ContractTemplate();
        contractTemplate.setContractTemplatePath("Contract Template Path");
        contractTemplate.setContractTemplateVersion("1.0.2");
        Map<String, ContractTemplate> institutionContractMapping = new HashMap<>();
        institutionContractMapping.put(Product.CONTRACT_TYPE_DEFAULT, contractTemplate);
        product.setInstitutionContractMappings(institutionContractMapping);

        product.setId("42");
        product.setParentId("42");
        product.setRoleMappings(null);
        product.setStatus(ProductStatus.ACTIVE);
        product.setTitle("Dr");
        return product;
    }
}
