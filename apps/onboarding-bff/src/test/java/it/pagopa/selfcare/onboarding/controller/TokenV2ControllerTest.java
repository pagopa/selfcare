package it.pagopa.selfcare.onboarding.controller;

import it.pagopa.selfcare.onboarding.model.dto.request.DownloadDocumentType;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.quarkus.security.identity.SecurityIdentity;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.test.UniAssertSubscriber;
import io.smallrye.mutiny.subscription.UniEmitter;
import it.pagopa.selfcare.onboarding.client.model.AvailableDocuments;
import it.pagopa.selfcare.onboarding.client.model.BinaryData;
import it.pagopa.selfcare.onboarding.client.model.OnboardingData;
import it.pagopa.selfcare.onboarding.client.model.UploadedFile;
import it.pagopa.selfcare.onboarding.exception.AccessDeniedException;
import it.pagopa.selfcare.onboarding.exception.InvalidRequestException;
import it.pagopa.selfcare.onboarding.mapper.OnboardingMapper;
import it.pagopa.selfcare.onboarding.model.OnboardingVerify;
import it.pagopa.selfcare.onboarding.model.dto.request.ReasonForRejectDto;
import it.pagopa.selfcare.onboarding.model.dto.response.AvailableDocumentsResource;
import it.pagopa.selfcare.onboarding.model.dto.response.OnboardingRequestResource;
import it.pagopa.selfcare.onboarding.security.AuthorizationService;
import it.pagopa.selfcare.onboarding.service.TokenService;
import it.pagopa.selfcare.onboarding.util.PermissionConstants;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;
import org.jboss.resteasy.reactive.multipart.FileUpload;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TokenV2ControllerTest {

    private static final String VIEW_PAGE = PermissionConstants.SELC_VIEW_ACCOUNT_PAGE;
    private static final String VIEW_DOCUMENTS = PermissionConstants.SELC_VIEW_ACCOUNT_DOCUMENTS;
    private static final String MANAGE_PAGE = PermissionConstants.SELC_MANAGE_ACCOUNT_PAGE;
    private static final String EXPOSE_HEADERS = "Access-Control-Expose-Headers";
    private static final Duration TIMEOUT = Duration.ofSeconds(2);

    @TempDir
    Path folder;

    @Mock
    AuthorizationService authorizationService;
    @Mock
    TokenService tokenService;
    @Mock
    OnboardingMapper onboardingMapper;
    @Mock
    SecurityIdentity securityIdentity;

    TokenV2Controller controller;

    /** A guarded endpoint: its permission, how its action is stubbed, invoked and verified. */
    private record GuardedCall(String name, String permission, Consumer<TokenV2ControllerTest> stub,
                               Function<TokenV2Controller, Uni<?>> call, Consumer<TokenV2ControllerTest> verifyAction) {
        @Override
        public String toString() {
            return name;
        }
    }

    static Stream<GuardedCall> guardedCalls() {
        return Stream.of(
                new GuardedCall("retrieve", VIEW_PAGE,
                        t -> {
                            when(t.tokenService.getOnboardingWithUserInfo("42")).thenReturn(Uni.createFrom().item(new OnboardingData()));
                            when(t.onboardingMapper.toOnboardingRequestResource(any(OnboardingData.class)))
                                    .thenReturn(new OnboardingRequestResource());
                        },
                        c -> c.retrieveOnboardingRequest("42"),
                        t -> verify(t.tokenService).getOnboardingWithUserInfo("42")),
                new GuardedCall("approve", MANAGE_PAGE,
                        t -> when(t.tokenService.approveOnboarding("42", "test-uid")).thenReturn(Uni.createFrom().voidItem()),
                        c -> c.approveOnboarding("42"),
                        t -> verify(t.tokenService).approveOnboarding("42", "test-uid")),
                new GuardedCall("reject", MANAGE_PAGE,
                        t -> when(t.tokenService.rejectOnboarding("42", "reason", "test-uid")).thenReturn(Uni.createFrom().voidItem()),
                        c -> {
                            ReasonForRejectDto request = new ReasonForRejectDto();
                            request.setReason("reason");
                            return c.rejectOnboarding("42", request);
                        },
                        t -> verify(t.tokenService).rejectOnboarding("42", "reason", "test-uid")),
                new GuardedCall("backstage contract", VIEW_DOCUMENTS,
                        t -> when(t.tokenService.getContract("42")).thenReturn(Uni.createFrom().item(new BinaryData("c.pdf", new byte[0]))),
                        c -> c.getContractBackstage("42"),
                        t -> verify(t.tokenService).getContract("42")),
                new GuardedCall("available documents", VIEW_DOCUMENTS,
                        t -> when(t.tokenService.getAvailableDocuments("42")).thenReturn(Uni.createFrom().item(new AvailableDocuments())),
                        c -> c.getAvailableDocuments("42"),
                        t -> verify(t.tokenService).getAvailableDocuments("42")),
                new GuardedCall("download", VIEW_DOCUMENTS,
                        t -> when(t.tokenService.downloadDocument("42", DownloadDocumentType.CONTRACT_SIGNED, null))
                                .thenReturn(Uni.createFrom().item(new BinaryData("s.pdf", new byte[0]))),
                        c -> c.downloadDocument("42", downloadQuery("CONTRACT_SIGNED", null)),
                        t -> verify(t.tokenService).downloadDocument("42", DownloadDocumentType.CONTRACT_SIGNED, null)),
                new GuardedCall("aggregates csv", VIEW_DOCUMENTS,
                        t -> when(t.tokenService.getAggregatesCsv("42", "prod-io")).thenReturn(Uni.createFrom().item(new BinaryData("a.csv", new byte[0]))),
                        c -> c.getAggregatesCsv("42", "prod-io"),
                        t -> verify(t.tokenService).getAggregatesCsv("42", "prod-io")));
    }

    private static UriInfo downloadQuery(String type, String name) {
        var uriInfo = mock(UriInfo.class);
        var query = new MultivaluedHashMap<String, String>();
        if (type != null) {
            query.putSingle("type", type);
        }
        if (name != null) {
            query.putSingle("name", name);
        }
        when(uriInfo.getQueryParameters()).thenReturn(query);
        return uriInfo;
    }

    @BeforeEach
    void setUp() {
        controller = new TokenV2Controller(tokenService, onboardingMapper);
        controller.securityIdentity = securityIdentity;
        controller.authorizationService = authorizationService;
    }

    private static UniAssertSubscriber<Object> subscribe(Uni<?> operation) {
        return operation.map(item -> (Object) item).subscribe().withSubscriber(UniAssertSubscriber.create());
    }

    private void givenPermission(String permission, boolean allowed) {
        when(authorizationService.hasPermission(securityIdentity, "42", permission))
                .thenReturn(Uni.createFrom().item(allowed));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("guardedCalls")
    void guardedActionStartsOnlyAfterThePendingPermissionAllowsIt(GuardedCall guarded) {
        // given
        AtomicReference<UniEmitter<? super Boolean>> pending = new AtomicReference<>();
        when(authorizationService.hasPermission(securityIdentity, "42", guarded.permission()))
                .thenReturn(Uni.createFrom().<Boolean>emitter(pending::set));
        lenient().when(securityIdentity.getAttribute("uid")).thenReturn("test-uid");
        guarded.stub().accept(this);

        // when
        Uni<?> operation = guarded.call().apply(controller);
        verify(authorizationService).hasPermission(securityIdentity, "42", guarded.permission());
        UniAssertSubscriber<Object> result = subscribe(operation);

        // then
        result.assertNotTerminated();
        verifyNoInteractions(tokenService);
        pending.get().complete(Boolean.TRUE);
        result.assertCompleted();
        guarded.verifyAction().accept(this);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("guardedCalls")
    void guardedActionIsNeverStartedWhenThePermissionIsDenied(GuardedCall guarded) {
        // given
        when(authorizationService.hasPermission(securityIdentity, "42", guarded.permission()))
                .thenReturn(Uni.createFrom().item(Boolean.FALSE));
        lenient().when(securityIdentity.getAttribute("uid")).thenReturn("test-uid");

        // when
        UniAssertSubscriber<Object> result = subscribe(guarded.call().apply(controller));

        // then
        assertEquals("Access Denied", ((AccessDeniedException) result.getFailure()).getMessage());
        verifyNoInteractions(tokenService, onboardingMapper);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("guardedCalls")
    void guardedActionIsNeverStartedWhenThePermissionLookupFails(GuardedCall guarded) {
        // given
        ProcessingException failure = new ProcessingException("iam unavailable");
        when(authorizationService.hasPermission(securityIdentity, "42", guarded.permission()))
                .thenReturn(Uni.createFrom().failure(failure));
        lenient().when(securityIdentity.getAttribute("uid")).thenReturn("test-uid");

        // when
        UniAssertSubscriber<Object> result = subscribe(guarded.call().apply(controller));

        // then
        assertSame(failure, result.getFailure());
        verifyNoInteractions(tokenService, onboardingMapper);
    }

    @Test
    void verifyOnboarding_returnsMappedResult() {
        // given
        OnboardingData onboardingData = new OnboardingData();
        OnboardingVerify expected = new OnboardingVerify();
        when(tokenService.verifyOnboarding("42")).thenReturn(Uni.createFrom().item(onboardingData));
        when(onboardingMapper.toOnboardingVerify(onboardingData)).thenReturn(expected);

        // when
        var actualAsync1 = controller.verifyOnboarding("42").await().indefinitely();

        // then
        assertSame(expected, actualAsync1);
    }

    @Test
    void retrieveOnboardingRequest_checksViewAccountPageThenMapsTheResult() {
        // given
        OnboardingData onboardingData = new OnboardingData();
        OnboardingRequestResource expected = new OnboardingRequestResource();
        givenPermission(VIEW_PAGE, true);
        when(tokenService.getOnboardingWithUserInfo("42")).thenReturn(Uni.createFrom().item(onboardingData));
        when(onboardingMapper.toOnboardingRequestResource(onboardingData)).thenReturn(expected);

        // when
        OnboardingRequestResource result = controller.retrieveOnboardingRequest("42").await().indefinitely();

        // then
        assertSame(expected, result);
    }

    @Test
    void retrieveOnboardingRequest_deniedIsAccessDeniedWithoutReadingTheOnboarding() {
        // given
        givenPermission(VIEW_PAGE, false);

        // when
        AccessDeniedException e = assertThrows(AccessDeniedException.class,
                () -> controller.retrieveOnboardingRequest("42").await().indefinitely());

        // then
        assertEquals("Access Denied", e.getMessage());
        verifyNoInteractions(tokenService);
    }

    @Test
    void approveOnboarding_requiresManageAccountPageAndForwardsTheCallerUid() {
        // given
        givenPermission(MANAGE_PAGE, true);
        when(securityIdentity.getAttribute("uid")).thenReturn("test-uid");
        when(tokenService.approveOnboarding("42", "test-uid")).thenReturn(Uni.createFrom().voidItem());

        // when
        Response response = controller.approveOnboarding("42").await().indefinitely();

        // then
        assertEquals(200, response.getStatus());
        assertNull(response.getEntity());
        verify(tokenService).approveOnboarding("42", "test-uid");
    }

    @Test
    void approveOnboarding_completesOnlyWhenTheServiceCompletes() {
        // given
        AtomicReference<UniEmitter<? super Void>> pending = new AtomicReference<>();
        givenPermission(MANAGE_PAGE, true);
        when(securityIdentity.getAttribute("uid")).thenReturn("test-uid");
        when(tokenService.approveOnboarding("42", "test-uid")).thenReturn(Uni.createFrom().<Void>emitter(pending::set));

        // when
        UniAssertSubscriber<Response> result = controller.approveOnboarding("42")
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        // then
        result.assertNotTerminated();
        pending.get().complete(null);
        assertEquals(200, result.assertCompleted().getItem().getStatus());
    }

    @Test
    void approveOnboarding_deniedIsAccessDenied() {
        // given
        givenPermission(MANAGE_PAGE, false);

        // when
        assertThrows(AccessDeniedException.class, () -> controller.approveOnboarding("42").await().indefinitely());

        // then
        verifyNoInteractions(tokenService);
    }

    @Test
    void rejectOnboarding_requiresManageAccountPageAndForwardsTheReason() {
        // given
        ReasonForRejectDto request = new ReasonForRejectDto();
        request.setReason("reason");
        givenPermission(MANAGE_PAGE, true);
        when(securityIdentity.getAttribute("uid")).thenReturn("test-uid");
        when(tokenService.rejectOnboarding("42", "reason", "test-uid")).thenReturn(Uni.createFrom().voidItem());

        // when
        Response response = controller.rejectOnboarding("42", request).await().indefinitely();

        // then
        assertEquals(200, response.getStatus());
        verify(tokenService).rejectOnboarding("42", "reason", "test-uid");
    }

    @Test
    void rejectOnboarding_deniedIsAccessDenied() {
        // given
        ReasonForRejectDto request = new ReasonForRejectDto();
        request.setReason("reason");
        givenPermission(MANAGE_PAGE, false);

        // when
        assertThrows(AccessDeniedException.class, () -> controller.rejectOnboarding("42", request).await().indefinitely());

        // then
        verifyNoInteractions(tokenService);
    }

    @Test
    void rejectOnboarding_missingBodyIsABadRequestBeforeAnyPermissionCheck() {
        // given
        // when
        assertThrows(InvalidRequestException.class, () -> controller.rejectOnboarding("42", null));

        // then
        verifyNoInteractions(authorizationService, tokenService);
    }

    @Test
    void deleteOnboarding_rejectsAsUserWithoutIamCheck() {
        // given
        when(securityIdentity.getAttribute("uid")).thenReturn("test-uid");
        when(tokenService.rejectOnboarding("42", "REJECTED_BY_USER", "test-uid")).thenReturn(Uni.createFrom().voidItem());

        // when
        Response response = controller.deleteOnboarding("42").await().indefinitely();

        // then
        assertEquals(Response.Status.NO_CONTENT.getStatusCode(), response.getStatus());
        verify(tokenService).rejectOnboarding("42", "REJECTED_BY_USER", "test-uid");
        verifyNoInteractions(authorizationService);
    }

    @Test
    void getContract_skipsIamAndExposesTheFileName() {
        // given
        when(tokenService.getContract("42")).thenReturn(Uni.createFrom().item(new BinaryData("contract.pdf", "content".getBytes())));

        // when
        Response response = controller.getContract("42").await().indefinitely();

        // then
        assertBinary(response, "contract.pdf", "content".getBytes());
        verify(tokenService).getContract("42");
        verifyNoInteractions(authorizationService, securityIdentity);
    }

    @Test
    void getContractBackstage_requiresViewAccountDocumentsAndExposesTheFileName() {
        // given
        givenPermission(VIEW_DOCUMENTS, true);
        when(tokenService.getContract("42")).thenReturn(Uni.createFrom().item(new BinaryData("contract.pdf", "content".getBytes())));

        // when
        Response response = controller.getContractBackstage("42").await().indefinitely();

        // then
        assertBinary(response, "contract.pdf", "content".getBytes());
        verify(authorizationService).hasPermission(securityIdentity, "42", VIEW_DOCUMENTS);
    }

    @Test
    void getContractBackstage_deniedIsAccessDenied() {
        // given
        givenPermission(VIEW_DOCUMENTS, false);

        // when
        assertThrows(AccessDeniedException.class, () -> controller.getContractBackstage("42").await().indefinitely());

        // then
        verifyNoInteractions(tokenService);
    }

    @Test
    void getTemplateAttachment_requiresAttachmentNameAndSkipsIam() {
        // given
        // when
        assertThrows(InvalidRequestException.class, () -> controller.getTemplateAttachment("42", null).await().indefinitely());
        // then
        verifyNoInteractions(tokenService, authorizationService);
    }

    @Test
    void getTemplateAttachment_returnsTheBinary() {
        // given
        when(tokenService.getTemplateAttachment("42", "template.pdf"))
                .thenReturn(Uni.createFrom().item(new BinaryData("template.pdf", "content".getBytes())));

        // when
        Response response = controller.getTemplateAttachment("42", "template.pdf").await().indefinitely();

        // then
        assertBinary(response, "template.pdf", "content".getBytes());
        verifyNoInteractions(authorizationService);
    }

    @Test
    void getAttachment_requiresNameAndSkipsIam() {
        // given
        // when
        assertThrows(InvalidRequestException.class, () -> controller.getAttachment("42", null));

        // then
        verifyNoInteractions(tokenService, authorizationService);
    }

    @Test
    void getAttachment_returnsTheBinaryWithoutIam() {
        // given
        when(tokenService.getAttachment("42", "doc.pdf")).thenReturn(Uni.createFrom().item(new BinaryData("doc.pdf", new byte[] {1, 2})));

        // when
        Response response = controller.getAttachment("42", "doc.pdf").await().indefinitely();

        // then
        assertBinary(response, "doc.pdf", new byte[] {1, 2});
        verifyNoInteractions(authorizationService);
    }

    @Test
    void getAvailableDocuments_requiresViewAccountDocuments() {
        // given
        AvailableDocuments source = new AvailableDocuments();
        source.setAttachments(List.of("doc1.pdf"));
        source.setContractFilename("contract.pdf");
        givenPermission(VIEW_DOCUMENTS, true);
        when(tokenService.getAvailableDocuments("42")).thenReturn(Uni.createFrom().item(source));

        // when
        AvailableDocumentsResource result = controller.getAvailableDocuments("42").await().indefinitely();

        // then
        assertEquals(List.of("doc1.pdf"), result.getAttachments());
        assertEquals("contract.pdf", result.getContractFilename());
    }

    @Test
    void getAvailableDocuments_deniedIsAccessDenied() {
        // given
        givenPermission(VIEW_DOCUMENTS, false);

        // when
        assertThrows(AccessDeniedException.class, () -> controller.getAvailableDocuments("42").await().indefinitely());

        // then
        verifyNoInteractions(tokenService);
    }

    @Test
    void getAggregatesCsv_requiresViewAccountDocuments() {
        // given
        givenPermission(VIEW_DOCUMENTS, true);
        when(tokenService.getAggregatesCsv("42", "prod-io"))
                .thenReturn(Uni.createFrom().item(new BinaryData("agg.csv", "a;b".getBytes())));

        // when
        Response response = controller.getAggregatesCsv("42", "prod-io").await().indefinitely();

        // then
        assertBinary(response, "agg.csv", "a;b".getBytes());
    }

    @Test
    void getAggregatesCsv_deniedIsAccessDenied() {
        // given
        givenPermission(VIEW_DOCUMENTS, false);

        // when
        assertThrows(AccessDeniedException.class, () -> controller.getAggregatesCsv("42", "prod-io").await().indefinitely());

        // then
        verifyNoInteractions(tokenService);
    }

    @Test
    void downloadDocument_signedContract() {
        // given
        givenPermission(VIEW_DOCUMENTS, true);
        when(tokenService.downloadDocument("42", DownloadDocumentType.CONTRACT_SIGNED, null))
                .thenReturn(Uni.createFrom().item(new BinaryData("signed.pdf", "x".getBytes())));

        // when
        Response response = controller.downloadDocument("42", downloadQuery("CONTRACT_SIGNED", null)).await().indefinitely();

        // then
        assertBinary(response, "signed.pdf", "x".getBytes());
        verify(tokenService, never()).getAttachment(any(), any());
    }

    @Test
    void downloadDocument_attachment() {
        // given
        givenPermission(VIEW_DOCUMENTS, true);
        when(tokenService.downloadDocument("42", DownloadDocumentType.ATTACHMENT, "doc.pdf"))
                .thenReturn(Uni.createFrom().item(new BinaryData("doc.pdf", "x".getBytes())));

        // when
        Response response = controller.downloadDocument("42", downloadQuery("ATTACHMENT", "doc.pdf")).await().indefinitely();

        // then
        assertBinary(response, "doc.pdf", "x".getBytes());
    }

    @Test
    void downloadDocument_attachmentWithoutNameIsABadRequestAfterTheIamCheck() {
        // given
        givenPermission(VIEW_DOCUMENTS, true);
        InvalidRequestException missingName = new InvalidRequestException("Query parameter 'name' is required when type=ATTACHMENT");
        when(tokenService.downloadDocument("42", DownloadDocumentType.ATTACHMENT, null)).thenThrow(missingName);
        when(tokenService.downloadDocument("42", DownloadDocumentType.ATTACHMENT, "  ")).thenThrow(missingName);

        // when
        InvalidRequestException missing = assertThrows(InvalidRequestException.class,
                () -> controller.downloadDocument("42", downloadQuery("ATTACHMENT", null)).await().indefinitely());
        InvalidRequestException blank = assertThrows(InvalidRequestException.class,
                () -> controller.downloadDocument("42", downloadQuery("ATTACHMENT", "  ")).await().indefinitely());

        // then
        assertEquals("Query parameter 'name' is required when type=ATTACHMENT", missing.getMessage());
        assertEquals(missing.getMessage(), blank.getMessage());
        verify(authorizationService, times(2)).hasPermission(securityIdentity, "42", VIEW_DOCUMENTS);
        verify(tokenService).downloadDocument("42", DownloadDocumentType.ATTACHMENT, null);
        verify(tokenService).downloadDocument("42", DownloadDocumentType.ATTACHMENT, "  ");
    }

    @Test
    void downloadDocument_missingOrUnknownTypeIsABadRequestWithoutAnyDownstreamCall() {
        // given
        // when
        assertThrows(InvalidRequestException.class, () -> controller.downloadDocument("42", downloadQuery(null, null)));
        assertThrows(InvalidRequestException.class, () -> controller.downloadDocument("42", downloadQuery("OTHER", "doc.pdf")));

        // then
        verifyNoInteractions(authorizationService, tokenService);
    }

    @Test
    void downloadDocument_deniedIsAccessDenied() {
        // given
        givenPermission(VIEW_DOCUMENTS, false);

        // when
        assertThrows(AccessDeniedException.class,
                () -> controller.downloadDocument("42", downloadQuery("CONTRACT_SIGNED", null)).await().indefinitely());

        // then
        verifyNoInteractions(tokenService);
    }

    @Test
    void attachmentStatus_isNoContentOn2xxAndNotFoundOtherwise() {
        // given
        when(tokenService.headAttachment("42", "ok.pdf")).thenReturn(Uni.createFrom().item(200));
        when(tokenService.headAttachment("42", "gone.pdf")).thenReturn(Uni.createFrom().item(404));
        when(tokenService.headAttachment("42", "err.pdf")).thenReturn(Uni.createFrom().item(500));

        // when
        int getOk = controller.getAttachmentStatus("42", "ok.pdf").await().indefinitely().getStatus();
        int headOk = controller.headAttachment("42", "ok.pdf").await().indefinitely().getStatus();
        int getGone = controller.getAttachmentStatus("42", "gone.pdf").await().indefinitely().getStatus();
        int headError = controller.headAttachment("42", "err.pdf").await().indefinitely().getStatus();

        // then
        assertEquals(204, getOk);
        assertEquals(204, headOk);
        assertEquals(404, getGone);
        assertEquals(404, headError);
        verifyNoInteractions(authorizationService);
    }

    @Test
    void attachmentStatus_waitsForTheServiceAndKeepsItsFailure() {
        // given
        AtomicReference<UniEmitter<? super Integer>> pending = new AtomicReference<>();
        ProcessingException failure = new ProcessingException("document unavailable");
        when(tokenService.headAttachment("42", "slow.pdf")).thenReturn(Uni.createFrom().<Integer>emitter(pending::set));
        when(tokenService.headAttachment("42", "broken.pdf")).thenReturn(Uni.createFrom().failure(failure));

        // when
        UniAssertSubscriber<Response> slow = controller.headAttachment("42", "slow.pdf")
                .subscribe().withSubscriber(UniAssertSubscriber.create());
        UniAssertSubscriber<Response> broken = controller.getAttachmentStatus("42", "broken.pdf")
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        // then
        slow.assertNotTerminated();
        pending.get().complete(204);
        assertEquals(204, slow.assertCompleted().getItem().getStatus());
        assertSame(failure, broken.getFailure());
    }

    @Test
    void attachmentStatus_requiresName() {
        // given
        // when
        assertThrows(InvalidRequestException.class, () -> controller.getAttachmentStatus("42", null));
        assertThrows(InvalidRequestException.class, () -> controller.headAttachment("42", null));

        // then
        verifyNoInteractions(tokenService);
    }

    @Test
    void uploadAttachment_requiresAttachmentNameBeforeAnyWork() {
        // given
        FileUpload upload = mock(FileUpload.class);

        // when
        assertThrows(InvalidRequestException.class,
                () -> controller.uploadAttachment("42", null, null, null, upload, "tenant").await().indefinitely());
        // then
        verifyNoInteractions(tokenService, upload);
    }

    @Test
    void uploadAttachment_requiresTheFilePart() {
        // given
        // when
        InvalidRequestException e = assertThrows(InvalidRequestException.class,
                () -> controller.uploadAttachment("42", "att", null, null, null, "tenant").await().indefinitely());

        // then
        assertEquals("Required part 'attachment' is not present.", e.getMessage());
        verifyNoInteractions(tokenService);
    }

    @Test
    void uploadAttachment_requiresATenant() throws IOException {
        // given
        FileUpload upload = upload(file("contract.pdf", "pdf-content"), "contract.pdf", "application/pdf");

        // when
        InvalidRequestException blank = assertThrows(InvalidRequestException.class,
                () -> controller.uploadAttachment("42", "att", null, null, upload, " ").await().atMost(TIMEOUT));
        InvalidRequestException missing = assertThrows(InvalidRequestException.class,
                () -> controller.uploadAttachment("42", "att", null, null, upload, null).await().atMost(TIMEOUT));

        // then
        assertEquals("Tenant context is required", blank.getMessage());
        assertEquals("Tenant context is required", missing.getMessage());
        verifyNoInteractions(tokenService);
    }

    @Test
    void uploadAttachment_rejectsFormatsOtherThanPdfAndP7mBeforeTheTenantCheck() throws IOException {
        // given
        FileUpload upload = upload(file("contract.txt", "text"), "contract.txt", "text/plain");

        // when
        InvalidRequestException e = assertThrows(InvalidRequestException.class,
                () -> controller.uploadAttachment("42", "att", null, null, upload, null).await().atMost(TIMEOUT));

        // then
        assertEquals("Formato file non supportato. Ammessi: [.pdf, .p7m]", e.getMessage());
        verifyNoInteractions(tokenService);
    }

    @Test
    void uploadAttachment_emptyFileIsRejectedBeforeFormatAndTenantChecks() throws IOException {
        // given
        FileUpload upload = upload(file("contract.txt", ""), "contract.txt", "text/plain");

        // when
        InvalidRequestException e = assertThrows(InvalidRequestException.class,
                () -> controller.uploadAttachment("42", "att", null, null, upload, null).await().atMost(TIMEOUT));

        // then
        assertEquals("Il file è vuoto o mancante.", e.getMessage());
        verifyNoInteractions(tokenService);
    }

    @Test
    void uploadAttachment_unreadableFileFailsOnSubscriptionBeforeEveryOtherValidation() {
        // given an unreadable temporary file, an unsupported format and no tenant
        FileUpload upload = upload(folder.resolve("missing.tmp"), "contract.txt", "text/plain");

        // when the Uni is built and then subscribed
        Uni<Response> operation = controller.uploadAttachment("42", "att", null, null, upload, null);
        IllegalStateException e = assertThrows(IllegalStateException.class, () -> operation.await().atMost(TIMEOUT));

        // then
        assertEquals("Cannot read uploaded file", e.getMessage());
        verifyNoInteractions(tokenService);
    }

    @Test
    void uploadAttachment_readsTheFileOnAWorkerAtSubscriptionAndForwardsTenantNameAndMultipartFields() throws IOException {
        // given
        Thread caller = Thread.currentThread();
        AtomicReference<Thread> serviceThread = new AtomicReference<>();
        Path path = file("contract.pdf", "content-at-call");
        FileUpload upload = upload(path, "contract.pdf", "application/pdf");
        when(tokenService.uploadAttachment(eq("tenant-1"), eq("42"), any(UploadedFile.class),
                eq("att"), eq("att-id"), eq("att description"))).thenAnswer(call -> {
                    serviceThread.set(Thread.currentThread());
                    return Uni.createFrom().voidItem();
                });
        Uni<Response> operation = controller.uploadAttachment("42", "att", "att-id", "att description", upload, "tenant-1");
        verifyNoInteractions(tokenService);
        Files.writeString(path, "content-at-subscription");

        // when
        Response response = operation.await().atMost(TIMEOUT);

        // then
        assertEquals(204, response.getStatus());
        ArgumentCaptor<UploadedFile> file = ArgumentCaptor.forClass(UploadedFile.class);
        verify(tokenService).uploadAttachment(eq("tenant-1"), eq("42"), file.capture(),
                eq("att"), eq("att-id"), eq("att description"));
        assertEquals(new UploadedFile("contract.pdf", "application/pdf",
                "content-at-subscription".getBytes(StandardCharsets.UTF_8)), file.getValue());
        assertNotSame(caller, serviceThread.get());
        verify(upload, never()).size();
    }

    @Test
    void uploadAttachment_completesOnlyWhenTheServiceCompletesAndKeepsItsFailure() throws IOException, InterruptedException {
        // given
        AtomicReference<UniEmitter<? super Void>> pending = new AtomicReference<>();
        CountDownLatch started = new CountDownLatch(1);
        ProcessingException failure = new ProcessingException("upload failed");
        FileUpload upload = upload(file("contract.pdf", "pdf-content"), "contract.pdf", "application/pdf");
        when(tokenService.uploadAttachment(eq("tenant-1"), eq("42"), any(UploadedFile.class), eq("slow"), any(), any()))
                .thenReturn(Uni.createFrom().<Void>emitter(emitter -> {
                    pending.set(emitter);
                    started.countDown();
                }));
        when(tokenService.uploadAttachment(eq("tenant-1"), eq("42"), any(UploadedFile.class), eq("broken"), any(), any()))
                .thenReturn(Uni.createFrom().failure(failure));

        // when
        UniAssertSubscriber<Response> slow = controller.uploadAttachment("42", "slow", null, null, upload, "tenant-1")
                .subscribe().withSubscriber(UniAssertSubscriber.create());
        UniAssertSubscriber<Response> broken = controller.uploadAttachment("42", "broken", null, null, upload, "tenant-1")
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        // then
        assertTrue(started.await(TIMEOUT.toMillis(), TimeUnit.MILLISECONDS));
        slow.assertNotTerminated();
        pending.get().complete(null);
        assertEquals(204, slow.awaitItem(TIMEOUT).getItem().getStatus());
        assertSame(failure, broken.awaitFailure(TIMEOUT).getFailure());
    }

    @Test
    void complete_acceptsPdfAndP7mOnly() throws IOException {
        // given
        when(tokenService.completeTokenV2(eq("42"), any(UploadedFile.class))).thenReturn(Uni.createFrom().voidItem());
        FileUpload pdf = upload(file("c.pdf", "pdf-content"), "c.pdf", "application/pdf");
        FileUpload p7m = upload(file("c.pdf.p7m", "signed"), "c.pdf.p7m", "application/pkcs7-mime");
        FileUpload rejected = upload(file("c.txt", "text"), "c.txt", "text/plain");

        // when
        int pdfStatus = controller.complete("42", pdf).await().atMost(TIMEOUT).getStatus();
        int p7mStatus = controller.complete("42", p7m).await().atMost(TIMEOUT).getStatus();
        InvalidRequestException e = assertThrows(InvalidRequestException.class,
                () -> controller.complete("42", rejected).await().atMost(TIMEOUT));

        // then
        assertEquals(204, pdfStatus);
        assertEquals(204, p7mStatus);
        assertEquals("Formato file non supportato. Ammessi: [.pdf, .p7m]", e.getMessage());
        verify(tokenService, times(2)).completeTokenV2(eq("42"), any(UploadedFile.class));
        assertThrows(InvalidRequestException.class, () -> controller.complete("42", null));
    }

    @Test
    void complete_passesTheReadContentOnAWorkerAndWaitsForTheService() throws IOException, InterruptedException {
        // given
        AtomicReference<UniEmitter<? super Void>> pending = new AtomicReference<>();
        CountDownLatch started = new CountDownLatch(1);
        Path pdf = file("c.pdf", "pdf-content");
        when(tokenService.completeTokenV2(eq("42"), any(UploadedFile.class)))
                .thenReturn(Uni.createFrom().<Void>emitter(emitter -> {
                    pending.set(emitter);
                    started.countDown();
                }));

        // when
        UniAssertSubscriber<Response> result = controller.complete("42", upload(pdf, "c.pdf", "application/pdf"))
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        // then
        assertTrue(started.await(TIMEOUT.toMillis(), TimeUnit.MILLISECONDS));
        result.assertNotTerminated();
        pending.get().complete(null);
        assertEquals(204, result.awaitItem(TIMEOUT).getItem().getStatus());
        ArgumentCaptor<UploadedFile> file = ArgumentCaptor.forClass(UploadedFile.class);
        verify(tokenService).completeTokenV2(eq("42"), file.capture());
        assertEquals(new UploadedFile("c.pdf", "application/pdf", "pdf-content".getBytes(StandardCharsets.UTF_8)),
                file.getValue());
        assertTrue(Files.exists(pdf));
    }

    @Test
    void complete_emptyFileIsRejectedBeforeTheFormat() throws IOException {
        // given
        FileUpload empty = upload(file("c.txt", ""), "c.txt", "text/plain");

        // when
        InvalidRequestException e = assertThrows(InvalidRequestException.class,
                () -> controller.complete("42", empty).await().atMost(TIMEOUT));

        // then
        assertEquals("Il file è vuoto o mancante.", e.getMessage());
        verifyNoInteractions(tokenService);
    }

    @Test
    void complete_unreadableFileFailsOnSubscriptionBeforeTheFormatValidation() {
        // given
        FileUpload upload = upload(folder.resolve("missing.tmp"), "c.txt", "text/plain");

        // when
        Uni<Response> operation = controller.complete("42", upload);
        IllegalStateException e = assertThrows(IllegalStateException.class, () -> operation.await().atMost(TIMEOUT));

        // then
        assertEquals("Cannot read uploaded file", e.getMessage());
        verifyNoInteractions(tokenService);
    }

    @Test
    void completeOnboardingUsers_acceptsPdfAndP7mOnly() throws IOException {
        // given
        when(tokenService.completeOnboardingUsers(eq("42"), any(UploadedFile.class))).thenReturn(Uni.createFrom().voidItem());
        FileUpload pdf = upload(file("c.pdf", "pdf-content"), "c.pdf", "application/pdf");
        FileUpload rejected = upload(file("c.txt", "text"), "c.txt", "text/plain");

        // when
        int status = controller.completeOnboardingUsers("42", pdf).await().atMost(TIMEOUT).getStatus();
        InvalidRequestException e = assertThrows(InvalidRequestException.class,
                () -> controller.completeOnboardingUsers("42", rejected).await().atMost(TIMEOUT));

        // then
        assertEquals(204, status);
        assertEquals("Formato file non supportato. Ammessi: [.pdf, .p7m]", e.getMessage());
        verify(tokenService).completeOnboardingUsers(eq("42"), any(UploadedFile.class));
        assertThrows(InvalidRequestException.class, () -> controller.completeOnboardingUsers("42", null));
    }

    @Test
    void completeOnboardingUsers_unreadableFileFailsOnSubscriptionBeforeTheFormatValidation() {
        // given
        FileUpload upload = upload(folder.resolve("missing.tmp"), "c.txt", "text/plain");

        // when
        Uni<Response> operation = controller.completeOnboardingUsers("42", upload);
        IllegalStateException e = assertThrows(IllegalStateException.class, () -> operation.await().atMost(TIMEOUT));

        // then
        assertEquals("Cannot read uploaded file", e.getMessage());
        verifyNoInteractions(tokenService);
    }

    private static void assertBinary(Response response, String fileName, byte[] content) {
        assertEquals(200, response.getStatus());
        assertEquals("attachment; filename=" + fileName, response.getHeaderString(HttpHeaders.CONTENT_DISPOSITION));
        assertEquals(HttpHeaders.CONTENT_DISPOSITION, response.getHeaderString(EXPOSE_HEADERS));
        assertEquals(MediaType.APPLICATION_OCTET_STREAM_TYPE, response.getMediaType());
        assertArrayEquals(content, (byte[]) response.getEntity());
    }

    private Path file(String name, String content) throws IOException {
        return Files.writeString(folder.resolve(name), content);
    }

    private static FileUpload upload(Path tempFile, String fileName, String contentType) {
        FileUpload upload = mock(FileUpload.class);
        when(upload.fileName()).thenReturn(fileName);
        when(upload.contentType()).thenReturn(contentType);
        when(upload.uploadedFile()).thenReturn(tempFile);
        return upload;
    }
}
