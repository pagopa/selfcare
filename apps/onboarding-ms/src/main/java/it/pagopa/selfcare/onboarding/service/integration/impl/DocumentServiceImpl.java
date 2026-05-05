package it.pagopa.selfcare.onboarding.service.integration.impl;

import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.onboarding.service.integration.DocumentService;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.openapi.quarkus.document_json.api.DocumentControllerApi;
import org.openapi.quarkus.document_json.model.DocumentResponse;

@ApplicationScoped
public class DocumentServiceImpl implements DocumentService {

    private final DocumentControllerApi documentControllerApi;

    public DocumentServiceImpl(@RestClient DocumentControllerApi documentControllerApi) {
        this.documentControllerApi = documentControllerApi;
    }

    @Override
    public Uni<DocumentResponse> getDocumentByOnboardingId(String onboardingId) {
        return documentControllerApi.getDocumentByOnboardingId(onboardingId);
    }
}
