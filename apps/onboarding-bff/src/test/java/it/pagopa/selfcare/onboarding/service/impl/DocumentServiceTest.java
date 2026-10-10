package it.pagopa.selfcare.onboarding.service.impl;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.onboarding.client.DocumentContentRestClient;
import it.pagopa.selfcare.onboarding.client.model.BinaryData;
import it.pagopa.selfcare.onboarding.client.model.AttachmentTemplate;
import it.pagopa.selfcare.onboarding.client.model.UploadedFile;
import it.pagopa.selfcare.onboarding.exception.ResourceNotFoundException;
import it.pagopa.selfcare.onboarding.mapper.DocumentMapper;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.jboss.resteasy.reactive.client.api.ClientMultipartForm;
import org.openapi.quarkus.document_json.model.DocumentBuilderRequest;
import org.openapi.quarkus.document_json.model.UserAttachmentRequest;
import org.openapi.quarkus.document_json.api.DocumentControllerApi;

@ExtendWith(MockitoExtension.class)
class DocumentServiceTest {

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
    void downloadsKeepBytesAndResponseFilenameAndCloseTheResponse(String operation) {
        // given
        Response response = mock(Response.class);
        byte[] content = {0, 1, 2, -1};
        when(response.hasEntity()).thenReturn(true);
        when(response.readEntity(byte[].class)).thenReturn(content);
        when(response.getHeaderString(HttpHeaders.CONTENT_DISPOSITION))
                .thenReturn("attachment; filename*=UTF-8''document%20name.pdf");
        Supplier<BinaryData> download = switch (operation) {
            case "contract" -> {
                when(contentClient.getContract("ob1")).thenReturn(response);
                yield () -> service.getContract("ob1");
            }
            case "signed" -> {
                when(contentClient.getContractSigned("ob1")).thenReturn(response);
                yield () -> service.getContractSigned("ob1");
            }
            case "attachment" -> {
                when(contentClient.getAttachment("ob1", "requested.pdf")).thenReturn(response);
                yield () -> service.getAttachment("ob1", "requested.pdf");
            }
            case "template" -> {
                when(contentClient.getTemplateAttachment("ob1", "institution", "requested.pdf", "prod-io", "template"))
                        .thenReturn(response);
                yield () -> service.getTemplateAttachment("ob1", "institution", "requested.pdf", "prod-io", "template")
                        .await().indefinitely();
            }
            case "aggregates" -> {
                when(contentClient.getAggregatesCsv("ob1", "prod-io")).thenReturn(response);
                yield () -> service.getAggregatesCsv("ob1", "prod-io");
            }
            default -> throw new AssertionError(operation);
        };

        // when
        BinaryData result = download.get();

        // then
        assertArrayEquals(content, result.content());
        assertEquals("document name.pdf", result.fileName());
        verify(response).close();
    }

    @Test
    void emptyDocumentHasNoInventedFilename() {
        Response response = mock(Response.class);
        when(contentClient.getContract("ob1")).thenReturn(response);

        BinaryData result = service.getContract("ob1");

        assertArrayEquals(new byte[0], result.content());
        assertEquals(null, result.fileName());
        verify(response, never()).readEntity(byte[].class);
        verify(response).close();
    }

    @Test
    void malformedResponseFilenameStillClosesTheResponse() {
        Response response = mock(Response.class);
        when(contentClient.getContract("ob1")).thenReturn(response);
        when(response.getHeaderString(HttpHeaders.CONTENT_DISPOSITION))
                .thenReturn("attachment; filename*=UTF-16''file.pdf");

        assertThrows(IllegalArgumentException.class, () -> service.getContract("ob1"));

        verify(response).close();
    }

    @Test
    void downstreamFailureKeepsItsOriginalStatusException() {
        var failure = new ResourceNotFoundException("missing");
        when(contentClient.getContract("ob1")).thenThrow(failure);

        assertSame(failure, assertThrows(ResourceNotFoundException.class, () -> service.getContract("ob1")));
    }

    @Test
    void headReturnsStatusAndClosesTheResponse() {
        Response response = mock(Response.class);
        when(response.getStatus()).thenReturn(204);
        when(documentApi.headAttachment("ob1", "file.pdf")).thenReturn(Uni.createFrom().item(response));

        assertEquals(204, service.headAttachment("ob1", "file.pdf"));

        verify(response).close();
    }

