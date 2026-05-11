package it.pagopa.selfcare.external_api.service;

import it.pagopa.selfcare.commons.utils.TestUtils;
import it.pagopa.selfcare.core.generated.openapi.v1.dto.OnboardingResponse;
import it.pagopa.selfcare.core.generated.openapi.v1.dto.OnboardingsResponse;
import it.pagopa.selfcare.document.generated.openapi.v1.dto.Document;
import it.pagopa.selfcare.document.generated.openapi.v1.dto.DocumentResponse;
import it.pagopa.selfcare.external_api.client.MsCoreInstitutionApiClient;
import it.pagopa.selfcare.external_api.client.MsDocumentApiClient;
import it.pagopa.selfcare.external_api.client.MsDocumentContentApiClient;
import it.pagopa.selfcare.external_api.exception.ResourceNotFoundException;
import it.pagopa.selfcare.external_api.mapper.DocumentMapperImpl;
import it.pagopa.selfcare.external_api.mapper.InstitutionMapperImpl;
import it.pagopa.selfcare.external_api.model.document.ResourceResponse;
import it.pagopa.selfcare.external_api.model.onboarding.InstitutionOnboarding;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;

import java.io.IOException;
import java.nio.file.Files;
import java.util.Collections;
import java.util.List;

import static com.azure.core.http.ContentType.APPLICATION_OCTET_STREAM;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith({MockitoExtension.class})
class ContractServiceImplTest extends BaseServiceTestUtils {
    @InjectMocks
    private ContractServiceImpl contractService;

    @Mock
    private MsDocumentApiClient documentApiClient;

    @Mock
    private MsDocumentContentApiClient documentContentApiClient;

    @Mock
    private MsCoreInstitutionApiClient institutionApiClient;

    @Spy
    private InstitutionMapperImpl institutionMapper;

    @Spy
    private DocumentMapperImpl documentMapper;

    @BeforeEach
    public void setUp() {
        super.setUp();
    }

    @Test
    void getContractV2() throws Exception {
        InstitutionOnboarding institutionOnboarding = new InstitutionOnboarding();
        institutionOnboarding.setTokenId("tokenId");
        ClassPathResource inputResource = new ClassPathResource("expectations/Document.json");
        byte[] documentStream = Files.readAllBytes(inputResource.getFile().toPath());
        Document document = objectMapper.readValue(documentStream, Document.class);
        OnboardingsResponse onboardingsResponse = mock(OnboardingsResponse.class);

        Resource resource = new ByteArrayResource("test content".getBytes());
        ResponseEntity<Resource> responseFile = ResponseEntity.ok()
          .header(HttpHeaders.CONTENT_TYPE, APPLICATION_OCTET_STREAM)
          .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"contractSigned\"")
          .body(resource);

        when(onboardingsResponse.getOnboardings()).thenReturn(List.of(TestUtils.mockInstance(new OnboardingResponse())));
        when(documentContentApiClient._getContractSigned(anyString())).thenReturn(responseFile);

        DocumentResponse documentResponse = new DocumentResponse();
        documentResponse.setContractSigned(document.getContractSigned());
        when(documentApiClient._getDocumentByOnboardingId(any()))
                .thenReturn(ResponseEntity.ok(documentResponse));
        when(institutionApiClient._getOnboardingsInstitutionUsingGET("institutionId", "productId")).thenReturn(ResponseEntity.ok(onboardingsResponse));
        ResourceResponse result = contractService.getContractV2("institutionId", "productId");
        Assertions.assertEquals("application/octet-stream", result.getMimetype());
        Assertions.assertEquals("contractSigned", result.getFileName());
        Assertions.assertEquals(12, result.getData().length);
    }

    @Test
    void getContractErrorTest() throws Exception {
        InstitutionOnboarding institutionOnboarding = new InstitutionOnboarding();
        institutionOnboarding.setTokenId("tokenId");
        ClassPathResource inputResource = new ClassPathResource("expectations/Document.json");
        byte[] documentStream = Files.readAllBytes(inputResource.getFile().toPath());
        Document document = objectMapper.readValue(documentStream, Document.class);
        OnboardingsResponse onboardingsResponse = mock(OnboardingsResponse.class);

        ResponseEntity<Resource> responseFile = ResponseEntity.ok()
          .header(HttpHeaders.CONTENT_TYPE, APPLICATION_OCTET_STREAM)
          .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"contractSigned\"")
          .body(null);

        when(onboardingsResponse.getOnboardings()).thenReturn(List.of(TestUtils.mockInstance(new OnboardingResponse())));
        when(documentContentApiClient._getContractSigned(anyString())).thenReturn(responseFile);

        DocumentResponse documentResponse = new DocumentResponse();
        documentResponse.setContractSigned(document.getContractSigned());
        when(documentApiClient._getDocumentByOnboardingId(any()))
          .thenReturn(ResponseEntity.ok(documentResponse));
        when(institutionApiClient._getOnboardingsInstitutionUsingGET("institutionId", "productId")).thenReturn(ResponseEntity.ok(onboardingsResponse));
        Assertions.assertThrows(ResourceNotFoundException.class, () -> contractService.getContractV2("institutionId", "productId"));
    }

    @Test
    void getContractV2WhereTokenIsEmpty() {
        InstitutionOnboarding institutionOnboarding = new InstitutionOnboarding();
        institutionOnboarding.setTokenId(null);
        OnboardingsResponse onboardingsResponse = mock(OnboardingsResponse.class);
        when(onboardingsResponse.getOnboardings()).thenReturn(Collections.emptyList());
        when(institutionApiClient._getOnboardingsInstitutionUsingGET("institutionId", "productId")).thenReturn(ResponseEntity.ok(onboardingsResponse));
        Assertions.assertThrows(ResourceNotFoundException.class,
                () -> contractService.getContractV2("institutionId", "productId"),
                "Token for institutionId and productId not found!");
    }

    @Test
    void getContractV2WhereTokenIsNull() {
        InstitutionOnboarding institutionOnboarding = new InstitutionOnboarding();
        institutionOnboarding.setTokenId(null);
        OnboardingsResponse onboardingsResponse = new OnboardingsResponse();
        when(institutionApiClient._getOnboardingsInstitutionUsingGET("institutionId", "productId")).thenReturn(ResponseEntity.ok(onboardingsResponse));
        Assertions.assertThrows(ResourceNotFoundException.class,
                () -> contractService.getContractV2("institutionId", "productId"),
                "Token for institutionId and productId not found!");
    }

    @Test
    void getContractV2WhereContractSignedIsNull() throws IOException {
        InstitutionOnboarding institutionOnboarding = new InstitutionOnboarding();
        institutionOnboarding.setTokenId(null);



        OnboardingsResponse onboardingsResponse = mock(OnboardingsResponse.class);

        DocumentResponse documentResponse = new DocumentResponse();
        when(documentApiClient._getDocumentByOnboardingId(any()))
                .thenReturn(ResponseEntity.ok(documentResponse));

        when(onboardingsResponse.getOnboardings()).thenReturn(List.of(TestUtils.mockInstance(new OnboardingResponse())));
        when(institutionApiClient._getOnboardingsInstitutionUsingGET("institutionId", "productId")).thenReturn(ResponseEntity.ok(onboardingsResponse));
        Assertions.assertThrows(ResourceNotFoundException.class,
                () -> contractService.getContractV2("institutionId", "productId"),
                "Token for institutionId and productId found but contract signed reference is empty!");
    }
}
