package it.pagopa.selfcare.onboarding.entity;


import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class MailTemplate {
    private String subject;
    private String body;

    public String getSubject() {
        return new String(Base64.getDecoder().decode(subject), StandardCharsets.UTF_8);
    }

    public String getBody() {
        return new String(Base64.getDecoder().decode(body), StandardCharsets.UTF_8);
    }

    public void setBody(String body) {
        this.body = body;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }
}
