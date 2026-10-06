package it.pagopa.selfcare.onboarding.core;

import it.pagopa.selfcare.onboarding.common.InstitutionType;
import it.pagopa.selfcare.onboarding.connector.exceptions.ResourceNotFoundException;
import it.pagopa.selfcare.onboarding.connector.model.product.Product;
import it.pagopa.selfcare.onboarding.connector.model.product.ProductStatus;
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
    private ProductService productServiceMock;

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
        Mockito.verifyNoInteractions(productServiceMock);
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
        Mockito.verifyNoInteractions(productServiceMock);
    }

    @Test
    void getProduct_InstitutionTypeNotNull() {
        //given
        String productId = "productId";
        InstitutionType institutionType = InstitutionType.PA;
        Product productMock = Mockito.mock(Product.class);
        Mockito.when(productServiceMock.getProduct(productId, institutionType))
                .thenReturn(productMock);
        //when
        Product product = productService.getProduct(productId, institutionType);
        //then
        Assertions.assertSame(productMock, product);
        Mockito.verify(productServiceMock, Mockito.times(1))
                .getProduct(productId, institutionType);
        Mockito.verifyNoMoreInteractions(productServiceMock);
    }

    @Test
    void getProduct_NotFound() {
        //given
        final String productId = "productId";
        InstitutionType institutionType = InstitutionType.PA;
        Mockito.when(productServiceMock.getProduct(productId, institutionType))
                .thenThrow(new ResourceNotFoundException("not found"));
        //when
        Executable executable = () -> productService.getProduct(productId, institutionType);
        //then
        ResourceNotFoundException e = assertThrows(ResourceNotFoundException.class, executable);
        assertEquals("No product found with id " + productId, e.getMessage());
        Mockito.verify(productServiceMock, Mockito.times(1))
                .getProduct(productId, institutionType);
        Mockito.verifyNoMoreInteractions(productServiceMock);
    }

    @Test
    void getProduct_institutionTypeNull() {
        //given
        String productId = "productId";
        InstitutionType institutionType = null;
        Product productMock = Mockito.mock(Product.class);
        Mockito.when(productServiceMock.getProduct(productId, institutionType))
                .thenReturn(productMock);
        //when
        Product product = productService.getProduct(productId, institutionType);
        //then
        Assertions.assertSame(productMock, product);
        Mockito.verify(productServiceMock, Mockito.times(1))
                .getProduct(productId, institutionType);
        Mockito.verifyNoMoreInteractions(productServiceMock);
    }

    @Test
    void getProductValid() {
        //given
        String productId = "productId";
        Product productMock = Mockito.mock(Product.class);
        Mockito.when(productServiceMock.getProductValid(productId))
                .thenReturn(productMock);
        //when
        Product product = productService.getProductValid(productId);
        //then
        Assertions.assertSame(productMock, product);
        Mockito.verify(productServiceMock, Mockito.times(1))
                .getProductValid(productId);
        Mockito.verifyNoMoreInteractions(productServiceMock);
    }

    @Test
    void getProducts() {
        //when
        Product product1 = new Product();
        product1.setStatus(ProductStatus.TESTING);
        Product product2 = new Product();
        product2.setStatus(ProductStatus.ACTIVE);
        Mockito.when(productServiceMock.getProducts(true)).thenReturn(List.of(product2));
        List<Product> products = productService.getProducts(true);
        //then
        Assertions.assertNotNull(products);
        Assertions.assertEquals(List.of(product2), products);
        Mockito.verify(productServiceMock).getProducts(true);
        Mockito.verifyNoMoreInteractions(productServiceMock);
    }

}
