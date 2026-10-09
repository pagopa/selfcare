package it.pagopa.selfcare.onboarding.service.impl;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.test.UniAssertSubscriber;
import io.smallrye.mutiny.subscription.UniEmitter;
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
import it.pagopa.selfcare.onboarding.exception.InvalidRequestException;
import it.pagopa.selfcare.onboarding.model.dto.request.DownloadDocumentType;
import it.pagopa.selfcare.onboarding.mapper.OnboardingMapper;
import it.pagopa.selfcare.onboarding.service.impl.DocumentService;
import it.pagopa.selfcare.onboarding.service.OnboardingService;
import it.pagopa.selfcare.onboarding.service.ProductService;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import jakarta.ws.rs.ProcessingException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openapi.quarkus.onboarding_json.model.OnboardingGet;

@ExtendWith(MockitoExtension.class)
class TokenServiceImplTest {

    private static final String ONBOARDING_ID = "onboarding-id";
    private static final String PRODUCT_ID = "prod-io";
    private static final UploadedFile FILE = new UploadedFile("attachment.pdf", "application/pdf", new byte[] {1, 2});

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
    void downloadDocument_selectsTheSignedContractAndIgnoresTheAttachmentName() {
        // given
        BinaryData signed = new BinaryData("signed.pdf", new byte[] {1, 2});
        when(documentMsClient.getContractSigned(ONBOARDING_ID)).thenReturn(Uni.createFrom().item(signed));

        // when
        BinaryData downloaded = tokenService.downloadDocument(ONBOARDING_ID, DownloadDocumentType.CONTRACT_SIGNED, "ignored.pdf")
                .await().indefinitely();

        // then
        assertSame(signed, downloaded);
        verify(documentMsClient, never()).getAttachment(any(), any());
        verifyNoInteractions(onboardingMsConnector, productService);
    }

