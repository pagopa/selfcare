package it.pagopa.selfcare.institution.service;

import io.smallrye.mutiny.Uni;

public interface InstitutionSendMailScheduledService {
    Uni<Void> retrieveInstitutionFromMailNotificationAndSendMail();
}
