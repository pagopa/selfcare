package it.pagopa.selfcare.onboarding.connector;

import it.pagopa.selfcare.onboarding.common.InstitutionType;
import it.pagopa.selfcare.onboarding.connector.api.ProductMsConnector;
import it.pagopa.selfcare.onboarding.connector.model.product.Product;
import it.pagopa.selfcare.onboarding.connector.model.product.ProductStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
class ProductsConnectorImplTest {
    @InjectMocks
    private ProductsConnectorImpl productConnector;
    @Mock
    private ProductMsConnector productMsConnector;

    @Test
    void getProductByInstitutionType() {
        final Product product = dummyProduct();
        final String productId = "productId";
        when(productMsConnector.getProduct(anyString())).thenReturn(product);

        Product result = productConnector.getProduct(productId, InstitutionType.PA);

        assertEquals(product, result);

        verify(productMsConnector, times(1)).getProduct(productId);

    }

    @Test
    void getProduct_institutionTypeNotPresent(){
        final Product product = dummyProduct();
        final String productId = "productId";
        when(productMsConnector.getProduct(anyString())).thenReturn(product);

        Product result = productConnector.getProduct(productId, InstitutionType.SA);

        assertEquals(product, result);

        verify(productMsConnector, times(1)).getProduct(productId);

    }

    @Test
    void testGetProductValid() {
        Product product = dummyProduct();
        when(productMsConnector.getValidProduct(any())).thenReturn(product);
        assertSame(product, productConnector.getProductValid("42"));
        verify(productMsConnector).getValidProduct(any());
    }

    @Test
    void getProducts() {
        Product product = dummyProduct();
        List<Product> products = List.of(product);
        when(productMsConnector.getProducts(anyBoolean())).thenReturn(products);
        assertSame(products, productConnector.getProducts(true));
        verify(productMsConnector).getProducts(anyBoolean());
    }

    @Test
    void getProduct_blankIdThrows() {
        assertThrows(IllegalArgumentException.class, () -> productConnector.getProduct(" ", InstitutionType.PA));
        verifyNoInteractions(productMsConnector);
    }

    @Test
    void getProductValid_blankIdThrows() {
        assertThrows(IllegalArgumentException.class, () -> productConnector.getProductValid(""));
        verifyNoInteractions(productMsConnector);
    }

    private Product dummyProduct(){
        Product product = new Product();

        product.setId("42");
        product.setParentId("42");
        product.setRoleMappings(null);
        product.setStatus(ProductStatus.ACTIVE);
        product.setTitle("Dr");
        return product;
    }
}
