package it.pagopa.selfcare.onboarding.connector;

import it.pagopa.selfcare.onboarding.connector.model.product.OriginResult;
import it.pagopa.selfcare.onboarding.connector.model.product.RequiredDocumentModel;
import it.pagopa.selfcare.onboarding.connector.rest.client.MsProductApiClient;
import it.pagopa.selfcare.onboarding.connector.rest.mapper.ProductMapper;
import it.pagopa.selfcare.product.generated.openapi.v1.dto.InstitutionType;
import it.pagopa.selfcare.product.generated.openapi.v1.dto.Origin;
import it.pagopa.selfcare.product.generated.openapi.v1.dto.ProductOriginResponse;
import it.pagopa.selfcare.product.generated.openapi.v1.dto.RequiredDocumentResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ContextConfiguration;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
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

    @Test
    void getRequiredDocuments_success() {
        // given
        String productId = "prod-test";
        String institutionType = "PA";
        String origin = "IPA";

        RequiredDocumentResponse dto = new RequiredDocumentResponse();
        dto.setId("doc-1");
        dto.setName("Statuto");
        dto.setRequired(true);

        RequiredDocumentModel model = new RequiredDocumentModel();
        model.setId("doc-1");
        model.setName("Statuto");
        model.setRequired(true);

        when(msProductApiClientMock._getRequiredDocuments(productId, InstitutionType.PA, Origin.IPA))
                .thenReturn(ResponseEntity.ok(List.of(dto)));
        when(productMapperMock.toRequiredDocumentModelList(List.of(dto)))
                .thenReturn(List.of(model));

        // when
        List<RequiredDocumentModel> result = productMsConnector.getRequiredDocuments(productId, institutionType, origin);

        // then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("doc-1", result.get(0).getId());

        verify(msProductApiClientMock, times(1))._getRequiredDocuments(productId, InstitutionType.PA, Origin.IPA);
        verify(productMapperMock, times(1)).toRequiredDocumentModelList(List.of(dto));
        verifyNoMoreInteractions(msProductApiClientMock, productMapperMock);
    }

    @Test
    void getRequiredDocuments_emptyList() {
        // given
        String productId = "prod-test";
        String institutionType = "PA";
        String origin = "IPA";

        when(msProductApiClientMock._getRequiredDocuments(productId, InstitutionType.PA, Origin.IPA))
                .thenReturn(ResponseEntity.ok(List.of()));
        when(productMapperMock.toRequiredDocumentModelList(List.of()))
                .thenReturn(List.of());

        // when
        List<RequiredDocumentModel> result = productMsConnector.getRequiredDocuments(productId, institutionType, origin);

        // then
        assertNotNull(result);
        assertTrue(result.isEmpty());

        verify(msProductApiClientMock, times(1))._getRequiredDocuments(productId, InstitutionType.PA, Origin.IPA);
    }

    @Test
    void isRequiredDocumentsEnabled_returnsTrue() {
        // given
        String productId = "prod-test";
        String institutionType = "PA";
        String origin = "IPA";

        when(msProductApiClientMock._isRequiredDocumentsEnabled(productId, InstitutionType.PA, Origin.IPA))
                .thenReturn(ResponseEntity.ok(Boolean.TRUE));

        // when
        boolean result = productMsConnector.isRequiredDocumentsEnabled(productId, institutionType, origin);

        // then
        assertTrue(result);

        verify(msProductApiClientMock, times(1))._isRequiredDocumentsEnabled(productId, InstitutionType.PA, Origin.IPA);
        verifyNoMoreInteractions(msProductApiClientMock, productMapperMock);
    }

    @Test
    void isRequiredDocumentsEnabled_returnsFalse() {
        // given
        String productId = "prod-test";
        String institutionType = "PA";
        String origin = "IPA";

        when(msProductApiClientMock._isRequiredDocumentsEnabled(productId, InstitutionType.PA, Origin.IPA))
                .thenReturn(ResponseEntity.ok(Boolean.FALSE));

        // when
        boolean result = productMsConnector.isRequiredDocumentsEnabled(productId, institutionType, origin);

        // then
        assertFalse(result);

        verify(msProductApiClientMock, times(1))._isRequiredDocumentsEnabled(productId, InstitutionType.PA, Origin.IPA);
    }

    @Test
    void isRequiredDocumentsEnabled_nullBodyReturnsFalse() {
        // given
        String productId = "prod-test";
        String institutionType = "PA";
        String origin = "IPA";

        when(msProductApiClientMock._isRequiredDocumentsEnabled(productId, InstitutionType.PA, Origin.IPA))
                .thenReturn(ResponseEntity.ok(null));

        // when
        boolean result = productMsConnector.isRequiredDocumentsEnabled(productId, institutionType, origin);

        // then
        assertFalse(result);

        verify(msProductApiClientMock, times(1))._isRequiredDocumentsEnabled(productId, InstitutionType.PA, Origin.IPA);
    }

}
