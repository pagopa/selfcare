package it.pagopa.selfcare.mscore.api;

import java.util.Map;

public interface OneMailConnector {

    String sendMail(String email, String templateId, Map<String, String> templateAttributes);
}
