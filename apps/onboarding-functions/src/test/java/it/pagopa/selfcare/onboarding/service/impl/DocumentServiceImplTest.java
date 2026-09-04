package it.pagopa.selfcare.onboarding.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openapi.quarkus.document_json.api.DocumentContentControllerApi;
import org.openapi.quarkus.document_json.api.DocumentControllerApi;
import org.openapi.quarkus.document_json.model.AttachmentPdfRequest;
import org.openapi.quarkus.document_json.model.ContractPdfRequest;
import org.openapi.quarkus.document_json.model.DocumentBuilderRequest;
import org.openapi.quarkus.document_json.model.DocumentResponse;
import org.openapi.quarkus.document_json.model.RelatedDocumentResponse;

class DocumentServiceImplTest {

  private DocumentContentControllerApi contentApi;
  private DocumentControllerApi controllerApi;
  private DocumentServiceImpl service;

  @BeforeEach
  void setUp() {
    contentApi = mock(DocumentContentControllerApi.class);
    controllerApi = mock(DocumentControllerApi.class);
    service = new DocumentServiceImpl(contentApi, controllerApi);
  }

  // ---------------------------------------------------------------------------
  // createContractPdf
  // ---------------------------------------------------------------------------

  @Test
  void createContractPdf_shouldDelegateToApi() {
    ContractPdfRequest req = new ContractPdfRequest();
    service.createContractPdf(req);
    verify(contentApi).createContractPdf(req);
  }

  @Test
  void createContractPdf_shouldLogAndRethrowOnWebApplicationException() {
    ContractPdfRequest req = new ContractPdfRequest();
    Response errorResponse = Response.status(500).entity("boom").build();
    WebApplicationException wae = new WebApplicationException(errorResponse);
    doThrow(wae).when(contentApi).createContractPdf(req);

    WebApplicationException thrown =
        assertThrows(WebApplicationException.class, () -> service.createContractPdf(req));
    assertSame(wae, thrown);
  }

  @Test
  void createContractPdf_shouldLogAndRethrowOnWebApplicationExceptionWithNullResponse() {
    ContractPdfRequest req = new ContractPdfRequest();
    WebApplicationException nullResponseException =
        new WebApplicationException("null-response") {
          @Override
          public Response getResponse() {
            return null;
          }
        };
    doThrow(nullResponseException).when(contentApi).createContractPdf(req);

    assertThrows(WebApplicationException.class, () -> service.createContractPdf(req));
  }

  // ---------------------------------------------------------------------------
  // createAttachmentPdf
  // ---------------------------------------------------------------------------

  @Test
  void createAttachmentPdf_shouldDelegateToApi() {
    AttachmentPdfRequest req = new AttachmentPdfRequest();
    service.createAttachmentPdf(req);
    verify(contentApi).createAttachmentPdf(req);
  }

  // ---------------------------------------------------------------------------
  // saveDocument
  // ---------------------------------------------------------------------------

  @Test
  void saveDocument_shouldDelegateToApi() {
    DocumentBuilderRequest req = new DocumentBuilderRequest();
    Response expected = Response.ok().build();
    when(controllerApi.saveDocument(req)).thenReturn(expected);

    assertSame(expected, service.saveDocument(req));
    verify(controllerApi).saveDocument(req);
  }

  // ---------------------------------------------------------------------------
  // uploadAggregatesCsv
  // ---------------------------------------------------------------------------

  @Test
  void uploadAggregatesCsv_shouldDelegateToApi() {
    DocumentContentControllerApi.UploadAggregatesCsvMultipartForm req =
        new DocumentContentControllerApi.UploadAggregatesCsvMultipartForm();
    Response expected = Response.ok().build();
    when(contentApi.uploadAggregatesCsv(req)).thenReturn(expected);

    assertSame(expected, service.uploadAggregatesCsv(req));
    verify(contentApi).uploadAggregatesCsv(req);
  }


  // ---------------------------------------------------------------------------
  // deleteContract
  // ---------------------------------------------------------------------------

