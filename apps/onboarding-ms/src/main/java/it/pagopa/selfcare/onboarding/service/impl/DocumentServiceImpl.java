package it.pagopa.selfcare.onboarding.service.impl;

import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.onboarding.service.DocumentService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.openapi.quarkus.document_json.api.DocumentContentControllerApi;
import org.openapi.quarkus.document_json.api.DocumentControllerApi;
import org.openapi.quarkus.document_json.model.DocumentResponse;
import org.openapi.quarkus.document_json.model.OnboardingDocumentRequest;

import java.util.List;

@ApplicationScoped
public class DocumentServiceImpl implements DocumentService {

    private final DocumentControllerApi documentControllerApi;
    private final DocumentContentControllerApi documentContentControllerApi;

    public DocumentServiceImpl(@RestClient DocumentControllerApi documentControllerApi,
                               @RestClient DocumentContentControllerApi documentContentControllerApi) {
        this.documentControllerApi = documentControllerApi;
        this.documentContentControllerApi = documentContentControllerApi;
    }

    @Override
    public Uni<DocumentResponse> getDocumentByOnboardingId(String onboardingId) {
        return documentControllerApi.getDocumentByOnboardingId(onboardingId);
    }

    @Override
    public Uni<List<String>> getAttachments(String onboardingId) {
        return documentControllerApi.getAttachments(onboardingId);
    }

    @Override
    public Uni<Response> updateDocumentUpdatedAt(String onboardingId) {
        return documentControllerApi.updateDocumentUpdatedAt(onboardingId);
    }

    @Override
    public Uni<Response> persistDocumentForImport(OnboardingDocumentRequest request) {
        return documentControllerApi.persistDocumentForImport(request);
    }

    @Override
    public Uni<Response> uploadSignedContract(DocumentContentControllerApi.UploadSignedContractMultipartForm request,
                                              String onboardingId) {
        return documentContentControllerApi.uploadSignedContract(request, onboardingId);
    }

    @Override
    public Uni<Response> deleteContract(String onboardingId) {
        return documentContentControllerApi.deleteContract(onboardingId);
    }
}
