package it.pagopa.selfcare.onboarding.service;

import jakarta.ws.rs.core.Response;
import org.openapi.quarkus.document_json.api.DocumentContentControllerApi;
import org.openapi.quarkus.document_json.model.AttachmentPdfRequest;
import org.openapi.quarkus.document_json.model.ContractPdfRequest;
import org.openapi.quarkus.document_json.model.DocumentBuilderRequest;
import org.openapi.quarkus.document_json.model.DocumentResponse;

public interface DocumentService {

  void createContractPdf(ContractPdfRequest request);

  void createAttachmentPdf(AttachmentPdfRequest request);

  Response saveDocument(DocumentBuilderRequest request);

  Response uploadAggregatesCsv(DocumentContentControllerApi.UploadAggregatesCsvMultipartForm request);

  Response saveVisuraForMerchant(DocumentContentControllerApi.SaveVisuraForMerchantMultipartForm request);

  Response deleteContract(String onboardingId);

  DocumentResponse getDocumentByOnboardingId(String onboardingId);

  DocumentResponse getDocumentByOnboardingIdOrNull(String onboardingId);
}