  @Test
  void deleteContract_shouldDelegateToApi() {
    Response expected = Response.ok("deleted").build();
    when(contentApi.deleteContract("onb-1")).thenReturn(expected);

    assertSame(expected, service.deleteContract("onb-1"));
    verify(contentApi).deleteContract("onb-1");
  }

  // ---------------------------------------------------------------------------
  // deleteUserAttachments
  // ---------------------------------------------------------------------------

  @Test
  void deleteUserAttachments_shouldDelegateToApi() {
    Response expected = Response.ok("2/2 deleted").build();
    when(contentApi.deleteUserAttachments("onb-1")).thenReturn(expected);

    assertSame(expected, service.deleteUserAttachments("onb-1"));
    verify(contentApi).deleteUserAttachments("onb-1");
  }

  // ---------------------------------------------------------------------------
  // getDocumentByOnboardingId
  // ---------------------------------------------------------------------------

  @Test
  void getDocumentByOnboardingId_shouldDelegateToApi() {
    DocumentResponse expected = new DocumentResponse();
    when(controllerApi.getDocumentByOnboardingId("onb-1")).thenReturn(expected);

    assertSame(expected, service.getDocumentByOnboardingId("onb-1"));
    verify(controllerApi).getDocumentByOnboardingId("onb-1");
  }

  // ---------------------------------------------------------------------------
  // getDocumentByOnboardingIdOrNull
  // ---------------------------------------------------------------------------

  @Test
  void getDocumentByOnboardingIdOrNull_shouldReturnDocument() {
    DocumentResponse expected = new DocumentResponse();
    when(controllerApi.getDocumentByOnboardingId("onb-1")).thenReturn(expected);

    assertSame(expected, service.getDocumentByOnboardingIdOrNull("onb-1"));
  }

  @Test
  void getDocumentByOnboardingIdOrNull_shouldReturnNullOn404() {
    WebApplicationException wae = new WebApplicationException(Response.status(404).build());
    when(controllerApi.getDocumentByOnboardingId("onb-1")).thenThrow(wae);

    assertNull(service.getDocumentByOnboardingIdOrNull("onb-1"));
  }

  @Test
  void getDocumentByOnboardingIdOrNull_shouldRethrowOnOtherStatus() {
    WebApplicationException wae = new WebApplicationException(Response.status(500).build());
    when(controllerApi.getDocumentByOnboardingId("onb-1")).thenThrow(wae);

    assertThrows(
        WebApplicationException.class, () -> service.getDocumentByOnboardingIdOrNull("onb-1"));
  }

  @Test
  void getDocumentByOnboardingIdOrNull_shouldRethrowWhenResponseIsNull() {
    WebApplicationException wae =
        new WebApplicationException("null-response") {
          @Override
          public Response getResponse() {
            return null;
          }
        };
    when(controllerApi.getDocumentByOnboardingId("onb-1")).thenThrow(wae);

    assertThrows(
        WebApplicationException.class, () -> service.getDocumentByOnboardingIdOrNull("onb-1"));
  }

  // ---------------------------------------------------------------------------
  // getRelatedDocuments
  // ---------------------------------------------------------------------------

  @Test
  void getRelatedDocuments_shouldReturnList() {
    List<RelatedDocumentResponse> expected = List.of(new RelatedDocumentResponse());
    when(controllerApi.getRelatedDocuments("onb-1")).thenReturn(expected);

    assertEquals(expected, service.getRelatedDocuments("onb-1"));
  }

  @Test
  void getRelatedDocuments_shouldReturnEmptyListOn404() {
    WebApplicationException wae = new WebApplicationException(Response.status(404).build());
    when(controllerApi.getRelatedDocuments("onb-1")).thenThrow(wae);

    assertTrue(service.getRelatedDocuments("onb-1").isEmpty());
  }

  @Test
  void getRelatedDocuments_shouldRethrowOnOtherStatus() {
    WebApplicationException wae = new WebApplicationException(Response.status(500).build());
    when(controllerApi.getRelatedDocuments("onb-1")).thenThrow(wae);

    assertThrows(WebApplicationException.class, () -> service.getRelatedDocuments("onb-1"));
  }
}