    @Test
    void downloadDocument_forwardsTheAttachmentNameWithoutNormalizingWhitespace() {
        // given
        BinaryData attachment = new BinaryData("doc.pdf", new byte[] {3});
        when(documentMsClient.getAttachment(ONBOARDING_ID, " doc.pdf ")).thenReturn(Uni.createFrom().item(attachment));

        // when
        BinaryData downloaded = tokenService.downloadDocument(ONBOARDING_ID, DownloadDocumentType.ATTACHMENT, " doc.pdf ")
                .await().indefinitely();

        // then
        assertSame(attachment, downloaded);
        verify(documentMsClient, never()).getContractSigned(any());
        verifyNoInteractions(onboardingMsConnector, productService);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", " ", "\t"})
    void downloadDocument_requiresAnAttachmentNameBeforeCallingDocumentMs(String name) {
        // given
        DownloadDocumentType type = DownloadDocumentType.ATTACHMENT;

        // when
        InvalidRequestException failure = assertThrows(InvalidRequestException.class,
                () -> tokenService.downloadDocument(ONBOARDING_ID, type, name));

        // then
        assertEquals("Query parameter 'name' is required when type=ATTACHMENT", failure.getMessage());
        verifyNoInteractions(documentMsClient, onboardingMsConnector, productService);
    }

    @Test
    void verifyOnboarding_mapsTheOnboarding() {
        // given
        OnboardingGet downstream = new OnboardingGet();
        OnboardingData mapped = new OnboardingData();
        when(onboardingMsConnector.getOnboarding(ONBOARDING_ID)).thenReturn(Uni.createFrom().item(downstream));
        when(onboardingMapper.toOnboardingData(downstream)).thenReturn(mapped);

        // when
        var actualAsync1 = tokenService.verifyOnboarding(ONBOARDING_ID).await().indefinitely();

        // then
        assertSame(mapped, actualAsync1);
        verify(onboardingMsConnector, never()).getOnboardingWithUserInfo(any());
    }

    @Test
    void getOnboardingWithUserInfo_usesTheUserInfoEndpointAndMapsOnlyWhenItEmits() {
        // given
        AtomicReference<UniEmitter<? super OnboardingGet>> pending = new AtomicReference<>();
        OnboardingGet downstream = new OnboardingGet();
        OnboardingData mapped = new OnboardingData();
        when(onboardingMsConnector.getOnboardingWithUserInfo(ONBOARDING_ID))
                .thenReturn(Uni.createFrom().<OnboardingGet>emitter(pending::set));
        when(onboardingMapper.toOnboardingData(downstream)).thenReturn(mapped);

        // when
        UniAssertSubscriber<OnboardingData> result = tokenService.getOnboardingWithUserInfo(ONBOARDING_ID)
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        // then
        result.assertNotTerminated();
        verifyNoInteractions(onboardingMapper);
        pending.get().complete(downstream);
        result.assertCompleted().assertItem(mapped);
        verify(onboardingMsConnector, never()).getOnboarding(any());
    }

    @Test
    void getOnboardingWithUserInfo_propagatesTheLookupFailureWithoutMapping() {
        // given
        ResourceNotFoundException failure = new ResourceNotFoundException("missing onboarding");
        when(onboardingMsConnector.getOnboardingWithUserInfo(ONBOARDING_ID)).thenReturn(Uni.createFrom().failure(failure));

        // when
        UniAssertSubscriber<OnboardingData> result = tokenService.getOnboardingWithUserInfo(ONBOARDING_ID)
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        // then
        assertSame(failure, result.getFailure());
        verifyNoInteractions(onboardingMapper);
    }

    @Test
    void approveAndReject_forwardTheRequesterAndCompleteOnlyWithTheDownstream() {
        // given
        AtomicReference<UniEmitter<? super Void>> approval = new AtomicReference<>();
        AtomicReference<UniEmitter<? super Void>> rejection = new AtomicReference<>();
        when(onboardingMsConnector.approveOnboarding(ONBOARDING_ID, "uid-1"))
                .thenReturn(Uni.createFrom().<Void>emitter(approval::set));
        when(onboardingMsConnector.rejectOnboarding(ONBOARDING_ID, "wrong data", "uid-1"))
                .thenReturn(Uni.createFrom().<Void>emitter(rejection::set));

        // when
        UniAssertSubscriber<Void> approved = tokenService.approveOnboarding(ONBOARDING_ID, "uid-1")
                .subscribe().withSubscriber(UniAssertSubscriber.create());
        UniAssertSubscriber<Void> rejected = tokenService.rejectOnboarding(ONBOARDING_ID, "wrong data", "uid-1")
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        // then
        approved.assertNotTerminated();
        rejected.assertNotTerminated();
        approval.get().complete(null);
        approved.assertCompleted();
        rejected.assertNotTerminated();
        rejection.get().complete(null);
        rejected.assertCompleted();
        verify(onboardingMsConnector).approveOnboarding(ONBOARDING_ID, "uid-1");
        verify(onboardingMsConnector).rejectOnboarding(ONBOARDING_ID, "wrong data", "uid-1");
    }

    @Test
    void approveAndReject_propagateTheDownstreamFailure() {
        // given
        ProcessingException failure = new ProcessingException("connection dropped");
        when(onboardingMsConnector.approveOnboarding(ONBOARDING_ID, "uid-1")).thenReturn(Uni.createFrom().failure(failure));
        when(onboardingMsConnector.rejectOnboarding(ONBOARDING_ID, "wrong data", "uid-1"))
                .thenReturn(Uni.createFrom().failure(failure));

        // when
        UniAssertSubscriber<Void> approved = tokenService.approveOnboarding(ONBOARDING_ID, "uid-1")
                .subscribe().withSubscriber(UniAssertSubscriber.create());
        UniAssertSubscriber<Void> rejected = tokenService.rejectOnboarding(ONBOARDING_ID, "wrong data", "uid-1")
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        // then
        assertSame(failure, approved.getFailure());
        assertSame(failure, rejected.getFailure());
    }

    @Test
    void completeOperations_delegateTheContractAndKeepTheirFailure() {
        // given
        ProcessingException failure = new ProcessingException("upload failed");
        when(onboardingMsConnector.onboardingTokenComplete(ONBOARDING_ID, FILE)).thenReturn(Uni.createFrom().voidItem());
        when(onboardingMsConnector.onboardingUsersComplete(ONBOARDING_ID, FILE)).thenReturn(Uni.createFrom().failure(failure));

        // when
        UniAssertSubscriber<Void> token = tokenService.completeTokenV2(ONBOARDING_ID, FILE)
                .subscribe().withSubscriber(UniAssertSubscriber.create());
        UniAssertSubscriber<Void> users = tokenService.completeOnboardingUsers(ONBOARDING_ID, FILE)
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        // then
        token.assertCompleted();
        assertSame(failure, users.getFailure());
        verify(onboardingMsConnector).onboardingTokenComplete(ONBOARDING_ID, FILE);
        verify(onboardingMsConnector).onboardingUsersComplete(ONBOARDING_ID, FILE);
    }

    @Test
    void documentReads_delegateToDocumentMs() {
        // given
        BinaryData contract = new BinaryData("contract.pdf", new byte[]{1});
        BinaryData signed = new BinaryData("signed.pdf", new byte[]{2});
        BinaryData attachment = new BinaryData("att.pdf", new byte[]{3});
        BinaryData csv = new BinaryData("aggregates.csv", new byte[]{4});
        AvailableDocuments available = new AvailableDocuments();
        when(documentMsClient.getContract(ONBOARDING_ID)).thenReturn(Uni.createFrom().item(contract));
        when(documentMsClient.getContractSigned(ONBOARDING_ID)).thenReturn(Uni.createFrom().item(signed));
        when(documentMsClient.getAttachment(ONBOARDING_ID, "att")).thenReturn(Uni.createFrom().item(attachment));
        when(documentMsClient.getAggregatesCsv(ONBOARDING_ID, PRODUCT_ID)).thenReturn(Uni.createFrom().item(csv));
        when(documentMsClient.getAvailableDocuments(ONBOARDING_ID)).thenReturn(Uni.createFrom().item(available));
        when(documentMsClient.headAttachment(ONBOARDING_ID, "att")).thenReturn(Uni.createFrom().item(204));

        // when
        BinaryData actualContract = tokenService.getContract(ONBOARDING_ID).await().indefinitely();
        BinaryData actualSigned = tokenService.getContractSigned(ONBOARDING_ID).await().indefinitely();
        BinaryData actualAttachment = tokenService.getAttachment(ONBOARDING_ID, "att").await().indefinitely();
        BinaryData actualCsv = tokenService.getAggregatesCsv(ONBOARDING_ID, PRODUCT_ID).await().indefinitely();
        AvailableDocuments actualAvailable = tokenService.getAvailableDocuments(ONBOARDING_ID).await().indefinitely();
        Integer actualStatus = tokenService.headAttachment(ONBOARDING_ID, "att").await().indefinitely();

        // then
        assertSame(contract, actualContract);
        assertSame(signed, actualSigned);
        assertSame(attachment, actualAttachment);
        assertSame(csv, actualCsv);
        assertSame(available, actualAvailable);
        assertEquals(204, actualStatus);
    }

    @Test
    void documentReads_waitForTheDownstreamAndKeepItsFailure() {
        // given
        AtomicReference<UniEmitter<? super Integer>> pendingHead = new AtomicReference<>();
        ResourceNotFoundException failure = new ResourceNotFoundException("missing document");
        when(documentMsClient.headAttachment(ONBOARDING_ID, "att")).thenReturn(Uni.createFrom().<Integer>emitter(pendingHead::set));
        when(documentMsClient.getContract(ONBOARDING_ID)).thenReturn(Uni.createFrom().failure(failure));
        when(documentMsClient.getAvailableDocuments(ONBOARDING_ID)).thenReturn(Uni.createFrom().failure(failure));

        // when
        UniAssertSubscriber<Integer> head = tokenService.headAttachment(ONBOARDING_ID, "att")
                .subscribe().withSubscriber(UniAssertSubscriber.create());
        UniAssertSubscriber<BinaryData> contract = tokenService.getContract(ONBOARDING_ID)
                .subscribe().withSubscriber(UniAssertSubscriber.create());
        UniAssertSubscriber<AvailableDocuments> available = tokenService.getAvailableDocuments(ONBOARDING_ID)
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        // then
        head.assertNotTerminated();
        pendingHead.get().complete(404);
        head.assertCompleted().assertItem(404);
        assertSame(failure, contract.getFailure());
        assertSame(failure, available.getFailure());
    }

    @Test
    void requiredArgumentsAreChecked() {
        // given
        // when
        assertThrows(NullPointerException.class, () -> tokenService.verifyOnboarding(null).await().indefinitely());
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
        // then
        verifyNoInteractions(onboardingMsConnector, documentMsClient, productService);
    }

    @Test
    void getTemplateAttachment_resolvesTheTemplatePathFromTheProduct() {
        // given
        OnboardingData onboarding = onboarding();
        when(onboardingMsConnector.getOnboarding(ONBOARDING_ID)).thenReturn(Uni.createFrom().item(new OnboardingGet()));
        when(onboardingMapper.toOnboardingData(any(OnboardingGet.class))).thenReturn(onboarding);
        when(productService.getProductValid(PRODUCT_ID)).thenReturn(Uni.createFrom().item(product("Allegato 1", "template/path")));
        BinaryData expected = new BinaryData("template.pdf", new byte[]{1});
        when(documentMsClient.getTemplateAttachment(ONBOARDING_ID, "Comune di Test", "Allegato 1", PRODUCT_ID, "template/path"))
                .thenReturn(Uni.createFrom().item(expected));

        // when
        var actualAsync1 = tokenService.getTemplateAttachment(ONBOARDING_ID, "Allegato 1").await().indefinitely();

        // then
        assertSame(expected, actualAsync1);
    }

    @Test
    void getTemplateAttachment_unknownAttachmentIsNotFound() {
        // given
        when(onboardingMsConnector.getOnboarding(ONBOARDING_ID)).thenReturn(Uni.createFrom().item(new OnboardingGet()));
        when(onboardingMapper.toOnboardingData(any(OnboardingGet.class))).thenReturn(onboarding());
        when(productService.getProductValid(PRODUCT_ID)).thenReturn(Uni.createFrom().item(product("Allegato 1", "template/path")));

        // when
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
                () -> tokenService.getTemplateAttachment(ONBOARDING_ID, "Missing").await().indefinitely());

        // then
        assertEquals("Attachment with name Missing not found", exception.getMessage());
        verifyNoInteractions(documentMsClient);
    }

