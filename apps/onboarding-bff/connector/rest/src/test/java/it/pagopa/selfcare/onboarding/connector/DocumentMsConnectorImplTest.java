package it.pagopa.selfcare.onboarding.connector;

import it.pagopa.selfcare.document.generated.openapi.v1.dto.AvailableDocumentResponse;
import it.pagopa.selfcare.document.generated.openapi.v1.dto.AvailableDocumentsResponse;
import it.pagopa.selfcare.document.generated.openapi.v1.dto.DocumentBuilderRequest;
import it.pagopa.selfcare.document.generated.openapi.v1.dto.DocumentType;
import it.pagopa.selfcare.document.generated.openapi.v1.dto.UserAttachmentRequest;
import it.pagopa.selfcare.onboarding.connector.model.onboarding.AvailableDocument;
import it.pagopa.selfcare.onboarding.connector.model.onboarding.AvailableDocuments;
import it.pagopa.selfcare.onboarding.connector.model.onboarding.InstitutionUpdate;
import it.pagopa.selfcare.onboarding.connector.model.onboarding.OnboardingData;
import it.pagopa.selfcare.onboarding.connector.model.product.AttachmentTemplate;
import it.pagopa.selfcare.onboarding.connector.rest.client.MsDocumentApiClient;
import it.pagopa.selfcare.onboarding.connector.rest.client.MsDocumentContentApiClient;
import it.pagopa.selfcare.onboarding.connector.rest.mapper.DocumentMapper;
import it.pagopa.selfcare.onboarding.connector.rest.mapper.DocumentMapperImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ContextConfiguration;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
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

    @Spy
    private DocumentMapper documentMapper = new DocumentMapperImpl();

    @InjectMocks
    private DocumentMsConnectorImpl documentMsConnector;

    @Test
    void getAvailableDocuments_shouldMapDocuments() {
        // given
        final String onboardingId = "onboardingId";
        AvailableDocumentResponse institutionContract = new AvailableDocumentResponse();
        institutionContract.setName("signed_contratto_adesione.pdf.p7m");
        institutionContract.setType(DocumentType.INSTITUTION);
        institutionContract.setOnboardingId(onboardingId);
        institutionContract.setRootOnboardingId(onboardingId);
        AvailableDocumentResponse attachment = new AvailableDocumentResponse();
        attachment.setName("Allegato tecnico");
        attachment.setType(DocumentType.ATTACHMENT);
        attachment.setOnboardingId(onboardingId);
        attachment.setRootOnboardingId(onboardingId);
        AvailableDocumentsResponse response = new AvailableDocumentsResponse();
        response.setDocuments(List.of(institutionContract, attachment));
        when(msDocumentApiClient._getAvailableDocuments(onboardingId))
                .thenReturn(ResponseEntity.ok(response));

        // when
        AvailableDocuments result = documentMsConnector.getAvailableDocuments(onboardingId);

        // then
        assertEquals(2, result.getDocuments().size());
        AvailableDocument first = result.getDocuments().get(0);
        assertEquals("signed_contratto_adesione.pdf.p7m", first.getName());
        assertEquals("INSTITUTION", first.getType());
        assertEquals(onboardingId, first.getOnboardingId());
        assertEquals(onboardingId, first.getRootOnboardingId());
        assertEquals("Allegato tecnico", result.getDocuments().get(1).getName());
        assertEquals("ATTACHMENT", result.getDocuments().get(1).getType());
        verify(msDocumentApiClient, times(1))._getAvailableDocuments(onboardingId);
    }

    @Test
    void getAvailableDocuments_shouldReturnEmptyDocuments_whenNoneAvailable() {
        // given
        final String onboardingId = "onboardingId";
        when(msDocumentApiClient._getAvailableDocuments(onboardingId))
                .thenReturn(ResponseEntity.ok(new AvailableDocumentsResponse()));

        // when
        AvailableDocuments result = documentMsConnector.getAvailableDocuments(onboardingId);

        // then
        assertTrue(result.getDocuments() == null || result.getDocuments().isEmpty());
    }

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
    void getContractSigned() {
      //given
      final String onboardingId = "onboardingId";
      Resource resource = Mockito.mock(Resource.class);
      when(msDocumentContentApiClient._getContractSigned(onboardingId))
              .thenReturn(ResponseEntity.of(Optional.of(resource)));
      // when
      final Executable executable = () -> documentMsConnector.getContractSigned(onboardingId);
      // then
      assertDoesNotThrow(executable);
      verify(msDocumentContentApiClient, times(1))
              ._getContractSigned(onboardingId);
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
    void uploadUserAttachment() {
        // given
        final String onboardingId = "onboardingId";
        final String productId = "productId";
        final String attachmentId = "attachmentId";
        final String attachmentDescription = "attachmentDescription";
        final String attachmentName = "attachmentName";
        final Integer maxDocumentsRequired = 3;
        MockMultipartFile file = new MockMultipartFile("file", "content".getBytes());

        when(msDocumentContentApiClient._uploadUserAttachment(eq(file), any(UserAttachmentRequest.class)))
                .thenReturn(ResponseEntity.ok().build());

        // when
        documentMsConnector.uploadUserAttachment(onboardingId, file, productId, attachmentId,
                attachmentDescription, attachmentName, maxDocumentsRequired);

        // then
        verify(msDocumentContentApiClient, times(1))
                ._uploadUserAttachment(eq(file), argThat(req ->
                        onboardingId.equals(req.getOnboardingId())
                                && productId.equals(req.getProductId())
                                && attachmentId.equals(req.getAttachmentId())
                                && attachmentDescription.equals(req.getAttachmentDescription())
                                && attachmentName.equals(req.getAttachmentName())
                                && maxDocumentsRequired.equals(req.getMaxDocumentsRequired())));
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
