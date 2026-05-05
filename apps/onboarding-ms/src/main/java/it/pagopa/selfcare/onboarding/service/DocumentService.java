package it.pagopa.selfcare.onboarding.service;

import io.smallrye.mutiny.Uni;
import org.openapi.quarkus.document_json.model.DocumentResponse;

public interface DocumentService {
    Uni<DocumentResponse> getDocumentByOnboardingId(String onboardingId);
}