    @Test
    void uploadAttachment_systemStorageUsesTheProductTemplate() {
        // given
        OnboardingData onboarding = onboarding();
        when(onboardingMsConnector.getOnboarding(ONBOARDING_ID)).thenReturn(Uni.createFrom().item(new OnboardingGet()));
        when(onboardingMapper.toOnboardingData(any(OnboardingGet.class))).thenReturn(onboarding);
        when(productService.getRequiredDocuments("AR", PRODUCT_ID, "PA", "IPA")).thenReturn(Uni.createFrom().item(List.of()));
        when(productService.getProductValid(PRODUCT_ID)).thenReturn(Uni.createFrom().item(product("Allegato 1", "template/path")));

        when(documentMsClient.uploadAttachment(eq(ONBOARDING_ID), eq(FILE), eq("Allegato 1"), eq(PRODUCT_ID), any()))
                .thenReturn(Uni.createFrom().voidItem());

        // when
        tokenService.uploadAttachment("AR", ONBOARDING_ID, FILE, "Allegato 1", null, null).await().indefinitely();

        // then
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
        // given
        OnboardingData onboarding = onboarding();
        when(onboardingMsConnector.getOnboarding(ONBOARDING_ID)).thenReturn(Uni.createFrom().item(new OnboardingGet()));
        when(onboardingMapper.toOnboardingData(any(OnboardingGet.class))).thenReturn(onboarding);
        RequiredDocumentModel required = new RequiredDocumentModel();
        required.setId("statuto");
        required.setStorageOrigin(StorageOrigin.USER);
        required.setMaxDocumentsRequired(3);
        RequiredDocumentModel other = new RequiredDocumentModel();
        other.setId("other");
        other.setStorageOrigin(StorageOrigin.SYSTEM);
        when(productService.getRequiredDocuments("AR", PRODUCT_ID, "PA", "IPA")).thenReturn(Uni.createFrom().item(List.of(other, required)));
        when(documentMsClient.uploadUserAttachment(ONBOARDING_ID, FILE, PRODUCT_ID, "statuto", "descrizione", "Statuto", 3))
                .thenReturn(Uni.createFrom().voidItem());

        // when
        tokenService.uploadAttachment("AR", ONBOARDING_ID, FILE, "Statuto", "statuto", "descrizione").await().indefinitely();

        // then
        verify(documentMsClient).uploadUserAttachment(ONBOARDING_ID, FILE, PRODUCT_ID, "statuto", "descrizione", "Statuto", 3);
        verify(documentMsClient, never()).uploadAttachment(any(), any(), any(), any(), any());
        verify(productService, never()).getProductValid(any());
    }

