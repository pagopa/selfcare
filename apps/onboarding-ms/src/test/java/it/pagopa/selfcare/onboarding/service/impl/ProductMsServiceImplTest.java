package it.pagopa.selfcare.onboarding.service.impl;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.onboarding.common.ProductId;
import it.pagopa.selfcare.onboarding.service.ProductMsService;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.Test;
import org.openapi.quarkus.product_json.api.ProductApi;
import org.openapi.quarkus.product_json.model.InstitutionType;
import org.openapi.quarkus.product_json.model.Origin;
import org.openapi.quarkus.product_json.model.RequiredDocumentResponse;
import org.openapi.quarkus.product_json.model.WorkflowType;
import org.openapi.quarkus.product_json.model.WorkflowTypeResponse;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

@QuarkusTest
class ProductMsServiceImplTest {

    @Inject
    ProductMsService productMsService;

    @InjectMock
    @RestClient
    @Inject
    ProductApi productApi;

    @Test
    void getWorkflowType_shouldReturnWorkflowTypeResponse() {
        // Given
        InstitutionType institutionType = InstitutionType.PA;
        Origin origin = Origin.IPA;
        ProductId productId = ProductId.PROD_IO;

        WorkflowTypeResponse expected = new WorkflowTypeResponse();
        expected.setWorkflowType(WorkflowType.CONTRACT_REGISTRATION);

        when(productApi.getWorkflowType(institutionType, origin, productId.getValue()))
                .thenReturn(Uni.createFrom().item(expected));

        // When
        WorkflowTypeResponse result = productMsService
                .getWorkflowType(institutionType, origin, productId)
                .await().indefinitely();

        // Then
        assertNotNull(result);
        assertEquals(WorkflowType.CONTRACT_REGISTRATION, result.getWorkflowType());
        verify(productApi).getWorkflowType(institutionType, origin, productId.getValue());
        verifyNoMoreInteractions(productApi);
    }

    @Test
    void getRequiredDocuments_shouldReturnDocumentList() {
        // Given
        ProductId productId = ProductId.PROD_IO;
        InstitutionType institutionType = InstitutionType.PA;
        Origin origin = Origin.IPA;

        RequiredDocumentResponse doc = new RequiredDocumentResponse();
        doc.setId("doc-1");
        doc.setName("Atto costitutivo");
        doc.setRequired(true);

        when(productApi.getRequiredDocuments(productId.getValue(), institutionType, origin))
                .thenReturn(Uni.createFrom().item(List.of(doc)));

        // When
        List<RequiredDocumentResponse> result = productMsService
                .getRequiredDocuments(productId, institutionType, origin)
                .await().indefinitely();

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("doc-1", result.get(0).getId());
        assertEquals("Atto costitutivo", result.get(0).getName());
        verify(productApi).getRequiredDocuments(productId.getValue(), institutionType, origin);
        verifyNoMoreInteractions(productApi);
    }

    @Test
    void isRequiredDocumentsEnabled_shouldReturnResponse() {
        // Given
        ProductId productId = ProductId.PROD_IO;
        InstitutionType institutionType = InstitutionType.PA;
        Origin origin = Origin.IPA;

        Response expectedResponse = Response.ok().header("X-Required-Documents-Enabled", "true").build();

        when(productApi.isRequiredDocumentsEnabled(productId.getValue(), institutionType, origin))
                .thenReturn(Uni.createFrom().item(expectedResponse));

        // When
        Response result = productMsService
                .isRequiredDocumentsEnabled(productId, institutionType, origin)
                .await().indefinitely();

        // Then
        assertNotNull(result);
        assertEquals(200, result.getStatus());
        verify(productApi).isRequiredDocumentsEnabled(productId.getValue(), institutionType, origin);
        verifyNoMoreInteractions(productApi);
    }
}

