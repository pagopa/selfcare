package it.pagopa.selfcare.onboarding.service.impl;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.onboarding.common.ProductId;
import it.pagopa.selfcare.onboarding.exception.ResourceNotFoundException;
import it.pagopa.selfcare.onboarding.service.ProductService;
import it.pagopa.selfcare.tenant.TenantContext;
import it.pagopa.selfcare.tenant.UnknownTenantException;
import it.pagopa.selfcare.tenant.UnresolvedTenantException;
import jakarta.inject.Inject;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.openapi.quarkus.product_json.api.ProductApi;
import org.openapi.quarkus.product_json.model.InstitutionType;
import org.openapi.quarkus.product_json.model.Origin;
import org.openapi.quarkus.product_json.model.ProductResponse;
import org.openapi.quarkus.product_json.model.ProductExpirationResponse;
import org.openapi.quarkus.product_json.model.RequiredDocumentResponse;
import org.openapi.quarkus.product_json.model.WorkflowType;
import org.openapi.quarkus.product_json.model.WorkflowTypeResponse;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@QuarkusTest
class ProductServiceImplTest {

    @Inject
    ProductService productService;

    @Inject
    TenantContext tenantContext;

    @InjectMock
    @RestClient
    @Inject
    ProductApi productApi;

    @BeforeEach
    void initializeTenant() {
        tenantContext.setTenantId("AR");
    }

    @AfterEach
    void clearTenant() {
        tenantContext.clear();
    }

    @Test
    void getWorkflowType_shouldReturnWorkflowTypeResponse() {
        // Given
        InstitutionType institutionType = InstitutionType.PA;
        Origin origin = Origin.IPA;
        ProductId productId = ProductId.PROD_IO;

        WorkflowTypeResponse expected = new WorkflowTypeResponse();
        expected.setWorkflowType(WorkflowType.CONTRACT_REGISTRATION);

        when(productApi.getWorkflowType("AR", institutionType, origin, productId.getValue()))
                .thenReturn(Uni.createFrom().item(expected));

        // When
        WorkflowTypeResponse result = productService
                .getWorkflowType(institutionType, origin, productId)
                .await().indefinitely();

        // Then
        assertNotNull(result);
        assertEquals(WorkflowType.CONTRACT_REGISTRATION, result.getWorkflowType());
        verify(productApi).getWorkflowType("AR", institutionType, origin, productId.getValue());
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

        when(productApi.getRequiredDocuments(productId.getValue(), "AR", institutionType, origin))
                .thenReturn(Uni.createFrom().item(List.of(doc)));

        // When
        List<RequiredDocumentResponse> result = productService
                .getRequiredDocuments(productId, institutionType, origin)
                .await().indefinitely();

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("doc-1", result.get(0).getId());
        assertEquals("Atto costitutivo", result.get(0).getName());
        verify(productApi).getRequiredDocuments(productId.getValue(), "AR", institutionType, origin);
        verifyNoMoreInteractions(productApi);
    }

    @Test
    void isRequiredDocuments_shouldReturnBooleanFromHeader() {
        // Given
        ProductId productId = ProductId.PROD_IO;
        InstitutionType institutionType = InstitutionType.PA;
        Origin origin = Origin.IPA;

        Response expectedResponse = Response.ok().header("X-Required-Documents-Enabled", "true").build();

        when(productApi.isRequiredDocumentsEnabled(productId.getValue(), "AR", institutionType, origin))
                .thenReturn(Uni.createFrom().item(expectedResponse));

        // When
        Boolean result = productService
                .isRequiredDocuments(productId, institutionType, origin)
                .await().indefinitely();

        // Then
        assertNotNull(result);
        assertEquals(Boolean.TRUE, result);
        verify(productApi).isRequiredDocumentsEnabled(productId.getValue(), "AR", institutionType, origin);
        verifyNoMoreInteractions(productApi);
    }

    @Test
    void getValidProduct_shouldInitializeCanonicalTenantFromExplicitParameter() {
        tenantContext.clear();
        ProductResponse expected = new ProductResponse();
        expected.setProductId("prod-io");
        when(productApi.getValidProductById("prod-io", "PNPG"))
                .thenReturn(Uni.createFrom().item(expected));

        ProductResponse result = productService.getValidProduct("prod-io", " pnpg ")
                .await().indefinitely();

        assertEquals(expected, result);
        assertEquals("PNPG", tenantContext.getTenantId());
        verify(productApi).getValidProductById("prod-io", "PNPG");
    }

