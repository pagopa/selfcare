package it.pagopa.selfcare.onboarding.connector;

import it.pagopa.selfcare.document.generated.openapi.v1.dto.DocumentBuilderRequest;
import it.pagopa.selfcare.onboarding.connector.model.onboarding.InstitutionUpdate;
import it.pagopa.selfcare.onboarding.connector.model.onboarding.OnboardingData;
import it.pagopa.selfcare.onboarding.connector.rest.client.MsDocumentApiClient;
import it.pagopa.selfcare.onboarding.connector.rest.client.MsDocumentContentApiClient;
import it.pagopa.selfcare.product.entity.AttachmentTemplate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ContextConfiguration;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ContextConfiguration(classes = {DocumentMsConnectorImpl.class})
@ExtendWith(MockitoExtension.class)
class DocumentMsConnectorImplTest {

    @Mock
    private MsDocumentApiClient msDocumentApiClient;

    @Mock
    private MsDocumentContentApiClient msDocumentContentApiClient;

    @InjectMocks
    private DocumentMsConnectorImpl documentMsConnector;

    @Test
    void getContract() {
        // given
        final String onboardingId = "onboardingId";
        Resource resource = Mockito.mock(Resource.class);
        when(msDocumentContentApiClient._getContract(onboardingId))
                .thenReturn(ResponseEntity.of(Optional.of(resource)));
        // when
        final Executable executable = () -> documentMsConnector.getContract(onboardingId);
        // then
        assertDoesNotThrow(executable);
        verify(msDocumentContentApiClient, times(1))
                ._getContract(onboardingId);
        verifyNoMoreInteractions(msDocumentContentApiClient);
    }

    @Test
    void getAttachment() {
        // given
        final String onboardingId = "onboardingId";
        final String filename = "filename";
        Resource resource = Mockito.mock(Resource.class);
        when(msDocumentContentApiClient._getAttachment(onboardingId, filename))
                .thenReturn(ResponseEntity.of(Optional.of(resource)));
        // when
        final Executable executable = () -> documentMsConnector.getAttachment(onboardingId, filename);
        // then
        assertDoesNotThrow(executable);
        verify(msDocumentContentApiClient, times(1))
                ._getAttachment(onboardingId, filename);
        verifyNoMoreInteractions(msDocumentContentApiClient);
    }

    @Test
    void getTemplateAttachment() {
        // given
        final String onboardingId = "onboardingId";
        final String filename = "filename";
        final String templatePath = "templatePath";
        final OnboardingData onboarding = new OnboardingData();
        onboarding.setId(onboardingId);
        onboarding.setInstitutionUpdate(new InstitutionUpdate());
        onboarding.getInstitutionUpdate().setDescription("description");
        onboarding.setProductId("productId");
        Resource resource = Mockito.mock(Resource.class);
        when(msDocumentContentApiClient._getTemplateAttachment(
                onboardingId,
                onboarding.getInstitutionUpdate().getDescription(),
                filename,
                onboarding.getProductId(),
                templatePath))
            .thenReturn(ResponseEntity.of(Optional.of(resource)));
        // when
        final Executable executable = () -> documentMsConnector.getTemplateAttachment(onboarding, filename, templatePath);
        // then
        assertDoesNotThrow(executable);
        verify(msDocumentContentApiClient, times(1))
                ._getTemplateAttachment(onboardingId,
                        onboarding.getInstitutionUpdate().getDescription(),
                        filename,
                        onboarding.getProductId(),
                        templatePath);
        verifyNoMoreInteractions(msDocumentContentApiClient);
    }

    @Test
    void headAttachment() {
        // given
        final String onboardingId = "onboardingId";
        final String filename = "filename";

        when(msDocumentApiClient._headAttachment(onboardingId, filename))
                .thenReturn(ResponseEntity.status(HttpStatusCode.valueOf(204)).build());
        // when
        HttpStatusCode result = documentMsConnector.headAttachment(onboardingId, filename);

        // then
        verify(msDocumentApiClient, times(1))
                ._headAttachment(onboardingId, filename);
        verifyNoMoreInteractions(msDocumentApiClient);
        assertEquals(result, HttpStatusCode.valueOf(204));
    }

    @Test
    void headAttachment_shouldReturn404_whenNotFound() {
        // given
        final String onboardingId = "onboardingId";
        final String filename = "filename";

        when(msDocumentApiClient._headAttachment(onboardingId, filename))
                .thenReturn(ResponseEntity.status(HttpStatusCode.valueOf(404)).build());
        // when
        HttpStatusCode result = documentMsConnector.headAttachment(onboardingId, filename);

        // then
        verify(msDocumentApiClient, times(1))
                ._headAttachment(onboardingId, filename);
        verifyNoMoreInteractions(msDocumentApiClient);
        assertEquals(result, HttpStatusCode.valueOf(404));
    }

    @Test
    void uploadAttachment() {
        // given
        final String onboardingId = "onboardingId";
        final String filename = "filename";
        MockMultipartFile file = new MockMultipartFile("file", "content".getBytes());
        final String productId = "productId";
        final AttachmentTemplate template = new AttachmentTemplate();
        template.setTemplatePath("templatePath");
        template.setTemplateVersion("templateVersion");

        when(msDocumentContentApiClient._uploadAttachment(eq(file), any(DocumentBuilderRequest.class)))
                .thenReturn(ResponseEntity.ok().build());

        // when
        documentMsConnector.uploadAttachment(onboardingId, file, filename, productId, template);

        // then
        verify(msDocumentContentApiClient, times(1))
                ._uploadAttachment(eq(file), any(DocumentBuilderRequest.class));
        verifyNoMoreInteractions(msDocumentContentApiClient);
    }

    @Test
    void getAggregatesCsv() {
        // given
        final String onboardingId = "onboardingId";
        final String productId = "productId";
        Resource resource = Mockito.mock(Resource.class);
        when(msDocumentContentApiClient._getAggregatesCsv(onboardingId, productId))
                .thenReturn(ResponseEntity.of(Optional.of(resource)));
        // when
        final Executable executable = () -> documentMsConnector.getAggregatesCsv(onboardingId, productId);
        // then
        assertDoesNotThrow(executable);
        verify(msDocumentContentApiClient, times(1))
                ._getAggregatesCsv(onboardingId, productId);
        verifyNoMoreInteractions(msDocumentContentApiClient);
    }
}
