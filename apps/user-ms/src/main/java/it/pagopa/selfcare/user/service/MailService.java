package it.pagopa.selfcare.user.service;

import io.smallrye.mutiny.Uni;

public interface MailService {
    Uni<Void> sendOneMail(String userId, String email, String templateId, java.util.Map<String, String> templateAttributes);
}