    @Test
    void templateIoIsLazyAndRunsOnAWorker() {
        // given
        Thread caller = Thread.currentThread();
        AtomicReference<Thread> worker = new AtomicReference<>();
        Response response = mock(Response.class);
        when(contentClient.getTemplateAttachment("ob1", "institution", "file.pdf", "prod-test", "template"))
                .thenAnswer(call -> {
                    worker.set(Thread.currentThread());
                    return response;
                });

        // when
        Uni<BinaryData> operation = service.getTemplateAttachment("ob1", "institution", "file.pdf", "prod-test", "template");
        verifyNoInteractions(contentClient, response);
        BinaryData result = operation.await().atMost(Duration.ofSeconds(2));

        // then
        assertNotEquals(caller, worker.get());
        assertArrayEquals(new byte[0], result.content());
        verify(response).close();
    }

    @Test
    void templateReadFailureStillClosesTheResponse() {
        // given
        Response response = mock(Response.class);
        ResourceNotFoundException failure = new ResourceNotFoundException("failed read");
        when(contentClient.getTemplateAttachment("ob1", "institution", "file.pdf", "prod-test", "template"))
                .thenReturn(response);
        when(response.hasEntity()).thenReturn(true);
        when(response.readEntity(byte[].class)).thenThrow(failure);

        // when
        ResourceNotFoundException result = assertThrows(ResourceNotFoundException.class,
                () -> service.getTemplateAttachment("ob1", "institution", "file.pdf", "prod-test", "template")
                        .await().atMost(Duration.ofSeconds(2)));

        // then
        assertSame(failure, result);
        verify(response).close();
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void uploadsRunOnAWorkerAndCloseTheResponse(boolean userStorage) {
        // given
        Thread caller = Thread.currentThread();
        AtomicReference<Thread> worker = new AtomicReference<>();
        UploadedFile file = new UploadedFile("file.pdf", "application/pdf", new byte[]{1});
        AttachmentTemplate template = new AttachmentTemplate();
        Response response = mock(Response.class);
        if (userStorage) {
            when(mapper.toUserAttachmentRequest("ob1", "prod-test", "doc1", "description", "file.pdf", 1))
                    .thenReturn(new UserAttachmentRequest());
            when(contentClient.uploadUserAttachment(any(ClientMultipartForm.class))).thenAnswer(call -> {
                worker.set(Thread.currentThread());
                return response;
            });
        } else {
            when(mapper.toDocumentBuilderRequest("ob1", "file.pdf", "prod-test", template))
                    .thenReturn(new DocumentBuilderRequest());
            when(contentClient.uploadAttachment(any(ClientMultipartForm.class))).thenAnswer(call -> {
                worker.set(Thread.currentThread());
                return response;
            });
        }

        // when
        Uni<Void> operation = userStorage
                ? service.uploadUserAttachment("ob1", file, "prod-test", "doc1", "description", "file.pdf", 1)
                : service.uploadAttachment("ob1", file, "file.pdf", "prod-test", template);
        operation.await().atMost(Duration.ofSeconds(2));

        // then
        assertNotEquals(caller, worker.get());
        verify(response).close();
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void uploadFailureIsPropagatedWithoutRepeatingTheWrite(boolean userStorage) {
        // given
        UploadedFile file = new UploadedFile("file.pdf", "application/pdf", new byte[]{1});
        AttachmentTemplate template = new AttachmentTemplate();
        ResourceNotFoundException failure = new ResourceNotFoundException("upload failed");
        if (userStorage) {
            when(mapper.toUserAttachmentRequest("ob1", "prod-test", "doc1", "description", "file.pdf", 1))
                    .thenReturn(new UserAttachmentRequest());
            when(contentClient.uploadUserAttachment(any(ClientMultipartForm.class))).thenThrow(failure);
        } else {
            when(mapper.toDocumentBuilderRequest("ob1", "file.pdf", "prod-test", template))
                    .thenReturn(new DocumentBuilderRequest());
            when(contentClient.uploadAttachment(any(ClientMultipartForm.class))).thenThrow(failure);
        }

        // when
        Uni<Void> operation = userStorage
                ? service.uploadUserAttachment("ob1", file, "prod-test", "doc1", "description", "file.pdf", 1)
                : service.uploadAttachment("ob1", file, "file.pdf", "prod-test", template);
        ResourceNotFoundException result = assertThrows(ResourceNotFoundException.class,
                () -> operation.await().atMost(Duration.ofSeconds(2)));

        // then
        assertSame(failure, result);
        if (userStorage) {
            verify(contentClient).uploadUserAttachment(any(ClientMultipartForm.class));
        } else {
            verify(contentClient).uploadAttachment(any(ClientMultipartForm.class));
        }
        org.mockito.Mockito.verifyNoMoreInteractions(contentClient);
    }
}
