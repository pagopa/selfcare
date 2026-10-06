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
import org.jboss.logmanager.ExtHandler;
import org.jboss.logmanager.ExtLogRecord;
import org.jboss.logmanager.LogContext;
import org.jboss.logmanager.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.openapi.quarkus.product_json.api.ProductApi;
import org.openapi.quarkus.product_json.model.InstitutionType;
import org.openapi.quarkus.product_json.model.Origin;
import org.openapi.quarkus.product_json.model.ProductResponse;
import org.openapi.quarkus.product_json.model.ProductExpirationResponse;
import org.openapi.quarkus.product_json.model.RequiredDocumentResponse;
import org.openapi.quarkus.product_json.model.WorkflowType;
import org.openapi.quarkus.product_json.model.WorkflowTypeResponse;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@QuarkusTest
class ProductServiceImplTest {

    private final Logger serviceLogger = LogContext.getLogContext().getLogger(ProductServiceImpl.class.getName());
    private final List<String> logMessages = new ArrayList<>();
    private final ExtHandler logHandler = new ExtHandler() {
        @Override
        protected void doPublish(ExtLogRecord record) {
            logMessages.add(record.getFormattedMessage());
        }
    };

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
        serviceLogger.addHandler(logHandler);
    }

    @AfterEach
    void clearTenant() {
        serviceLogger.removeHandler(logHandler);
        logHandler.close();
        tenantContext.clear();
    }

    @ParameterizedTest
    @MethodSource("logValues")
    void productLookups_shouldEscapeOnlyLoggedValues(String productId, String loggedProductId) {
        // Given
        ProductResponse expected = new ProductResponse();
        when(productApi.getProductById(productId, "AR")).thenReturn(Uni.createFrom().item(expected));
        when(productApi.getValidProductById(productId, "AR")).thenReturn(Uni.createFrom().item(expected));
        when(productApi.getProductExpirationDays(productId, "AR"))
                .thenReturn(Uni.createFrom().item(new ProductExpirationResponse().expirationDays(45)));

        // When
        ProductResponse product = productService.getProduct(productId).await().indefinitely();
        ProductResponse validProduct = productService.getValidProduct(productId).await().indefinitely();
        ProductResponse explicitTenantProduct = productService.getValidProduct(productId, " ar ")
                .await().indefinitely();
        Integer expirationDays = productService.getProductExpirationDays(productId).await().indefinitely();

        // Then
        assertSame(expected, product);
        assertSame(expected, validProduct);
        assertSame(expected, explicitTenantProduct);
        assertEquals(45, expirationDays);
        assertEquals(List.of(
                "Calling getProductById: productId=" + loggedProductId,
                "Calling getValidProductById: productId=" + loggedProductId,
                "Calling getValidProductById: productId=" + loggedProductId + ", tenantId=null",
                "Calling getValidProductById: productId=" + loggedProductId + ", tenantId= ar ",
                "Calling getProductExpirationDays: productId=" + loggedProductId), logMessages);
        assertSingleLineMessages();
        verify(productApi).getProductById(productId, "AR");
        verify(productApi, times(2)).getValidProductById(productId, "AR");
        verify(productApi).getProductExpirationDays(productId, "AR");
        verifyNoMoreInteractions(productApi);
    }

    @ParameterizedTest
    @ValueSource(strings = {"A\rR", "A\nR", "A\r\nR", "A\u0085R", "A\u2028R", "A\u2029R"})
    void getValidProduct_shouldEscapeLoggedTenantWithoutAcceptingIt(String tenantId) {
        // Given
        String productId = "prod-io";

        // When
        assertThrows(UnknownTenantException.class, () -> productService.getValidProduct(productId, tenantId));

        // Then
        assertEquals(1, logMessages.size());
        assertTrue(logMessages.get(0).startsWith(
                "Calling getValidProductById: productId=prod-io, tenantId=A\\"));
        assertSingleLineMessages();
        assertEquals("AR", tenantContext.getTenantId());
        verifyNoInteractions(productApi);
    }

    @Test
    void getValidProduct_shouldRejectConflictingTenantWithoutCallingApi() {
        // Given
        String tenantId = "PNPG";

        // When
        assertThrows(IllegalArgumentException.class, () -> productService.getValidProduct("prod-io", tenantId));

        // Then
        assertEquals(List.of("Calling getValidProductById: productId=prod-io, tenantId=PNPG"), logMessages);
        assertEquals("AR", tenantContext.getTenantId());
        verifyNoInteractions(productApi);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " "})
    void getValidProduct_shouldRejectBlankTenantWithoutCallingApi(String tenantId) {
        // Given
        String productId = "prod-io";

        // When
        assertThrows(IllegalArgumentException.class, () -> productService.getValidProduct(productId, tenantId));

        // Then
        assertEquals(List.of("Calling getValidProductById: productId=prod-io, tenantId=" + tenantId), logMessages);
        assertEquals("AR", tenantContext.getTenantId());
        verifyNoInteractions(productApi);
    }

    private static Stream<Arguments> logValues() {
        return Stream.of(
                Arguments.of(null, "null"),
                Arguments.of("", ""),
                Arguments.of("prod-io", "prod-io"),
                Arguments.of("prod-\\nio", "prod-\\\\nio"),
                Arguments.of("prod-\rio", "prod-\\rio"),
                Arguments.of("prod-\nio", "prod-\\nio"),
                Arguments.of("prod-\r\nio", "prod-\\r\\nio"),
                Arguments.of("prod-\u0085io", "prod-\\205io"),
                Arguments.of("prod-\u2028io", "prod-\\u2028io"),
                Arguments.of("prod-\u2029io", "prod-\\u2029io"));
    }

    private void assertSingleLineMessages() {
        assertTrue(logMessages.stream().allMatch(message -> message.codePoints()
                .noneMatch(c -> c == '\r' || c == '\n' || c == 0x85 || c == 0x2028 || c == 0x2029)));
    }

    @Test
    void getWorkflowType_shouldReturnWorkflowTypeResponse() {
        // Given
        InstitutionType institutionType = InstitutionType.PA;
        Origin origin = Origin.IPA;
        ProductId productId = ProductId.PROD_IO;

        WorkflowTypeResponse expected = new WorkflowTypeResponse();
        expected.setWorkflowType(WorkflowType.CONTRACT_REGISTRATION);

        when(productApi.getWorkflowType(institutionType, origin, productId.getValue(), "AR"))
                .thenReturn(Uni.createFrom().item(expected));

        // When
        WorkflowTypeResponse result = productService
                .getWorkflowType(institutionType, origin, productId)
                .await().indefinitely();

        // Then
        assertNotNull(result);
        assertEquals(WorkflowType.CONTRACT_REGISTRATION, result.getWorkflowType());
        verify(productApi).getWorkflowType(institutionType, origin, productId.getValue(), "AR");
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

        when(productApi.getRequiredDocuments(productId.getValue(), institutionType, origin, "AR"))
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
        verify(productApi).getRequiredDocuments(productId.getValue(), institutionType, origin, "AR");
        verifyNoMoreInteractions(productApi);
    }

    @Test
    void isRequiredDocuments_shouldReturnBooleanFromHeader() {
        // Given
        ProductId productId = ProductId.PROD_IO;
        InstitutionType institutionType = InstitutionType.PA;
        Origin origin = Origin.IPA;

        Response expectedResponse = Response.ok().header("X-Required-Documents-Enabled", "true").build();

        when(productApi.isRequiredDocumentsEnabled(productId.getValue(), institutionType, origin, "AR"))
                .thenReturn(Uni.createFrom().item(expectedResponse));

        // When
        Boolean result = productService
                .isRequiredDocuments(productId, institutionType, origin)
                .await().indefinitely();

        // Then
        assertNotNull(result);
        assertEquals(Boolean.TRUE, result);
        verify(productApi).isRequiredDocumentsEnabled(productId.getValue(), institutionType, origin, "AR");
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
        when(productApi.isRequiredDocumentsEnabled("prod-io", InstitutionType.PA, Origin.IPA, "AR"))
                .thenReturn(Uni.createFrom().item(Response.ok().build()));

        assertFalse(productService.isRequiredDocuments(ProductId.PROD_IO, InstitutionType.PA, Origin.IPA, "AR")
                .await().indefinitely());
    }
}
