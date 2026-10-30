package it.pagopa.selfcare.dashboard.service;

import it.pagopa.selfcare.core.generated.openapi.v1.dto.OnboardingResponse;
import it.pagopa.selfcare.core.generated.openapi.v1.dto.OnboardingsResponse;
import it.pagopa.selfcare.dashboard.client.CoreInstitutionApiRestClient;
import it.pagopa.selfcare.dashboard.client.DocumentRestClient;
import it.pagopa.selfcare.dashboard.client.OnboardingRestClient;
import it.pagopa.selfcare.dashboard.exception.ResourceNotFoundException;
import it.pagopa.selfcare.dashboard.model.document.DocumentResource;
import it.pagopa.selfcare.dashboard.model.document.DocumentStatusResource;
import it.pagopa.selfcare.dashboard.model.document.DocumentType;
import it.pagopa.selfcare.dashboard.model.document.DocumentsResource;
import it.pagopa.selfcare.dashboard.model.document.ProductDocumentStatusResource;
import it.pagopa.selfcare.document.generated.openapi.v1.dto.AvailableDocumentResponse;
import it.pagopa.selfcare.document.generated.openapi.v1.dto.AvailableDocumentsResponse;
import it.pagopa.selfcare.onboarding.generated.openapi.v1.dto.OnboardingGet;
import it.pagopa.selfcare.onboarding.generated.openapi.v1.dto.OnboardingGetResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DocumentServiceImplTest {

    private static final String INSTITUTION_ID = "institutionId";
    private static final String PRODUCT_ID = "prod-pagopa";
    private static final List<String> REQUESTED_PRODUCTS = List.of("prod-io", "prod-pagopa", "prod-pn", "prod-interop", "prod-fd");

    private DocumentServiceImpl documentService;

    @Mock
    private OnboardingRestClient onboardingRestClient;

    @Mock
    private CoreInstitutionApiRestClient coreInstitutionApiRestClient;

    @Mock
    private DocumentRestClient documentRestClient;

    @BeforeEach
    void setUp() {
        documentService = new DocumentServiceImpl(onboardingRestClient, coreInstitutionApiRestClient, documentRestClient, List.of("Addendum"));
    }

    @Test
    void getDocumentStatus_computesStatusForEachRequestedProduct() {
        // given
        LocalDateTime now = LocalDateTime.now();
        OnboardingGetResponse response = new OnboardingGetResponse();
        response.setItems(List.of(
                onboarding("prod-io", "CONTRACT_REGISTRATION", "COMPLETED", null, true),
                onboarding("prod-io", "CONTRACT_UPDATE", "PENDING", now.minusDays(1), null),
                onboarding("prod-pagopa", "CONTRACT_REGISTRATION", "COMPLETED", null, true),
                onboarding("prod-pagopa", "CONTRACT_UPDATE", "PENDING", now.plusDays(10), null),
                onboarding("prod-pn", "CONTRACT_REGISTRATION", "DELETED", null, true),
                onboarding("prod-pn", "CONTRACT_UPDATE", "REQUEST", now.plusDays(10), null),
                onboarding("prod-pn", "USERS", "PENDING", now.plusDays(10), null),
                onboarding("prod-interop", "CONTRACT_UPDATE", "COMPLETED", now.plusDays(10), null)));
        when(onboardingRestClient._getOnboardingWithFilter(null, INSTITUTION_ID, null, null, null, REQUESTED_PRODUCTS, null, true, null, null, null, null, null, null))
                .thenReturn(ResponseEntity.ok(response));

        // when
        DocumentStatusResource result = documentService.getDocumentStatus(INSTITUTION_ID,
                List.of("prod-io", "prod-pagopa", "prod-pn", "prod-interop", "prod-io", "prod-fd"));

        // then
        assertEquals(List.of(
                new ProductDocumentStatusResource("prod-io", true, false),
                new ProductDocumentStatusResource("prod-pagopa", true, true),
                new ProductDocumentStatusResource("prod-pn", false, false),
                new ProductDocumentStatusResource("prod-interop", false, false),
                new ProductDocumentStatusResource("prod-fd", false, false)), result.getProducts());
        verify(onboardingRestClient, times(1))
                ._getOnboardingWithFilter(null, INSTITUTION_ID, null, null, null, REQUESTED_PRODUCTS, null, true, null, null, null, null, null, null);
        verifyNoMoreInteractions(onboardingRestClient);
    }

    @Test
    void getDocumentStatus_withoutOnboardings_returnsFalseFlags() {
        // given
        when(onboardingRestClient._getOnboardingWithFilter(null, INSTITUTION_ID, null, null, null, List.of("prod-io"), null, true, null, null, null, null, null, null))
                .thenReturn(ResponseEntity.ok().build());

        // when
        DocumentStatusResource result = documentService.getDocumentStatus(INSTITUTION_ID, List.of("prod-io"));

        // then
        assertEquals(List.of(new ProductDocumentStatusResource("prod-io", false, false)), result.getProducts());
    }

    @Test
    void getDocumentStatus_withoutProductIds_doesNotCallOnboardingMs() {
        DocumentStatusResource result = documentService.getDocumentStatus(INSTITUTION_ID, List.of());

        assertEquals(List.of(), result.getProducts());
        verifyNoInteractions(onboardingRestClient);
    }

    @Test
    void getDocumentStatus_withoutInstitutionId_throwsIllegalArgument() {
        List<String> productIds = List.of("prod-io");
        assertThrows(IllegalArgumentException.class, () -> documentService.getDocumentStatus(null, productIds));
        verifyNoInteractions(onboardingRestClient);
    }

    @Test
    void getDocuments_returnsContractsAndAllowedAttachmentsOfTheActiveOnboarding() {
        // given
        OnboardingsResponse onboardings = new OnboardingsResponse();
        onboardings.setOnboardings(List.of(
                institutionOnboarding(PRODUCT_ID, OnboardingResponse.StatusEnum.DELETED, "deletedToken", OffsetDateTime.now()),
                institutionOnboarding(PRODUCT_ID, OnboardingResponse.StatusEnum.ACTIVE, "oldToken", OffsetDateTime.now().minusYears(2)),
                institutionOnboarding(PRODUCT_ID, OnboardingResponse.StatusEnum.ACTIVE, "tokenId", OffsetDateTime.now().minusYears(1)),
                institutionOnboarding("prod-io", OnboardingResponse.StatusEnum.ACTIVE, "otherProductToken", OffsetDateTime.now())));
        when(coreInstitutionApiRestClient._getOnboardingsInstitutionUsingGET(INSTITUTION_ID, null))
                .thenReturn(ResponseEntity.ok(onboardings));

        AvailableDocumentsResponse available = new AvailableDocumentsResponse();
        available.setDocuments(List.of(
                availableDocument("Piattaforma_pagoPA_accordo_adesione.pdf", it.pagopa.selfcare.document.generated.openapi.v1.dto.DocumentType.INSTITUTION),
                availableDocument("Accordo di Adesione IO v. 2.3_20 gennaio 2021.pdf.0000010072.p7m", it.pagopa.selfcare.document.generated.openapi.v1.dto.DocumentType.INSTITUTION),
                availableDocument("Addendum", it.pagopa.selfcare.document.generated.openapi.v1.dto.DocumentType.ATTACHMENT),
                availableDocument("Dichiarazione_sostitutiva_certificazione", it.pagopa.selfcare.document.generated.openapi.v1.dto.DocumentType.ATTACHMENT),
                availableDocument("user-contract.pdf", it.pagopa.selfcare.document.generated.openapi.v1.dto.DocumentType.USER)));
        when(documentRestClient._getAvailableDocuments("tokenId")).thenReturn(ResponseEntity.ok(available));

        // when
        DocumentsResource result = documentService.getDocuments(INSTITUTION_ID, PRODUCT_ID);

        // then
        assertEquals(List.of(
                new DocumentResource("Piattaforma_pagoPA_accordo_adesione", DocumentType.INSTITUTION),
                new DocumentResource("Accordo di Adesione IO v. 2.3_20 gennaio 2021", DocumentType.INSTITUTION),
                new DocumentResource("Addendum", DocumentType.ATTACHMENT)), result.getDocuments());
        verify(documentRestClient, times(1))._getAvailableDocuments("tokenId");
        verifyNoMoreInteractions(documentRestClient);
    }

    @Test
    void getDocuments_withoutActiveOnboarding_throwsResourceNotFound() {
        OnboardingsResponse onboardings = new OnboardingsResponse();
        onboardings.setOnboardings(List.of(
                institutionOnboarding(PRODUCT_ID, OnboardingResponse.StatusEnum.SUSPENDED, "tokenId", OffsetDateTime.now())));
        when(coreInstitutionApiRestClient._getOnboardingsInstitutionUsingGET(INSTITUTION_ID, null))
                .thenReturn(ResponseEntity.ok(onboardings));

        assertThrows(ResourceNotFoundException.class, () -> documentService.getDocuments(INSTITUTION_ID, PRODUCT_ID));
        verifyNoInteractions(documentRestClient);
    }

    @Test
    void getDocuments_withoutAvailableDocuments_returnsEmptyList() {
        OnboardingsResponse onboardings = new OnboardingsResponse();
        onboardings.setOnboardings(List.of(
                institutionOnboarding(PRODUCT_ID, OnboardingResponse.StatusEnum.ACTIVE, "tokenId", OffsetDateTime.now())));
        when(coreInstitutionApiRestClient._getOnboardingsInstitutionUsingGET(INSTITUTION_ID, null))
                .thenReturn(ResponseEntity.ok(onboardings));
        when(documentRestClient._getAvailableDocuments("tokenId")).thenReturn(ResponseEntity.ok().build());

        DocumentsResource result = documentService.getDocuments(INSTITUTION_ID, PRODUCT_ID);

        assertEquals(List.of(), result.getDocuments());
    }

    @Test
    void getDocuments_keepsAttachmentNameAsStoredForDownload() {
        documentService = new DocumentServiceImpl(onboardingRestClient, coreInstitutionApiRestClient, documentRestClient, List.of("Addendum.pdf"));
        OnboardingsResponse onboardings = new OnboardingsResponse();
        onboardings.setOnboardings(List.of(
                institutionOnboarding(PRODUCT_ID, OnboardingResponse.StatusEnum.ACTIVE, "tokenId", OffsetDateTime.now())));
        when(coreInstitutionApiRestClient._getOnboardingsInstitutionUsingGET(INSTITUTION_ID, null))
                .thenReturn(ResponseEntity.ok(onboardings));
        AvailableDocumentsResponse available = new AvailableDocumentsResponse();
        available.setDocuments(List.of(
                availableDocument("contract.pdf", it.pagopa.selfcare.document.generated.openapi.v1.dto.DocumentType.INSTITUTION),
                availableDocument("Addendum.pdf", it.pagopa.selfcare.document.generated.openapi.v1.dto.DocumentType.ATTACHMENT)));
        when(documentRestClient._getAvailableDocuments("tokenId")).thenReturn(ResponseEntity.ok(available));

        DocumentsResource result = documentService.getDocuments(INSTITUTION_ID, PRODUCT_ID);

        assertEquals(List.of(
                new DocumentResource("contract", DocumentType.INSTITUTION),
                new DocumentResource("Addendum.pdf", DocumentType.ATTACHMENT)), result.getDocuments());
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "Accordo di Adesione IO v. 2.3_20 gennaio 2021.pdf.0000010072.p7m | Accordo di Adesione IO v. 2.3_20 gennaio 2021",
            "Piattaforma_pagoPA_accordo_adesione.PDF                          | Piattaforma_pagoPA_accordo_adesione",
            "signed_contract.pdf.p7m                                          | signed_contract",
            "Addendum                                                         | Addendum",
            "report.pdfx                                                      | report.pdfx"
    })
    void toDisplayName_stripsFileExtensions(String fileName, String expected) {
        assertEquals(expected, DocumentServiceImpl.toDisplayName(fileName));
    }

    private static OnboardingResponse institutionOnboarding(String productId, OnboardingResponse.StatusEnum status,
                                                            String tokenId, OffsetDateTime createdAt) {
        OnboardingResponse onboarding = new OnboardingResponse();
        onboarding.setProductId(productId);
        onboarding.setStatus(status);
        onboarding.setTokenId(tokenId);
        onboarding.setCreatedAt(createdAt);
        return onboarding;
    }

    private static AvailableDocumentResponse availableDocument(String name,
                                                               it.pagopa.selfcare.document.generated.openapi.v1.dto.DocumentType type) {
        AvailableDocumentResponse document = new AvailableDocumentResponse();
        document.setName(name);
        document.setType(type);
        return document;
    }

    private static OnboardingGet onboarding(String productId, String workflowType, String status,
                                            LocalDateTime expiringDate, Boolean contractUpdateRequired) {
        OnboardingGet onboarding = new OnboardingGet();
        onboarding.setProductId(productId);
        onboarding.setWorkflowType(workflowType);
        onboarding.setStatus(status);
        onboarding.setExpiringDate(expiringDate);
        onboarding.setContractUpdateRequired(contractUpdateRequired);
        return onboarding;
    }

}
