package it.pagopa.selfcare.onboarding.service.impl;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.onboarding.client.DocumentContentRestClient;
import it.pagopa.selfcare.onboarding.client.model.BinaryData;
import it.pagopa.selfcare.onboarding.exception.ResourceNotFoundException;
import it.pagopa.selfcare.onboarding.mapper.DocumentMapper;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
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
        Response response = mock(Response.class);
        byte[] content = {0, 1, 2, -1};
        when(response.hasEntity()).thenReturn(true);
        when(response.readEntity(byte[].class)).thenReturn(content);
        when(response.getHeaderString(HttpHeaders.CONTENT_DISPOSITION))
                .thenReturn("attachment; filename*=UTF-8''document%20name.pdf");
        BinaryData result = switch (operation) {
            case "contract" -> {
                when(contentClient.getContract("ob1")).thenReturn(response);
                yield service.getContract("ob1");
            }
            case "signed" -> {
                when(contentClient.getContractSigned("ob1")).thenReturn(response);
                yield service.getContractSigned("ob1");
            }
            case "attachment" -> {
                when(contentClient.getAttachment("ob1", "requested.pdf")).thenReturn(response);
                yield service.getAttachment("ob1", "requested.pdf");
            }
            case "template" -> {
                when(contentClient.getTemplateAttachment("ob1", "institution", "requested.pdf", "prod-io", "template"))
                        .thenReturn(response);
                yield service.getTemplateAttachment("ob1", "institution", "requested.pdf", "prod-io", "template");
            }
            case "aggregates" -> {
                when(contentClient.getAggregatesCsv("ob1", "prod-io")).thenReturn(response);
                yield service.getAggregatesCsv("ob1", "prod-io");
            }
            default -> throw new AssertionError(operation);
        };

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
}
