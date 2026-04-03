package it.pagopa.selfcare.onboarding.dto;

import it.pagopa.selfcare.onboarding.entity.Onboarding;
import org.openapi.quarkus.core_json.model.InstitutionResponse;
import org.openapi.quarkus.document_json.model.DocumentResponse;

public class NotificationsResources {
    private Onboarding onboarding;
    private InstitutionResponse institution;
    private DocumentResponse document;
    private QueueEvent queueEvent;

    public NotificationsResources(Onboarding onboarding, InstitutionResponse institution, org.openapi.quarkus.document_json.model.DocumentResponse document, QueueEvent queueEvent) {
        this.onboarding = onboarding;
        this.institution = institution;
        this.document = document;
        this.queueEvent = queueEvent;
    }

    public Onboarding getOnboarding() {
        return onboarding;
    }

    public void setOnboarding(Onboarding onboarding) {
        this.onboarding = onboarding;
    }

    public InstitutionResponse getInstitution() {
        return institution;
    }

    public void setInstitution(InstitutionResponse institution) {
        this.institution = institution;
    }

    public DocumentResponse getDocument() {
        return document;
    }

    public void setToken(DocumentResponse document) {
        this.document = document;
    }

    public QueueEvent getQueueEvent() {
        return queueEvent;
    }

    public void setQueueEvent(QueueEvent queueEvent) {
        this.queueEvent = queueEvent;
    }
}
