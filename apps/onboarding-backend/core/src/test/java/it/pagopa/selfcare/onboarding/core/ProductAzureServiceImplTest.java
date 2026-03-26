package it.pagopa.selfcare.onboarding.core;

import it.pagopa.selfcare.onboarding.common.InstitutionType;
import it.pagopa.selfcare.onboarding.connector.api.ProductsConnector;
import it.pagopa.selfcare.onboarding.connector.exceptions.ResourceNotFoundException;
import it.pagopa.selfcare.product.entity.Product;
import it.pagopa.selfcare.product.entity.ProductStatus;
import it.pagopa.selfcare.product.exception.ProductNotFoundException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
class ProductAzureServiceImplTest {

    @InjectMocks
    private ProductAzureServiceImpl productService;

    @Mock
    private ProductsConnector productsConnectorMock;

    @Test
    void getProduct_nullProductId() {
        //given
        String productId = null;
        InstitutionType institutionType = InstitutionType.PA;
        //when
        Executable executable = () -> productService.getProduct(productId, institutionType);
        //then
        Exception e = Assertions.assertThrows(IllegalArgumentException.class, executable);
        Assertions.assertEquals("ProductId is required", e.getMessage());
        Mockito.verifyNoInteractions(productsConnectorMock);
    }

    @Test
    void getProductValid_nullProductId() {
        //given
        String productId = null;
        //when
        Executable executable = () -> productService.getProductValid(productId);
        //then
        Exception e = Assertions.assertThrows(IllegalArgumentException.class, executable);
        Assertions.assertEquals("ProductId is required", e.getMessage());
        Mockito.verifyNoInteractions(productsConnectorMock);
    }

    @Test
    void getProduct_InstitutionTypeNotNull() {
        //given
        String productId = "productId";
        InstitutionType institutionType = InstitutionType.PA;
        Product productMock = Mockito.mock(Product.class);
        Mockito.when(productsConnectorMock.getProduct(productId, institutionType))
                .thenReturn(productMock);
        //when
        Product product = productService.getProduct(productId, institutionType);
        //then
        Assertions.assertSame(productMock, product);
        Mockito.verify(productsConnectorMock, Mockito.times(1))
                .getProduct(productId, institutionType);
        Mockito.verifyNoMoreInteractions(productsConnectorMock);
    }

    @Test
    void getProduct_NotFound() {
        //given
        final String productId = "productId";
        InstitutionType institutionType = InstitutionType.PA;
        Mockito.when(productsConnectorMock.getProduct(productId, institutionType))
                .thenThrow(ProductNotFoundException.class);
        //when
        Executable executable = () -> productService.getProduct(productId, institutionType);
        //then
        ResourceNotFoundException e = assertThrows(ResourceNotFoundException.class, executable);
        assertEquals("No product found with id " + productId, e.getMessage());
        Mockito.verify(productsConnectorMock, Mockito.times(1))
                .getProduct(productId, institutionType);
        Mockito.verifyNoMoreInteractions(productsConnectorMock);
    }

    @Test
    void getProduct_institutionTypeNull() {
        //given
        String productId = "productId";
        InstitutionType institutionType = null;
        Product productMock = Mockito.mock(Product.class);
        Mockito.when(productsConnectorMock.getProduct(productId, institutionType))
                .thenReturn(productMock);
        //when
        Product product = productService.getProduct(productId, institutionType);
        //then
        Assertions.assertSame(productMock, product);
        Mockito.verify(productsConnectorMock, Mockito.times(1))
                .getProduct(productId, institutionType);
        Mockito.verifyNoMoreInteractions(productsConnectorMock);
    }

    @Test
    void getProductValid() {
        //given
        String productId = "productId";
        Product productMock = Mockito.mock(Product.class);
        Mockito.when(productsConnectorMock.getProductValid(productId))
                .thenReturn(productMock);
        //when
        Product product = productService.getProductValid(productId);
        //then
        Assertions.assertSame(productMock, product);
        Mockito.verify(productsConnectorMock, Mockito.times(1))
                .getProductValid(productId);
        Mockito.verifyNoMoreInteractions(productsConnectorMock);
    }

    @Test
    void getProducts() {
        //when
        Product product1 = new Product();
        product1.setStatus(ProductStatus.TESTING);
        Product product2 = new Product();
        product2.setStatus(ProductStatus.ACTIVE);
        Mockito.when(productsConnectorMock.getProducts(true))
                .thenReturn(List.of(product1, product2));
        List<Product> products = productService.getProducts(true);
        //then
        Assertions.assertNotNull(products);
        Assertions.assertEquals(1, products.size());
        Mockito.verifyNoMoreInteractions(productsConnectorMock);
    }

}