    @Test
    void getProduct_shouldNormalizeContextTenant() {
        tenantContext.setTenantId(" ar ");
        ProductResponse expected = new ProductResponse().productId("prod-io").tenantId("AR");
        when(productApi.getProductById("prod-io", "AR")).thenReturn(Uni.createFrom().item(expected));

        assertSame(expected, productService.getProduct("prod-io").await().indefinitely());
        assertEquals("AR", tenantContext.getTenantId());
    }

    @Test
    void getProduct_shouldAcceptEquivalentExplicitTenant() {
        when(productApi.getProductById("prod-io", "AR")).thenReturn(Uni.createFrom().item(new ProductResponse()));

        productService.getProduct("prod-io", " ar ").await().indefinitely();

        verify(productApi).getProductById("prod-io", "AR");
    }

    @Test
    void getProduct_shouldRejectConflictingTenantBeforeCallingApi() {
        assertThrows(IllegalArgumentException.class, () -> productService.getProduct("prod-io", "PNPG"));
        assertEquals("AR", tenantContext.getTenantId());
        verifyNoInteractions(productApi);
    }

    @Test
    void getProduct_shouldRejectMissingTenantBeforeCallingApi() {
        tenantContext.clear();
        assertThrows(UnresolvedTenantException.class, () -> productService.getProduct("prod-io"));
        verifyNoInteractions(productApi);
    }

    @Test
    void getProduct_shouldRejectUnknownTenantBeforeCallingApi() {
        assertThrows(UnknownTenantException.class, () -> productService.getProduct("prod-io", "unknown"));
        verifyNoInteractions(productApi);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " "})
    void getProduct_shouldRejectBlankExplicitTenant(String tenant) {
        assertThrows(IllegalArgumentException.class, () -> productService.getProduct("prod-io", tenant));
        verifyNoInteractions(productApi);
    }

    @Test
    void getProduct_shouldTranslateNotFound() {
        when(productApi.getProductById("prod-io", "AR"))
                .thenReturn(Uni.createFrom().failure(new WebApplicationException(Response.Status.NOT_FOUND)));

        assertThrows(ResourceNotFoundException.class,
                () -> productService.getProduct("prod-io").await().indefinitely());
    }

    @ParameterizedTest
    @ValueSource(ints = {401, 403, 500, 503})
    void getValidProduct_shouldPreserveRemoteFailures(int status) {
        WebApplicationException failure = new WebApplicationException(status);
        when(productApi.getValidProductById("prod-io", "AR")).thenReturn(Uni.createFrom().failure(failure));

        assertSame(failure, assertThrows(WebApplicationException.class,
                () -> productService.getValidProduct("prod-io").await().indefinitely()));
    }

    @Test
    void getProduct_shouldPreserveTimeout() {
        ProcessingException failure = new ProcessingException("read timed out");
        when(productApi.getProductById("prod-io", "AR")).thenReturn(Uni.createFrom().failure(failure));

        assertSame(failure, assertThrows(ProcessingException.class,
                () -> productService.getProduct("prod-io").await().indefinitely()));
    }

    @Test
    void expirationDays_shouldUseConfiguredValue() {
        ProductExpirationResponse response = new ProductExpirationResponse().expirationDays(45);
        when(productApi.getProductExpirationDays("prod-io", "AR")).thenReturn(Uni.createFrom().item(response));

        assertEquals(45, productService.getProductExpirationDays("prod-io", " ar ").await().indefinitely());
    }

    @Test
    void expirationDays_shouldKeepDefaultForMissingValue() {
        when(productApi.getProductExpirationDays("prod-io", "AR"))
                .thenReturn(Uni.createFrom().item(new ProductExpirationResponse()));

        assertEquals(30, productService.getProductExpirationDays("prod-io").await().indefinitely());
    }

    @Test
    void isRequiredDocuments_shouldKeepFalseForMissingHeader() {
        when(productApi.isRequiredDocumentsEnabled("prod-io", "AR", InstitutionType.PA, Origin.IPA))
                .thenReturn(Uni.createFrom().item(Response.ok().build()));

        assertFalse(productService.isRequiredDocuments(ProductId.PROD_IO, InstitutionType.PA, Origin.IPA, "AR")
                .await().indefinitely());
    }
}