    @Test
    void uploadAttachment_requiresIdNameAndFile() {
        // given
        // when
        assertThrows(NullPointerException.class, () -> tokenService.uploadAttachment("AR", null, FILE, "n", null, null).await().indefinitely());
        assertThrows(NullPointerException.class, () -> tokenService.uploadAttachment("AR", ONBOARDING_ID, FILE, null, null, null).await().indefinitely());
        assertThrows(NullPointerException.class, () -> tokenService.uploadAttachment("AR", ONBOARDING_ID, null, "n", null, null).await().indefinitely());
        // then
        verifyNoInteractions(onboardingMsConnector, documentMsClient, productService);
    }

    @Test
    @Timeout(value = 2, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
    void systemUploadWaitsForOnboardingThenDocumentsThenProduct() {
        // given
        AtomicReference<UniEmitter<? super OnboardingGet>> onboarding = new AtomicReference<>();
        AtomicReference<UniEmitter<? super List<RequiredDocumentModel>>> documents = new AtomicReference<>();
        AtomicReference<UniEmitter<? super Product>> product = new AtomicReference<>();
        OnboardingGet downstream = new OnboardingGet();
        when(onboardingMsConnector.getOnboarding(ONBOARDING_ID))
                .thenReturn(Uni.createFrom().<OnboardingGet>emitter(onboarding::set));
        when(onboardingMapper.toOnboardingData(downstream)).thenReturn(onboarding());
        when(productService.getRequiredDocuments("AR", PRODUCT_ID, "PA", "IPA"))
                .thenReturn(Uni.createFrom().<List<RequiredDocumentModel>>emitter(documents::set));
        when(productService.getProductValid(PRODUCT_ID))
                .thenReturn(Uni.createFrom().<Product>emitter(product::set));
        when(documentMsClient.uploadAttachment(eq(ONBOARDING_ID), eq(FILE), eq("Allegato 1"), eq(PRODUCT_ID), any()))
                .thenReturn(Uni.createFrom().voidItem());

        // when
        UniAssertSubscriber<Void> result = tokenService.uploadAttachment("AR", ONBOARDING_ID, FILE, "Allegato 1", null, null)
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        // then
        result.assertNotTerminated();
        verifyNoInteractions(productService, documentMsClient);
        onboarding.get().complete(downstream);
        verify(productService).getRequiredDocuments("AR", PRODUCT_ID, "PA", "IPA");
        verify(productService, never()).getProductValid(any());
        documents.get().complete(List.of());
        verify(productService).getProductValid(PRODUCT_ID);
        verifyNoInteractions(documentMsClient);
        product.get().complete(product("Allegato 1", "template/path"));
        result.assertCompleted();
        verify(documentMsClient, never()).uploadUserAttachment(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void userUploadKeepsTheDefaultLimitWithoutLookingUpAValidProduct() {
        // given
        OnboardingGet downstream = new OnboardingGet();
        RequiredDocumentModel document = new RequiredDocumentModel();
        document.setId("statuto");
        document.setStorageOrigin(StorageOrigin.USER);
        when(onboardingMsConnector.getOnboarding(ONBOARDING_ID)).thenReturn(Uni.createFrom().item(downstream));
        when(onboardingMapper.toOnboardingData(downstream)).thenReturn(onboarding());
        when(productService.getRequiredDocuments("PNPG", PRODUCT_ID, "PA", "IPA"))
                .thenReturn(Uni.createFrom().item(List.of(document)));
        when(documentMsClient.uploadUserAttachment(ONBOARDING_ID, FILE, PRODUCT_ID, "statuto", null, "Statuto", 1))
                .thenReturn(Uni.createFrom().voidItem());

        // when
        tokenService.uploadAttachment("PNPG", ONBOARDING_ID, FILE, "Statuto", "statuto", null).await().indefinitely();

        // then
        verify(documentMsClient).uploadUserAttachment(ONBOARDING_ID, FILE, PRODUCT_ID, "statuto", null, "Statuto", 1);
        verify(productService, never()).getProductValid(any());
        verify(documentMsClient, never()).uploadAttachment(any(), any(), any(), any(), any());
    }

    @Test
    void documentLookupFailureStopsTheUploadWithoutRetry() {
        // given
        OnboardingGet downstream = new OnboardingGet();
        ResourceNotFoundException failure = new ResourceNotFoundException("missing required documents");
        when(onboardingMsConnector.getOnboarding(ONBOARDING_ID)).thenReturn(Uni.createFrom().item(downstream));
        when(onboardingMapper.toOnboardingData(downstream)).thenReturn(onboarding());
        when(productService.getRequiredDocuments("AR", PRODUCT_ID, "PA", "IPA"))
                .thenReturn(Uni.createFrom().failure(failure));

        // when
        UniAssertSubscriber<Void> result = tokenService.uploadAttachment("AR", ONBOARDING_ID, FILE, "Allegato 1", null, null)
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        // then
        assertSame(failure, result.getFailure());
        verify(productService, never()).getProductValid(any());
        verifyNoInteractions(documentMsClient);
        verify(productService).getRequiredDocuments("AR", PRODUCT_ID, "PA", "IPA");
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
