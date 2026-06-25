package it.pagopa.selfcare.mscore.model.onboarding;

import lombok.Data;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Data
public class MailTemplate {
    private String subject;
    private String body;

    public String getSubject() {
        return new String(Base64.getDecoder().decode(subject), StandardCharsets.UTF_8);
    }

    public String getBody() {
        return new String(Base64.getDecoder().decode(body), StandardCharsets.UTF_8);
    }
}
