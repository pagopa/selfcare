package it.pagopa.selfcare.onboarding.dto;

import it.pagopa.selfcare.onboarding.entity.Onboarding;
import it.pagopa.selfcare.onboarding.entity.Token;
import org.openapi.quarkus.core_json.model.InstitutionResponse;

public class NotificationsResources {
    private Onboarding onboarding;
    private InstitutionResponse institution;
    private Token token;
    private QueueEvent queueEvent;

    public NotificationsResources(Onboarding onboarding, InstitutionResponse institution, Token token, QueueEvent queueEvent) {
        this.onboarding = onboarding;
        this.institution = institution;
        this.token = token;
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

    public Token getToken() {
        return token;
    }

    public void setToken(Token token) {
        this.token = token;
    }

    public QueueEvent getQueueEvent() {
        return queueEvent;
    }

    public void setQueueEvent(QueueEvent queueEvent) {
        this.queueEvent = queueEvent;
    }
}
