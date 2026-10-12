package it.pagopa.selfcare.onboarding.service.impl;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.test.UniAssertSubscriber;
import io.smallrye.mutiny.subscription.UniEmitter;
import it.pagopa.selfcare.onboarding.client.DocumentContentRestClient;
import it.pagopa.selfcare.onboarding.client.model.AttachmentTemplate;
import it.pagopa.selfcare.onboarding.client.model.AvailableDocuments;
import it.pagopa.selfcare.onboarding.client.model.BinaryData;
import it.pagopa.selfcare.onboarding.client.model.UploadedFile;
import it.pagopa.selfcare.onboarding.exception.ResourceNotFoundException;
import it.pagopa.selfcare.onboarding.mapper.DocumentMapper;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.jboss.resteasy.reactive.client.api.ClientMultipartForm;
import org.jboss.resteasy.reactive.client.impl.multipart.QuarkusMultipartFormDataPart;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openapi.quarkus.document_json.api.DocumentControllerApi;
import org.openapi.quarkus.document_json.model.AvailableDocumentsResponse;
import org.openapi.quarkus.document_json.model.DocumentBuilderRequest;
import org.openapi.quarkus.document_json.model.UserAttachmentRequest;

@ExtendWith(MockitoExtension.class)
class DocumentServiceTest {

    private static final Duration TIMEOUT = Duration.ofSeconds(2);

    @Mock
    DocumentContentRestClient contentClient;
    @Mock
    DocumentControllerApi documentApi;
    @Mock
    DocumentMapper mapper;

    DocumentService service;

    @BeforeEach
    void setUp() {
        service = new DocumentService(contentClient, documentApi, mapper);
    }

    @ParameterizedTest
    @ValueSource(strings = {"contract", "signed", "attachment", "template", "aggregates"})
    void downloadsAreLazyRunOnAWorkerKeepBytesAndFilenameAndCloseTheResponse(String operation) {
        // given
        Thread caller = Thread.currentThread();
        AtomicReference<Thread> worker = new AtomicReference<>();
        Response response = mock(Response.class);
        byte[] content = {0, 1, 2, -1};
        when(response.hasEntity()).thenReturn(true);
        when(response.readEntity(byte[].class)).thenReturn(content);
        when(response.getHeaderString(HttpHeaders.CONTENT_DISPOSITION))
                .thenReturn("attachment; filename*=UTF-8''document%20name.pdf");
        Uni<BinaryData> download = switch (operation) {
            case "contract" -> {
                when(contentClient.getContract("onb-1")).thenAnswer(call -> {
                    worker.set(Thread.currentThread());
                    return response;
                });
                yield service.getContract("onb-1");
            }
            case "signed" -> {
                when(contentClient.getContractSigned("onb-1")).thenAnswer(call -> {
                    worker.set(Thread.currentThread());
                    return response;
                });
                yield service.getContractSigned("onb-1");
            }
            case "attachment" -> {
                when(contentClient.getAttachment("onb-1", "requested.pdf")).thenAnswer(call -> {
                    worker.set(Thread.currentThread());
                    return response;
                });
                yield service.getAttachment("onb-1", "requested.pdf");
            }
            case "template" -> {
                when(contentClient.getTemplateAttachment("onb-1", "institution", "requested.pdf", "prod-test", "template"))
                        .thenAnswer(call -> {
                            worker.set(Thread.currentThread());
                            return response;
                        });
                yield service.getTemplateAttachment("onb-1", "institution", "requested.pdf", "prod-test", "template");
            }
            case "aggregates" -> {
                when(contentClient.getAggregatesCsv("onb-1", "prod-test")).thenAnswer(call -> {
                    worker.set(Thread.currentThread());
                    return response;
                });
                yield service.getAggregatesCsv("onb-1", "prod-test");
            }
            default -> throw new AssertionError(operation);
        };
        verifyNoInteractions(contentClient, response);

        // when
        BinaryData result = download.await().atMost(TIMEOUT);

        // then
        assertArrayEquals(content, result.content());
        assertEquals("document name.pdf", result.fileName());
        assertNotEquals(caller, worker.get());
        verify(response).close();
    }

    @Test
    void emptyDocumentHasNoInventedFilenameAndClosesTheResponse() {
        // given
        Response response = mock(Response.class);
        when(contentClient.getContract("onb-1")).thenReturn(response);

        // when
        BinaryData result = service.getContract("onb-1").await().atMost(TIMEOUT);

        // then
        assertArrayEquals(new byte[0], result.content());
        assertNull(result.fileName());
        verify(response, never()).readEntity(byte[].class);
        verify(response).close();
    }

