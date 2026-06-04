package it.pagopa.selfcare.onboarding.service.impl;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.onboarding.service.ProductMsService;
import jakarta.inject.Inject;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.Test;
import org.openapi.quarkus.product_json.api.ProductApi;
import org.openapi.quarkus.product_json.model.InstitutionType;
import org.openapi.quarkus.product_json.model.Origin;
import org.openapi.quarkus.product_json.model.WorkflowType;
import org.openapi.quarkus.product_json.model.WorkflowTypeResponse;

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
        String productId = "prod-io";

        WorkflowTypeResponse expected = new WorkflowTypeResponse();
        expected.setWorkflowType(WorkflowType.CONTRACT_REGISTRATION);

        when(productApi.getWorkflowType(institutionType, origin, productId))
                .thenReturn(Uni.createFrom().item(expected));

        // When
        WorkflowTypeResponse result = productMsService
                .getWorkflowType(institutionType, origin, productId)
                .await().indefinitely();

        // Then
        assertNotNull(result);
        assertEquals(WorkflowType.CONTRACT_REGISTRATION, result.getWorkflowType());
        verify(productApi).getWorkflowType(institutionType, origin, productId);
        verifyNoMoreInteractions(productApi);
    }
}


