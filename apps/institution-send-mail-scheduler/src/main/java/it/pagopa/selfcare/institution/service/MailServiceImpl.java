package it.pagopa.selfcare.institution.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.mailer.Mail;
import io.quarkus.mailer.reactive.ReactiveMailer;
import io.smallrye.mutiny.Uni;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import it.pagopa.selfcare.azurestorage.AzureBlobClient;
import it.pagopa.selfcare.institution.exception.GenericException;
import it.pagopa.selfcare.institution.model.MailTemplate;
import jakarta.enterprise.context.ApplicationScoped;
import org.apache.commons.text.StringSubstitutor;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@ApplicationScoped
public class MailServiceImpl implements MailService {

    private static final String ERROR_DURING_SEND_MAIL = "Error during send mail";

    private static final Logger log = LoggerFactory.getLogger(MailServiceImpl.class);
    private final AzureBlobClient azureBlobClient;
    private final ObjectMapper objectMapper;
    private final String senderMail;
    private final Boolean destinationMailTest;
    private final String destinationMailTestAddress;
    private final ReactiveMailer reactiveMailer;

    public MailServiceImpl(AzureBlobClient azureBlobClient,
                           ObjectMapper objectMapper,
                           @ConfigProperty(name = "institution-send-mail.sender-mail") String senderMail,
                           @ConfigProperty(name = "institution-send-mail.destination-mail") Boolean destinationMailTest,
                           @ConfigProperty(name = "institution-send-mail.destination-mail-test-address") String destinationMailTestAddress,
                           ReactiveMailer reactiveMailer) {
        this.azureBlobClient = azureBlobClient;
        this.objectMapper = objectMapper;
        this.senderMail = senderMail;
        this.destinationMailTest = destinationMailTest;
        this.destinationMailTestAddress = destinationMailTestAddress;
        this.reactiveMailer = reactiveMailer;
    }

    @Override
    public Uni<Void> sendMail(List<String> destinationMail, String templateName, Map<String, String> mailParameters, String prefixSubject) {
        log.info(String.format("Sending mail with template %s", templateName));
        Mail mail = constructMail(destinationMail, mailParameters, templateName, prefixSubject);
        return reactiveMailer.send(mail)
                .onItem().invoke(unused -> log.info(String.format("Mail sent to %s, with subject %s", mail.getTo(), mail.getSubject())))
                .onFailure().invoke(throwable -> log.error(String.format("%s: %s", ERROR_DURING_SEND_MAIL, throwable.getMessage())))
                .onFailure().transform(throwable -> new GenericException(ERROR_DURING_SEND_MAIL));

    }

    private Mail constructMail(List<String> destinationMail, Map<String, String> mailParameters, String templateName, String prefixSubject) {
        try {

            // Dev mode send mail to test digital address
            String destination = destinationMailTest
                    ? destinationMailTestAddress
                    : destinationMail.get(0);

            log.info(String.format("Sending mail to %s", destination));
            String template = azureBlobClient.getFileAsText(templateName);
            MailTemplate mailTemplate = objectMapper.readValue(template, MailTemplate.class);
            String html = StringSubstitutor.replace(mailTemplate.getBody(), mailParameters);
            String subject = Optional.ofNullable(prefixSubject)
                    .map(value -> String.format("%s: %s", value, mailTemplate.getSubject()))
                    .orElse(mailTemplate.getSubject());

            return Mail
                    .withHtml(destination, subject, html)
                    .setFrom(senderMail);

        } catch (Exception e) {
            throw new GenericException(ERROR_DURING_SEND_MAIL);
        }
    }
}
