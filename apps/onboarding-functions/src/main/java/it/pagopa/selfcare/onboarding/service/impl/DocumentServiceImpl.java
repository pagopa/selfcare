package it.pagopa.selfcare.onboarding.service.impl;
import it.pagopa.selfcare.onboarding.service.*;


import it.pagopa.selfcare.onboarding.service.DocumentService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.openapi.quarkus.document_json.api.DocumentContentControllerApi;
import org.openapi.quarkus.document_json.api.DocumentControllerApi;
import org.openapi.quarkus.document_json.model.AttachmentPdfRequest;
import org.openapi.quarkus.document_json.model.ContractPdfRequest;
import org.openapi.quarkus.document_json.model.DocumentBuilderRequest;
import org.openapi.quarkus.document_json.model.DocumentResponse;

@ApplicationScoped
@Slf4j
public class DocumentServiceImpl implements DocumentService {

  private final DocumentContentControllerApi documentContentControllerApi;
  private final DocumentControllerApi documentControllerApi;

  public DocumentServiceImpl(
      @RestClient DocumentContentControllerApi documentContentControllerApi,
      @RestClient DocumentControllerApi documentControllerApi) {
    this.documentContentControllerApi = documentContentControllerApi;
    this.documentControllerApi = documentControllerApi;
  }

  @Override
  public void createContractPdf(ContractPdfRequest request) {
    try {
      documentContentControllerApi.createContractPdf(request);
    } catch (WebApplicationException e) {
      Response response = e.getResponse();
      if (response != null) {
        String responseBody = safeReadEntity(response);
        log.error(
            "Document API createContractPdf failed - status: {}, responseBody: {}",
            response.getStatus(),
            responseBody);
      } else {
        log.error("Document API createContractPdf failed with null response", e);
      }
      throw e;
    }
  }

  @Override
  public void createAttachmentPdf(AttachmentPdfRequest request) {
    documentContentControllerApi.createAttachmentPdf(request);
  }

  @Override
  public Response saveDocument(DocumentBuilderRequest request) {
    return documentControllerApi.saveDocument(request);
  }

  @Override
  public Response uploadAggregatesCsv(
      DocumentContentControllerApi.UploadAggregatesCsvMultipartForm request) {
    return documentContentControllerApi.uploadAggregatesCsv(request);
  }

  @Override
  public Response saveVisuraForMerchant(
      DocumentContentControllerApi.SaveVisuraForMerchantMultipartForm request) {
    return documentContentControllerApi.saveVisuraForMerchant(request);
  }

  @Override
  public Response deleteContract(String onboardingId) {
    return documentContentControllerApi.deleteContract(onboardingId);
  }

  @Override
  public DocumentResponse getDocumentByOnboardingId(String onboardingId) {
    return documentControllerApi.getDocumentByOnboardingId(onboardingId);
  }

  @Override
  public DocumentResponse getDocumentByOnboardingIdOrNull(String onboardingId) {
    try {
      return documentControllerApi.getDocumentByOnboardingId(onboardingId);
    } catch (WebApplicationException e) {
      if (e.getResponse() != null && e.getResponse().getStatus() == 404) {
        return null;
      }
      throw e;
    }
  }

  private String safeReadEntity(Response response) {
    try {
      return response.readEntity(String.class);
    } catch (Exception ex) {
      return "unavailable";
    }
  }
}
