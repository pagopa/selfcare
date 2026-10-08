package it.pagopa.selfcare.onboarding.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import it.pagopa.selfcare.onboarding.client.model.AttachmentTemplate;
import it.pagopa.selfcare.onboarding.client.model.AvailableDocuments;
import it.pagopa.selfcare.onboarding.client.model.BinaryData;
import it.pagopa.selfcare.onboarding.client.model.ContractTemplate;
import it.pagopa.selfcare.onboarding.client.model.InstitutionUpdate;
import it.pagopa.selfcare.onboarding.client.model.OnboardingData;
import it.pagopa.selfcare.onboarding.client.model.Product;
import it.pagopa.selfcare.onboarding.client.model.RequiredDocumentModel;
import it.pagopa.selfcare.onboarding.client.model.StorageOrigin;
import it.pagopa.selfcare.onboarding.client.model.UploadedFile;
import it.pagopa.selfcare.onboarding.common.InstitutionType;
import it.pagopa.selfcare.onboarding.exception.ResourceNotFoundException;
import it.pagopa.selfcare.onboarding.mapper.OnboardingMapper;
import it.pagopa.selfcare.onboarding.service.impl.DocumentService;
import it.pagopa.selfcare.onboarding.service.OnboardingService;
import it.pagopa.selfcare.onboarding.service.ProductService;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openapi.quarkus.onboarding_json.model.OnboardingGet;

@ExtendWith(MockitoExtension.class)
class TokenServiceImplTest {

    private static final String ONBOARDING_ID = "onboarding-id";
    private static final String PRODUCT_ID = "prod-io";
    private static final UploadedFile FILE = new UploadedFile("attachment.pdf", "application/pdf", new byte[]{1, 2});

    @InjectMocks
    private TokenServiceImpl tokenService;

    @Mock
    private OnboardingService onboardingMsConnector;

    @Mock
    private DocumentService documentMsClient;

    @Mock
    private ProductService productService;

    @Mock
    private OnboardingMapper onboardingMapper;

    @Test
    void verifyOnboarding_mapsTheOnboarding() {
        OnboardingGet downstream = new OnboardingGet();
        OnboardingData mapped = new OnboardingData();
        when(onboardingMsConnector.getOnboarding(ONBOARDING_ID)).thenReturn(downstream);
        when(onboardingMapper.toOnboardingData(downstream)).thenReturn(mapped);

        assertSame(mapped, tokenService.verifyOnboarding(ONBOARDING_ID));
        verify(onboardingMsConnector, never()).getOnboardingWithUserInfo(any());
    }

    @Test
    void getOnboardingWithUserInfo_usesTheUserInfoEndpoint() {
        OnboardingGet downstream = new OnboardingGet();
        OnboardingData mapped = new OnboardingData();
        when(onboardingMsConnector.getOnboardingWithUserInfo(ONBOARDING_ID)).thenReturn(downstream);
        when(onboardingMapper.toOnboardingData(downstream)).thenReturn(mapped);

        assertSame(mapped, tokenService.getOnboardingWithUserInfo(ONBOARDING_ID));
        verify(onboardingMsConnector, never()).getOnboarding(any());
    }

    @Test
    void approveAndReject_forwardTheRequester() {
        tokenService.approveOnboarding(ONBOARDING_ID, "uid-1");
        tokenService.rejectOnboarding(ONBOARDING_ID, "wrong data", "uid-1");

        verify(onboardingMsConnector).approveOnboarding(ONBOARDING_ID, "uid-1");
        verify(onboardingMsConnector).rejectOnboarding(ONBOARDING_ID, "wrong data", "uid-1");
    }

    @Test
    void completeOperations_delegateTheContract() {
        tokenService.completeTokenV2(ONBOARDING_ID, FILE);
        tokenService.completeOnboardingUsers(ONBOARDING_ID, FILE);

        verify(onboardingMsConnector).onboardingTokenComplete(ONBOARDING_ID, FILE);
        verify(onboardingMsConnector).onboardingUsersComplete(ONBOARDING_ID, FILE);
    }

