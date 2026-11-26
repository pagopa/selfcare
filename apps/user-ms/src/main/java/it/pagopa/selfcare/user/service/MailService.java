package it.pagopa.selfcare.user.service;

import io.smallrye.mutiny.Uni;
import java.util.Map;

public interface MailService {
    Uni<Void> sendOneMail(String tenant, String userId, String email, String templateId, Map<String, String> templateAttributes);
}