    @Test
    void malformedResponseFilenameFailsAndStillClosesTheResponse() {
        // given
        Response response = mock(Response.class);
        when(contentClient.getContract("onb-1")).thenReturn(response);
        when(response.getHeaderString(HttpHeaders.CONTENT_DISPOSITION))
                .thenReturn("attachment; filename*=UTF-16''file.pdf");

        // when
        Uni<BinaryData> download = service.getContract("onb-1");

        // then
        assertThrows(IllegalArgumentException.class, () -> download.await().atMost(TIMEOUT));
        verify(response).close();
    }

    @Test
    void readFailureKeepsItsExceptionAndClosesTheResponse() {
        // given
        Response response = mock(Response.class);
        ResourceNotFoundException failure = new ResourceNotFoundException("failed read");
        when(contentClient.getAttachment("onb-1", "file.pdf")).thenReturn(response);
        when(response.hasEntity()).thenReturn(true);
        when(response.readEntity(byte[].class)).thenThrow(failure);

        // when
        Uni<BinaryData> download = service.getAttachment("onb-1", "file.pdf");

        // then
        assertSame(failure, assertThrows(ResourceNotFoundException.class, () -> download.await().atMost(TIMEOUT)));
        verify(response).close();
    }

    @Test
    void downstreamFailureKeepsItsOriginalException() {
        // given
        ResourceNotFoundException failure = new ResourceNotFoundException("missing");
        when(contentClient.getContractSigned("onb-1")).thenThrow(failure);

        // when
        Uni<BinaryData> download = service.getContractSigned("onb-1");

        // then
        assertSame(failure, assertThrows(ResourceNotFoundException.class, () -> download.await().atMost(TIMEOUT)));
    }

    @Test
    void availableDocumentsAreMappedOnlyWhenTheNativeClientEmits() {
        // given
        AtomicReference<UniEmitter<? super AvailableDocumentsResponse>> pending = new AtomicReference<>();
        AvailableDocumentsResponse remote = new AvailableDocumentsResponse();
        AvailableDocuments mapped = new AvailableDocuments();
        when(documentApi.getAvailableDocuments("onb-1"))
                .thenReturn(Uni.createFrom().<AvailableDocumentsResponse>emitter(pending::set));
        when(mapper.toAvailableDocuments(remote)).thenReturn(mapped);

        // when
        UniAssertSubscriber<AvailableDocuments> result = service.getAvailableDocuments("onb-1")
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        // then
        result.assertNotTerminated();
        verifyNoInteractions(mapper);
        pending.get().complete(remote);
        result.assertCompleted().assertItem(mapped);
    }

    @Test
    void availableDocumentsPropagateTheNativeFailureWithoutMapping() {
        // given
        ResourceNotFoundException failure = new ResourceNotFoundException("missing");
        when(documentApi.getAvailableDocuments("onb-1")).thenReturn(Uni.createFrom().failure(failure));

        // when
        UniAssertSubscriber<AvailableDocuments> result = service.getAvailableDocuments("onb-1")
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        // then
        assertSame(failure, result.getFailure());
        verifyNoInteractions(mapper);
    }

    @Test
    void headWaitsForTheNativeResponseThenReturnsItsStatusAndClosesIt() {
        // given
        AtomicReference<UniEmitter<? super Response>> pending = new AtomicReference<>();
        Response response = mock(Response.class);
        when(response.getStatus()).thenReturn(204);
        when(documentApi.headAttachment("onb-1", "file.pdf")).thenReturn(Uni.createFrom().<Response>emitter(pending::set));

        // when
        UniAssertSubscriber<Integer> result = service.headAttachment("onb-1", "file.pdf")
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        // then
        result.assertNotTerminated();
        verifyNoInteractions(response);
        pending.get().complete(response);
        result.assertCompleted().assertItem(204);
        verify(response).close();
    }

