package it.pagopa.selfcare.user.service;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.ses.model.*;

@Slf4j
@ApplicationScoped
public class AwsMailServiceImpl implements MailService {

    private final SesClient sesClient;

    @ConfigProperty(name = "user-ms.mail.no-reply")
    String senderMail;

    public AwsMailServiceImpl(SesClient sesClient) {
        this.sesClient = sesClient;
    }

    @Override
    public Uni<Void> sendMail(String email, String html, String subject) {
        log.trace("sendMessage start");

        Destination destination = Destination.builder()
                .toAddresses(email)
                .build();

        Content content = Content.builder()
                .data(html)
                .build();

        Content sub = Content.builder()
                .data(subject)
                .build();

        Body body = Body.builder()
                .html(content)
                .build();

        Message msg = Message.builder()
                .subject(sub)
                .body(body)
                .build();

        SendEmailRequest emailRequest = SendEmailRequest.builder()
                .destination(destination)
                .message(msg)
                .source(senderMail)
                .build();

        log.trace("Attempting to send an email through Amazon SES using the AWS SDK for Java...");

        return Uni.createFrom()
                .item(() -> sesClient.sendEmail(emailRequest))
                .runSubscriptionOn(Infrastructure.getDefaultWorkerPool())
                .onFailure().invoke(throwable -> log.error("Error during send mail with AWS SES to: {} -> exception: {}", email, throwable.getMessage(), throwable))
                .onItem().invoke(() -> log.trace("Message sent successfully to: {}", email))
                .onFailure().recoverWithNull()
                .replaceWithVoid();

    }
}

