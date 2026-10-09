package it.pagopa.selfcare.onboarding.config;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import it.pagopa.selfcare.onboarding.controller.InstitutionV2Controller;
import it.pagopa.selfcare.onboarding.controller.TokenV2Controller;
import it.pagopa.selfcare.onboarding.controller.UserController;
import it.pagopa.selfcare.onboarding.model.dto.request.UserTaxCodeDto;
import jakarta.ws.rs.NotSupportedException;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ResourceInfo;
import org.jboss.resteasy.reactive.multipart.FileUpload;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RequestBodyMediaTypeFilterTest {

    @Mock
    ContainerRequestContext request;
    @Mock
    ResourceInfo resourceInfo;
    @InjectMocks
    RequestBodyMediaTypeFilter filter;

    @Test
    void aPresentJsonBodyWithoutContentTypeIsRejectedBeforeValidation() throws NoSuchMethodException {
        // given
        Mockito.<Class<?>>when(resourceInfo.getResourceClass()).thenReturn(UserController.class);
        when(resourceInfo.getResourceMethod()).thenReturn(UserController.class.getMethod("searchUser", UserTaxCodeDto.class));
        when(request.hasEntity()).thenReturn(true);

        // when / then
        assertThrows(NotSupportedException.class, () -> filter.filter(request));
    }

    @Test
    void contentLengthIdentifiesTheBodyBeforeRestInitializesItsInputStream() throws NoSuchMethodException {
        // given
        Mockito.<Class<?>>when(resourceInfo.getResourceClass()).thenReturn(UserController.class);
        when(resourceInfo.getResourceMethod()).thenReturn(UserController.class.getMethod("searchUser", UserTaxCodeDto.class));
        when(request.getLength()).thenReturn(2);

        // when / then
        assertThrows(NotSupportedException.class, () -> filter.filter(request));
    }

    @Test
    void aMissingJsonBodyStillReachesTheRequiredBodyGuard() throws NoSuchMethodException {
        // given
        Mockito.<Class<?>>when(resourceInfo.getResourceClass()).thenReturn(UserController.class);
        when(resourceInfo.getResourceMethod()).thenReturn(UserController.class.getMethod("searchUser", UserTaxCodeDto.class));

        // when / then
        assertDoesNotThrow(() -> filter.filter(request));
    }

    @Test
    void anExplicitVendorJsonHeaderIsLeftToNativeBinding() {
        // given
        Mockito.<Class<?>>when(resourceInfo.getResourceClass()).thenReturn(UserController.class);
        when(request.getHeaderString("Content-Type")).thenReturn("application/vnd.api+json");

        // when / then
        assertDoesNotThrow(() -> filter.filter(request));
    }

    @Test
    void aMultipartBodyWithoutContentTypeIsRejectedAtMappingTime() throws NoSuchMethodException {
        // given
        Mockito.<Class<?>>when(resourceInfo.getResourceClass()).thenReturn(InstitutionV2Controller.class);
        when(resourceInfo.getResourceMethod()).thenReturn(InstitutionV2Controller.class.getMethod(
                "verifyAggregatesCsv", FileUpload.class, String.class, String.class, String.class, String.class));

        // when / then
        assertThrows(NotSupportedException.class, () -> filter.filter(request));
    }

    @Test
    void aBodylessActionDoesNotStartConsumingTheRequestBody() throws NoSuchMethodException {
        // given
        Mockito.<Class<?>>when(resourceInfo.getResourceClass()).thenReturn(TokenV2Controller.class);
        when(resourceInfo.getResourceMethod()).thenReturn(TokenV2Controller.class.getMethod("approveOnboarding", String.class));

        // when / then
        assertDoesNotThrow(() -> filter.filter(request));
    }

    @Test
    void unrelatedResourcesKeepTheirNativeBinding() {
        // given
        Mockito.<Class<?>>when(resourceInfo.getResourceClass()).thenReturn(RequestBodyMediaTypeFilterTest.class);

        // when / then
        assertDoesNotThrow(() -> filter.filter(request));
    }
}
