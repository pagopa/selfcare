package it.pagopa.selfcare.dashboard.service;

import it.pagopa.selfcare.dashboard.client.OnboardingRestClient;
import it.pagopa.selfcare.dashboard.model.document.DocumentStatusResource;
import it.pagopa.selfcare.dashboard.model.document.ProductDocumentStatusResource;
import it.pagopa.selfcare.onboarding.generated.openapi.v1.dto.OnboardingGet;
import it.pagopa.selfcare.onboarding.generated.openapi.v1.dto.OnboardingGetResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DocumentServiceImplTest {

    private static final String INSTITUTION_ID = "institutionId";
    private static final List<String> REQUESTED_PRODUCTS = List.of("prod-io", "prod-pagopa", "prod-pn", "prod-interop", "prod-fd");

    @InjectMocks
    private DocumentServiceImpl documentService;

    @Mock
    private OnboardingRestClient onboardingRestClient;

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
