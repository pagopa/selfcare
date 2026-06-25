package it.pagopa.selfcare.institution.service;

import io.smallrye.mutiny.Uni;

import java.util.List;
import java.util.Map;

public interface MailService {

    Uni<Void> sendMail(List<String> destinationMail, String templateName, Map<String, String> mailParameters, String prefixSubject);

}