    @Test
    void documentReads_delegateToDocumentMs() {
        BinaryData contract = new BinaryData("contract.pdf", new byte[]{1});
        BinaryData signed = new BinaryData("signed.pdf", new byte[]{2});
        BinaryData attachment = new BinaryData("att.pdf", new byte[]{3});
        BinaryData csv = new BinaryData("aggregates.csv", new byte[]{4});
        AvailableDocuments available = new AvailableDocuments();
        when(documentMsClient.getContract(ONBOARDING_ID)).thenReturn(contract);
        when(documentMsClient.getContractSigned(ONBOARDING_ID)).thenReturn(signed);
        when(documentMsClient.getAttachment(ONBOARDING_ID, "att")).thenReturn(attachment);
        when(documentMsClient.getAggregatesCsv(ONBOARDING_ID, PRODUCT_ID)).thenReturn(csv);
        when(documentMsClient.getAvailableDocuments(ONBOARDING_ID)).thenReturn(available);
        when(documentMsClient.headAttachment(ONBOARDING_ID, "att")).thenReturn(204);

        assertSame(contract, tokenService.getContract(ONBOARDING_ID));
        assertSame(signed, tokenService.getContractSigned(ONBOARDING_ID));
        assertSame(attachment, tokenService.getAttachment(ONBOARDING_ID, "att"));
        assertSame(csv, tokenService.getAggregatesCsv(ONBOARDING_ID, PRODUCT_ID));
        assertSame(available, tokenService.getAvailableDocuments(ONBOARDING_ID));
        assertEquals(204, tokenService.headAttachment(ONBOARDING_ID, "att"));
    }

    @Test
    void requiredArgumentsAreChecked() {
        assertThrows(NullPointerException.class, () -> tokenService.verifyOnboarding(null));
        assertThrows(NullPointerException.class, () -> tokenService.getOnboardingWithUserInfo(null));
        assertThrows(NullPointerException.class, () -> tokenService.approveOnboarding(null, "uid"));
        assertThrows(NullPointerException.class, () -> tokenService.rejectOnboarding(null, "reason", "uid"));
        assertThrows(NullPointerException.class, () -> tokenService.completeTokenV2(null, FILE));
        assertThrows(NullPointerException.class, () -> tokenService.completeOnboardingUsers(null, FILE));
        assertThrows(NullPointerException.class, () -> tokenService.getContract(null));
        assertThrows(NullPointerException.class, () -> tokenService.getContractSigned(null));
        assertThrows(NullPointerException.class, () -> tokenService.getAttachment(null, "att"));
        assertThrows(NullPointerException.class, () -> tokenService.getAttachment(ONBOARDING_ID, null));
        assertThrows(NullPointerException.class, () -> tokenService.getAvailableDocuments(null));
        assertThrows(NullPointerException.class, () -> tokenService.getAggregatesCsv(null, PRODUCT_ID));
        assertThrows(NullPointerException.class, () -> tokenService.getAggregatesCsv(ONBOARDING_ID, null));
        assertThrows(NullPointerException.class, () -> tokenService.headAttachment(null, "att"));
        assertThrows(NullPointerException.class, () -> tokenService.headAttachment(ONBOARDING_ID, null));
        verifyNoInteractions(onboardingMsConnector, documentMsClient, productService);
    }

    @Test
    void getTemplateAttachment_resolvesTheTemplatePathFromTheProduct() {
        OnboardingData onboarding = onboarding();
        when(onboardingMsConnector.getOnboarding(ONBOARDING_ID)).thenReturn(new OnboardingGet());
        when(onboardingMapper.toOnboardingData(any(OnboardingGet.class))).thenReturn(onboarding);
        when(productService.getProductValid(PRODUCT_ID)).thenReturn(product("Allegato 1", "template/path"));
        BinaryData expected = new BinaryData("template.pdf", new byte[]{1});
        when(documentMsClient.getTemplateAttachment(ONBOARDING_ID, "Comune di Test", "Allegato 1", PRODUCT_ID, "template/path"))
                .thenReturn(expected);

        assertSame(expected, tokenService.getTemplateAttachment(ONBOARDING_ID, "Allegato 1"));
    }

    @Test
    void getTemplateAttachment_unknownAttachmentIsNotFound() {
        when(onboardingMsConnector.getOnboarding(ONBOARDING_ID)).thenReturn(new OnboardingGet());
        when(onboardingMapper.toOnboardingData(any(OnboardingGet.class))).thenReturn(onboarding());
        when(productService.getProductValid(PRODUCT_ID)).thenReturn(product("Allegato 1", "template/path"));

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
                () -> tokenService.getTemplateAttachment(ONBOARDING_ID, "Missing"));

