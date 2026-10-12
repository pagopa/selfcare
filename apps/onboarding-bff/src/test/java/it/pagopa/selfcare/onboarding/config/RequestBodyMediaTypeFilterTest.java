package it.pagopa.selfcare.onboarding.config;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import it.pagopa.selfcare.onboarding.controller.InstitutionV2Controller;
import it.pagopa.selfcare.onboarding.controller.TokenV2Controller;
import it.pagopa.selfcare.onboarding.controller.UserController;
import it.pagopa.selfcare.onboarding.model.dto.request.UserTaxCodeDto;
import jakarta.ws.rs.NotSupportedException;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ResourceInfo;
import jakarta.ws.rs.ext.ReaderInterceptorContext;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.jboss.resteasy.reactive.multipart.FileUpload;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@ExtendWith(MockitoExtension.class)
class RequestBodyMediaTypeFilterTest {

    @Mock
    ContainerRequestContext request;
    @Mock
    ResourceInfo resourceInfo;
    @Mock
    ReaderInterceptorContext reader;
    @InjectMocks
    RequestBodyMediaTypeFilter filter;

    @ParameterizedTest
    @ValueSource(strings = {"{}", "not-json", "\u0000", " "})
    void aPresentJsonBodyWithoutContentTypeIsRejectedBeforeParsing(String body) throws Exception {
        // given
        Mockito.<Class<?>>when(resourceInfo.getResourceClass()).thenReturn(UserController.class);
        when(resourceInfo.getResourceMethod()).thenReturn(UserController.class.getMethod("searchUser", UserTaxCodeDto.class));
        when(reader.getProperty(RequestBodyMediaTypeFilter.BODY_WITHOUT_CONTENT_TYPE)).thenReturn(true);
        when(reader.getInputStream()).thenReturn(new ByteArrayInputStream(body.getBytes(StandardCharsets.UTF_8)));

        // when / then
        filter.filter(request);
        verify(request).setProperty(RequestBodyMediaTypeFilter.BODY_WITHOUT_CONTENT_TYPE, true);
        assertThrows(NotSupportedException.class, () -> filter.aroundReadFrom(reader));
        verify(reader, never()).proceed();
    }

    @Test
    void theFilterOnlyMarksJsonBodiesWithoutReadingAnUninitializedStream() throws NoSuchMethodException {
        // given
        Mockito.<Class<?>>when(resourceInfo.getResourceClass()).thenReturn(UserController.class);
        when(resourceInfo.getResourceMethod()).thenReturn(UserController.class.getMethod("searchUser", UserTaxCodeDto.class));

        // when / then
        assertDoesNotThrow(() -> filter.filter(request));
        verify(request).setProperty(RequestBodyMediaTypeFilter.BODY_WITHOUT_CONTENT_TYPE, true);
        verify(request, never()).getEntityStream();
        verify(request, never()).hasEntity();
        verify(request, never()).getLength();
    }

    @Test
    void aMissingJsonBodyStillReachesTheRequiredBodyGuard() throws Exception {
        // given
        Mockito.<Class<?>>when(resourceInfo.getResourceClass()).thenReturn(UserController.class);
        when(resourceInfo.getResourceMethod()).thenReturn(UserController.class.getMethod("searchUser", UserTaxCodeDto.class));
        when(reader.getProperty(RequestBodyMediaTypeFilter.BODY_WITHOUT_CONTENT_TYPE)).thenReturn(true);
        when(reader.getInputStream()).thenReturn(new ByteArrayInputStream(new byte[0]));
        UserTaxCodeDto decoded = new UserTaxCodeDto();
        when(reader.proceed()).thenReturn(decoded);

        // when / then
        assertDoesNotThrow(() -> filter.filter(request));
        assertSame(decoded, filter.aroundReadFrom(reader));
    }

    @Test
    void anUnmarkedReaderKeepsItsInputStreamUntouched() throws IOException {
        UserTaxCodeDto decoded = new UserTaxCodeDto();
        when(reader.proceed()).thenReturn(decoded);

        assertSame(decoded, filter.aroundReadFrom(reader));
        verify(reader, never()).getInputStream();
    }

    @Test
    void anEntityReadFailureIsNotHidden() throws IOException {
        InputStream stream = Mockito.mock(InputStream.class);
        IOException failure = new IOException("entity read failed");
        when(reader.getProperty(RequestBodyMediaTypeFilter.BODY_WITHOUT_CONTENT_TYPE)).thenReturn(true);
        when(reader.getInputStream()).thenReturn(stream);
        when(stream.read()).thenThrow(failure);

        assertSame(failure, assertThrows(IOException.class, () -> filter.aroundReadFrom(reader)));
        verify(reader, never()).proceed();
    }

    @Test
    void anExplicitVendorJsonHeaderIsLeftToNativeBinding() {
        // given
        Mockito.<Class<?>>when(resourceInfo.getResourceClass()).thenReturn(UserController.class);
        when(request.getHeaderString("Content-Type")).thenReturn("application/vnd.api+json");

        // when / then
        assertDoesNotThrow(() -> filter.filter(request));
        verifyNoInteractions(reader);
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