    @Test
    void headPropagatesTheNativeFailure() {
        // given
        ResourceNotFoundException failure = new ResourceNotFoundException("missing");
        when(documentApi.headAttachment("onb-1", "file.pdf")).thenReturn(Uni.createFrom().failure(failure));

        // when
        UniAssertSubscriber<Integer> result = service.headAttachment("onb-1", "file.pdf")
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        // then
        assertSame(failure, result.getFailure());
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void uploadsSendTheBytesOnAWorkerWhenSubscribedAndCloseTheResponse(boolean userStorage) {
        // given
        Thread caller = Thread.currentThread();
        AtomicReference<Thread> worker = new AtomicReference<>();
        byte[] bytes = {1, 2, 3};
        UploadedFile uploaded = new UploadedFile("file.pdf", "application/pdf", bytes);
        Response response = mock(Response.class);
        Uni<Void> operation = givenUpload(userStorage, uploaded, response, worker);

        // when
        operation.await().atMost(TIMEOUT);

        // then
        QuarkusMultipartFormDataPart filePart = capturedParts(userStorage).get(0);
        assertEquals("file", filePart.name());
        assertEquals("file.pdf", filePart.filename());
        assertEquals("application/pdf", filePart.mediaType());
        assertArrayEquals(bytes, filePart.content().getBytes());
        assertNotEquals(caller, worker.get());
        verify(response).close();
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void uploadsWithoutContentTypeDefaultToOctetStream(boolean userStorage) {
        // given
        UploadedFile uploaded = new UploadedFile("file.bin", null, "content".getBytes(StandardCharsets.UTF_8));
        Uni<Void> operation = givenUpload(userStorage, uploaded, mock(Response.class), new AtomicReference<>());

        // when
        operation.await().atMost(TIMEOUT);

        // then
        QuarkusMultipartFormDataPart filePart = capturedParts(userStorage).get(0);
        assertEquals("application/octet-stream", filePart.mediaType());
        assertEquals("content", filePart.content().toString(StandardCharsets.UTF_8));
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void uploadsDoNotTouchTheClientBeforeSubscription(boolean userStorage) {
        // given
        UploadedFile uploaded = new UploadedFile("file.pdf", "application/pdf", new byte[]{1});
        AttachmentTemplate template = new AttachmentTemplate();
        givenMappedRequests(userStorage, template);

        // when
        Uni<Void> operation = userStorage
                ? service.uploadUserAttachment("onb-1", uploaded, "prod-test", "doc-1", "description", "file.pdf", 1)
                : service.uploadAttachment("onb-1", uploaded, "file.pdf", "prod-test", template);

        // then
        verifyNoInteractions(contentClient);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void uploadFailureIsPropagatedWithoutRepeatingTheWrite(boolean userStorage) {
        // given
        UploadedFile uploaded = new UploadedFile("file.pdf", "application/pdf", new byte[]{1});
        AttachmentTemplate template = new AttachmentTemplate();
        ResourceNotFoundException failure = new ResourceNotFoundException("upload failed");
        givenMappedRequests(userStorage, template);
        if (userStorage) {
            when(contentClient.uploadUserAttachment(any(ClientMultipartForm.class))).thenThrow(failure);
        } else {
            when(contentClient.uploadAttachment(any(ClientMultipartForm.class))).thenThrow(failure);
        }

        // when
        Uni<Void> operation = userStorage
                ? service.uploadUserAttachment("onb-1", uploaded, "prod-test", "doc-1", "description", "file.pdf", 1)
                : service.uploadAttachment("onb-1", uploaded, "file.pdf", "prod-test", template);

        // then
        assertSame(failure, assertThrows(ResourceNotFoundException.class, () -> operation.await().atMost(TIMEOUT)));
        if (userStorage) {
            verify(contentClient).uploadUserAttachment(any(ClientMultipartForm.class));
        } else {
            verify(contentClient).uploadAttachment(any(ClientMultipartForm.class));
        }
        verifyNoMoreInteractions(contentClient);
    }

    private Uni<Void> givenUpload(boolean userStorage, UploadedFile uploaded, Response response,
                                  AtomicReference<Thread> worker) {
        AttachmentTemplate template = new AttachmentTemplate();
        givenMappedRequests(userStorage, template);
        if (userStorage) {
            when(contentClient.uploadUserAttachment(any(ClientMultipartForm.class))).thenAnswer(call -> {
                worker.set(Thread.currentThread());
                return response;
            });
            return service.uploadUserAttachment("onb-1", uploaded, "prod-test", "doc-1", "description", "file.pdf", 1);
        }
        when(contentClient.uploadAttachment(any(ClientMultipartForm.class))).thenAnswer(call -> {
            worker.set(Thread.currentThread());
            return response;
        });
        return service.uploadAttachment("onb-1", uploaded, "file.pdf", "prod-test", template);
    }

    private void givenMappedRequests(boolean userStorage, AttachmentTemplate template) {
        if (userStorage) {
            when(mapper.toUserAttachmentRequest("onb-1", "prod-test", "doc-1", "description", "file.pdf", 1))
                    .thenReturn(new UserAttachmentRequest());
        } else {
            when(mapper.toDocumentBuilderRequest("onb-1", "file.pdf", "prod-test", template))
                    .thenReturn(new DocumentBuilderRequest());
        }
    }

    private List<QuarkusMultipartFormDataPart> capturedParts(boolean userStorage) {
        ArgumentCaptor<ClientMultipartForm> form = ArgumentCaptor.forClass(ClientMultipartForm.class);
        if (userStorage) {
            verify(contentClient).uploadUserAttachment(form.capture());
        } else {
            verify(contentClient).uploadAttachment(form.capture());
        }
        return partsOf(form.getValue());
    }

    @SuppressWarnings("unchecked")
    private static List<QuarkusMultipartFormDataPart> partsOf(ClientMultipartForm form) {
        try {
            Field parts = ClientMultipartForm.class.getDeclaredField("parts");
            parts.setAccessible(true);
            return (List<QuarkusMultipartFormDataPart>) parts.get(form);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
    }
}