        assertEquals("Attachment with name Missing not found", exception.getMessage());
        verifyNoInteractions(documentMsClient);
    }

    @Test
    void uploadAttachment_systemStorageUsesTheProductTemplate() {
        OnboardingData onboarding = onboarding();
        when(onboardingMsConnector.getOnboarding(ONBOARDING_ID)).thenReturn(new OnboardingGet());
        when(onboardingMapper.toOnboardingData(any(OnboardingGet.class))).thenReturn(onboarding);
        when(productService.getRequiredDocuments("AR", PRODUCT_ID, "PA", "IPA")).thenReturn(List.of());
        when(productService.getProductValid(PRODUCT_ID)).thenReturn(product("Allegato 1", "template/path"));

        tokenService.uploadAttachment("AR", ONBOARDING_ID, FILE, "Allegato 1", null, null);

        verify(documentMsClient).uploadAttachment(
                eq(ONBOARDING_ID),
                eq(FILE),
                eq("Allegato 1"),
                eq(PRODUCT_ID),
                argThat(template -> "template/path".equals(template.getTemplatePath())));
        verify(documentMsClient, never()).uploadUserAttachment(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void uploadAttachment_userStorageForwardsTheRequiredDocumentLimit() {
        OnboardingData onboarding = onboarding();
        when(onboardingMsConnector.getOnboarding(ONBOARDING_ID)).thenReturn(new OnboardingGet());
        when(onboardingMapper.toOnboardingData(any(OnboardingGet.class))).thenReturn(onboarding);
        RequiredDocumentModel required = new RequiredDocumentModel();
        required.setId("statuto");
        required.setStorageOrigin(StorageOrigin.USER);
        required.setMaxDocumentsRequired(3);
        RequiredDocumentModel other = new RequiredDocumentModel();
        other.setId("other");
        other.setStorageOrigin(StorageOrigin.SYSTEM);
        when(productService.getRequiredDocuments("AR", PRODUCT_ID, "PA", "IPA")).thenReturn(List.of(other, required));

        tokenService.uploadAttachment("AR", ONBOARDING_ID, FILE, "Statuto", "statuto", "descrizione");

        verify(documentMsClient).uploadUserAttachment(ONBOARDING_ID, FILE, PRODUCT_ID, "statuto", "descrizione", "Statuto", 3);
        verify(documentMsClient, never()).uploadAttachment(any(), any(), any(), any(), any());
        verify(productService, never()).getProductValid(any());
    }

    @Test
    void uploadAttachment_requiresIdNameAndFile() {
        assertThrows(NullPointerException.class, () -> tokenService.uploadAttachment("AR", null, FILE, "n", null, null));
        assertThrows(NullPointerException.class, () -> tokenService.uploadAttachment("AR", ONBOARDING_ID, FILE, null, null, null));
        assertThrows(NullPointerException.class, () -> tokenService.uploadAttachment("AR", ONBOARDING_ID, null, "n", null, null));
        verifyNoInteractions(onboardingMsConnector, documentMsClient, productService);
    }

    private static OnboardingData onboarding() {
        InstitutionUpdate institutionUpdate = new InstitutionUpdate();
        institutionUpdate.setDescription("Comune di Test");
        institutionUpdate.setOrigin("IPA");
        OnboardingData onboarding = new OnboardingData();
        onboarding.setId(ONBOARDING_ID);
        onboarding.setProductId(PRODUCT_ID);
        onboarding.setInstitutionType(InstitutionType.PA);
        onboarding.setInstitutionUpdate(institutionUpdate);
        return onboarding;
    }

    private static Product product(String attachmentName, String templatePath) {
        AttachmentTemplate attachment = new AttachmentTemplate();
        attachment.setName(attachmentName);
        attachment.setTemplatePath(templatePath);
        ContractTemplate contract = new ContractTemplate();
        contract.setAttachments(List.of(attachment));
        Product product = new Product();
        product.setId(PRODUCT_ID);
        product.setInstitutionContractMappings(Map.of("PA", contract));
        return product;
    }
}
