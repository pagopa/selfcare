package it.pagopa.selfcare.onboarding.connector;

import it.pagopa.selfcare.onboarding.connector.model.product.OriginResult;
import it.pagopa.selfcare.onboarding.connector.rest.client.MsProductApiClient;
import it.pagopa.selfcare.onboarding.connector.rest.mapper.ProductMapper;
import it.pagopa.selfcare.product.generated.openapi.v1.dto.ProductOriginResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ContextConfiguration;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.*;

@ContextConfiguration(classes = {ProductMsConnectorImplTest.class})
@ExtendWith(MockitoExtension.class)
class ProductMsConnectorImplTest {

    @Mock
    private MsProductApiClient msProductApiClientMock;

    @Mock
    private ProductMapper productMapperMock;

    @InjectMocks
    private ProductMsConnectorImpl productMsConnector;

    @Test
    void getOriginsTest_success() {
        // given
        String productId = "product-test";

        ProductOriginResponse productOriginResponse = new ProductOriginResponse();
        OriginResult mappedResult = new OriginResult();
        mappedResult.setOrigins(List.of());

        when(msProductApiClientMock._getProductOriginsById(productId))
                .thenReturn(ResponseEntity.ok(productOriginResponse));
        when(productMapperMock.toOriginResult(productOriginResponse))
                .thenReturn(mappedResult);

        // when
        OriginResult result = productMsConnector.getOrigins(productId);

        // then
        assertNotNull(result);
        assertSame(mappedResult, result);

        verify(msProductApiClientMock, times(1))._getProductOriginsById(productId);
        verify(productMapperMock, times(1)).toOriginResult(productOriginResponse);
        verifyNoMoreInteractions(msProductApiClientMock, productMapperMock);
    }

    @Test
    void getOriginsTest_nullBodyHandled() {
        // given
        String productId = "product-test";

        when(msProductApiClientMock._getProductOriginsById(productId))
                .thenReturn(ResponseEntity.ok(null));

        OriginResult mappedResult = new OriginResult();
        mappedResult.setOrigins(List.of());

        when(productMapperMock.toOriginResult(null)).thenReturn(mappedResult);

        // when
        OriginResult result = productMsConnector.getOrigins(productId);

        // then
        assertNotNull(result);
        assertSame(mappedResult, result);

        verify(msProductApiClientMock, times(1))._getProductOriginsById(productId);
        verify(productMapperMock, times(1)).toOriginResult(null);
        verifyNoMoreInteractions(msProductApiClientMock, productMapperMock);
    }


}
