package it.pagopa.selfcare.onboarding.controller;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.test.UniAssertSubscriber;
import it.pagopa.selfcare.onboarding.client.model.OriginResult;
import it.pagopa.selfcare.onboarding.client.model.RequiredDocumentModel;
import it.pagopa.selfcare.onboarding.exception.InvalidRequestException;
import it.pagopa.selfcare.onboarding.exception.ResourceNotFoundException;
import it.pagopa.selfcare.onboarding.mapper.InstitutionMapper;
import it.pagopa.selfcare.onboarding.model.dto.response.OriginResponse;
import it.pagopa.selfcare.onboarding.model.dto.response.RequiredDocumentsEnabledResource;
import it.pagopa.selfcare.onboarding.service.ProductService;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.UriInfo;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProductV2ControllerTest {

    @Mock
    ProductService productService;

    @Mock
    InstitutionMapper productMapper;

    ProductV2Controller controller;

    @BeforeEach
    void setup() {
        controller = new ProductV2Controller(productService, productMapper);
        controller.uriInfo = mock(UriInfo.class);
        when(controller.uriInfo.getQueryParameters()).thenReturn(new MultivaluedHashMap<>());
    }

    @Test
    void originsMapTheAsynchronousItemAndKeepTheTenant() {
        // given
        OriginResult origins = new OriginResult();
        OriginResponse response = new OriginResponse();
        when(productService.getOrigins("AR", "prod-test")).thenReturn(Uni.createFrom().item(origins));
        when(productMapper.toOriginResponse(origins)).thenReturn(response);

        // when
        OriginResponse result = controller.getOrigins("prod-test", "AR").await().indefinitely();

        // then
        assertSame(response, result);
        verify(productService).getOrigins("AR", "prod-test");
    }

    @Test
    void originsPropagateFailureWithoutMapping() {
        // given
        ResourceNotFoundException failure = new ResourceNotFoundException("missing product");
        when(productService.getOrigins("PNPG", "prod-test")).thenReturn(Uni.createFrom().failure(failure));

        // when
        UniAssertSubscriber<OriginResponse> result = controller.getOrigins("prod-test", "PNPG")
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        // then
        assertSame(failure, result.getFailure());
        verifyNoInteractions(productMapper);
    }

    @Test
    void originsRejectMissingProductBeforeCallingTheService() {
        // given
        String productId = null;

        // when
        assertThrows(InvalidRequestException.class, () -> controller.getOrigins(productId, "AR"));

        // then
        verifyNoInteractions(productService, productMapper);
    }

    @Test
    void originsRejectMissingTenantBeforeCallingTheService() {
        // given
        String tenant = " ";

        // when
        assertThrows(InvalidRequestException.class, () -> controller.getOrigins("prod-test", tenant));

        // then
        verifyNoInteractions(productService, productMapper);
    }

    @Test
    void enabledFlagWrapsTheAsynchronousBoolean() {
        // given
        when(productService.isRequiredDocumentsEnabled("AR", "prod-test", "PA", "IPA"))
                .thenReturn(Uni.createFrom().item(true));

        // when
        RequiredDocumentsEnabledResource result = controller.isRequiredDocumentsEnabled("prod-test", "PA", "IPA", "AR")
                .await().indefinitely();

        // then
        assertTrue(result.isRequiredDocumentsEnabled());
        verify(productService).isRequiredDocumentsEnabled("AR", "prod-test", "PA", "IPA");
    }

    @Test
    void enabledFlagPropagatesFailureInsteadOfReturningFalse() {
        // given
        ResourceNotFoundException failure = new ResourceNotFoundException("missing product");
        when(productService.isRequiredDocumentsEnabled("AR", "prod-test", "PA", "IPA"))
                .thenReturn(Uni.createFrom().failure(failure));

        // when
        UniAssertSubscriber<RequiredDocumentsEnabledResource> result = controller.isRequiredDocumentsEnabled("prod-test", "PA", "IPA", "AR")
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        // then
        assertSame(failure, result.getFailure());
    }

    @Test
    void enabledFlagRejectsMissingOriginBeforeCallingTheService() {
        // given
        String origin = null;

        // when
        assertThrows(InvalidRequestException.class,
                () -> controller.isRequiredDocumentsEnabled("prod-test", "PA", origin, "AR"));

        // then
        verifyNoInteractions(productService, productMapper);
    }

    @Test
    void requiredDocumentsKeepTheTenantAndAnEmptyResult() {
        // given
        when(productService.getRequiredDocuments("PNPG", "prod-test", "PA", "IPA"))
                .thenReturn(Uni.createFrom().item(List.of()));

        // when
        List<RequiredDocumentModel> result = controller.getRequiredDocuments("prod-test", "PA", "IPA", "PNPG")
                .await().indefinitely();

        // then
        assertEquals(List.of(), result);
        verify(productService).getRequiredDocuments("PNPG", "prod-test", "PA", "IPA");
    }

    @Test
    void requiredDocumentsPropagateTheOriginalFailure() {
        // given
        ResourceNotFoundException failure = new ResourceNotFoundException("missing product");
        when(productService.getRequiredDocuments("AR", "prod-test", "PA", "IPA"))
                .thenReturn(Uni.createFrom().failure(failure));

        // when
        UniAssertSubscriber<List<RequiredDocumentModel>> result = controller.getRequiredDocuments("prod-test", "PA", "IPA", "AR")
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        // then
        assertSame(failure, result.getFailure());
    }
}
