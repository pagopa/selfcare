package it.pagopa.selfcare.onboarding.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import it.pagopa.selfcare.onboarding.client.model.BinaryData;
import it.pagopa.selfcare.onboarding.client.model.OnboardingData;
import it.pagopa.selfcare.onboarding.client.model.RequiredDocumentModel;
import it.pagopa.selfcare.onboarding.client.model.UploadedFile;
import it.pagopa.selfcare.onboarding.common.InstitutionType;
import it.pagopa.selfcare.onboarding.exception.ResourceNotFoundException;
import it.pagopa.selfcare.onboarding.mapper.OnboardingMapper;
import it.pagopa.selfcare.onboarding.service.DocumentService;
import it.pagopa.selfcare.onboarding.service.OnboardingService;
import it.pagopa.selfcare.product.entity.Product;
import it.pagopa.selfcare.product.entity.StorageOrigin;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openapi.quarkus.onboarding_json.model.OnboardingGet;

import java.util.List;

@ExtendWith(MockitoExtension.class)
class TokenServiceImplTest {

    @InjectMocks
    private TokenServiceImpl tokenService;

    @Mock
    private OnboardingService onboardingMsConnector;

    @Mock
    private DocumentService documentMsClient;

    @Mock
    private it.pagopa.selfcare.onboarding.service.ProductService productService;

    @Mock
    private OnboardingMapper onboardingMapper;

    @Test
    void getContract_happyPath_returnsBinaryData() {
        // given
        String onboardingId = "onboarding-id";
        BinaryData expected = new BinaryData("contract.pdf", new byte[]{1, 2, 3});
        when(documentMsClient.getContract(onboardingId)).thenReturn(expected);

        // when
        BinaryData result = tokenService.getContract(onboardingId);

        // then
        assertNotNull(result);
        assertEquals(expected.fileName(), result.fileName());
    }

    @Test
    void getContract_nullOnboardingId_throwsNullPointerException() {
        // given / when / then
        assertThrows(NullPointerException.class, () -> tokenService.getContract(null));
    }

    @Test
    void getAttachment_happyPath_returnsBinaryData() {
        // given
        String onboardingId = "onboarding-id";
        String filename = "attachment.pdf";
        BinaryData expected = new BinaryData(filename, new byte[]{1, 2, 3});
        when(documentMsClient.getAttachment(onboardingId, filename)).thenReturn(expected);

        // when
        BinaryData result = tokenService.getAttachment(onboardingId, filename);

        // then
        assertNotNull(result);
        assertEquals(filename, result.fileName());
    }

    @Test
    void getAttachment_nullOnboardingId_throwsNullPointerException() {
        // given / when / then
        assertThrows(NullPointerException.class, () -> tokenService.getAttachment(null, "file.pdf"));
    }

    @Test
    void getAggregatesCsv_happyPath_returnsBinaryData() {
        // given
        String onboardingId = "onboarding-id";
        String productId = "prod-test";
        BinaryData expected = new BinaryData("aggregates.csv", new byte[]{1, 2, 3});
        when(documentMsClient.getAggregatesCsv(onboardingId, productId)).thenReturn(expected);

        // when
        BinaryData result = tokenService.getAggregatesCsv(onboardingId, productId);

        // then
        assertNotNull(result);
        assertEquals("aggregates.csv", result.fileName());
    }

    @Test
    void getAggregatesCsv_nullOnboardingId_throwsNullPointerException() {
        // given / when / then
        assertThrows(NullPointerException.class, () -> tokenService.getAggregatesCsv(null, "prod-test"));
    }

    @Test
    void getAggregatesCsv_nullProductId_throwsNullPointerException() {
        // given / when / then
        assertThrows(NullPointerException.class, () -> tokenService.getAggregatesCsv("onboarding-id", null));
    }

    @Test
    void headAttachment_happyPath_returnsStatusCode() {
        // given
        String onboardingId = "onboarding-id";
        String filename = "attachment.pdf";
        when(documentMsClient.headAttachment(onboardingId, filename)).thenReturn(200);

        // when
        int status = tokenService.headAttachment(onboardingId, filename);

        // then
        assertEquals(200, status);
    }

    @Test
    void headAttachment_nullOnboardingId_throwsNullPointerException() {
        // given / when / then
        assertThrows(NullPointerException.class, () -> tokenService.headAttachment(null, "file.pdf"));
    }

    @Test
    void uploadAttachment_userStorage_callsUploadUserAttachment() {
        // given
        String onboardingId = "onboarding-id";
        String attachmentName = "file.pdf";
        String attachmentId = "doc-id";
        String attachmentDescription = "description";
        UploadedFile attachment = new UploadedFile("file.pdf", "application/pdf", new byte[]{1, 2, 3});

        OnboardingGet onboardingGet = new OnboardingGet();
        OnboardingData onboardingData = new OnboardingData();
        onboardingData.setProductId("prod-test");
        onboardingData.setInstitutionType(InstitutionType.PA);

        RequiredDocumentModel requiredDocument = new RequiredDocumentModel();
        requiredDocument.setId(attachmentId);
        requiredDocument.setStorageOrigin(StorageOrigin.USER);
        requiredDocument.setMaxDocumentsRequired(1);

        when(onboardingMsConnector.getOnboarding(onboardingId)).thenReturn(onboardingGet);
        when(onboardingMapper.toOnboardingData(onboardingGet)).thenReturn(onboardingData);
        when(productService.getRequiredDocuments(anyString(), anyString(), any())).thenReturn(List.of(requiredDocument));

        // when
        tokenService.uploadAttachment(onboardingId, attachment, attachmentName, attachmentId, attachmentDescription);

        // then
        verify(documentMsClient).uploadUserAttachment(
                onboardingId, attachment, "prod-test", attachmentId, attachmentDescription, attachmentName, 1);
    }

    @Test
    void verifyAllowedUserByRole_userMatchesOnboarding_returnsTrue() {
        // given
        String onboardingId = "onboarding-id";
        String uid = "user-uid";

        it.pagopa.selfcare.onboarding.client.model.User user = new it.pagopa.selfcare.onboarding.client.model.User();
        user.setId(uid);
        OnboardingGet onboardingGet = new OnboardingGet();
        OnboardingData onboardingData = new OnboardingData();
        onboardingData.setUsers(List.of(user));

        when(onboardingMsConnector.getOnboardingWithUserInfo(onboardingId)).thenReturn(onboardingGet);
        when(onboardingMapper.toOnboardingData(onboardingGet)).thenReturn(onboardingData);

        // when
        boolean result = tokenService.verifyAllowedUserByRole(onboardingId, uid);

        // then
        assertFalse(!result);
    }

    @Test
    void verifyAllowedUserByRole_userNotInOnboarding_returnsFalse() {
        // given
        String onboardingId = "onboarding-id";
        String uid = "user-uid";

        it.pagopa.selfcare.onboarding.client.model.User user = new it.pagopa.selfcare.onboarding.client.model.User();
        user.setId("other-uid");
        OnboardingGet onboardingGet = new OnboardingGet();
        OnboardingData onboardingData = new OnboardingData();
        onboardingData.setUsers(List.of(user));

        when(onboardingMsConnector.getOnboardingWithUserInfo(onboardingId)).thenReturn(onboardingGet);
        when(onboardingMapper.toOnboardingData(onboardingGet)).thenReturn(onboardingData);

        // when
        boolean result = tokenService.verifyAllowedUserByRole(onboardingId, uid);

        // then
        assertFalse(result);
    }
}
