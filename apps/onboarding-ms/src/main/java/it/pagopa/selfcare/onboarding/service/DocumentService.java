package it.pagopa.selfcare.onboarding.service;

import io.smallrye.mutiny.Uni;
import jakarta.ws.rs.core.Response;
import org.openapi.quarkus.document_json.api.DocumentContentControllerApi;
import org.openapi.quarkus.document_json.model.DocumentResponse;
import org.openapi.quarkus.document_json.model.OnboardingDocumentRequest;

import java.util.List;

public interface DocumentService {
    Uni<DocumentResponse> getDocumentByOnboardingId(String onboardingId);

    Uni<List<String>> getAttachments(String onboardingId);

    Uni<Response> updateDocumentUpdatedAt(String onboardingId);

    Uni<Response> persistDocumentForImport(OnboardingDocumentRequest request);

    Uni<Response> uploadSignedContract(DocumentContentControllerApi.UploadSignedContractMultipartForm request,
                                       String onboardingId);

    Uni<Response> deleteContract(String onboardingId);
}
