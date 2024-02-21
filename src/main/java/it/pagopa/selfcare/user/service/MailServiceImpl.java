package it.pagopa.selfcare.user.service;

import io.quarkus.arc.properties.IfBuildProperty;
import io.quarkus.mailer.Mail;
import io.quarkus.mailer.Mailer;
import io.quarkus.mailer.reactive.ReactiveMailer;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@Slf4j
@ApplicationScoped
@IfBuildProperty(name = "user-ms.mail.connector.type", stringValue = "default", enableIfMissing = true)
public class MailServiceImpl implements MailService {

    @Inject
    ReactiveMailer reactiveMailer;

    @ConfigProperty(name = "user-ms.mail.user-subject-prefix")
    String userMailSubjectPrefix;
    @ConfigProperty(name = "user-ms.mail.no-reply")
    String senderMail;

    public MailServiceImpl(ReactiveMailer reactiveMailer) {
        this.reactiveMailer = reactiveMailer;
    }

    @Override
    public Uni<Void> sendMail(String email, String content, String subject) {
        Mail mail = Mail
                .withHtml(email, userMailSubjectPrefix + subject, content)
                .setFrom(senderMail);

        return reactiveMailer.send(mail)
                .onFailure().invoke(throwable -> log.error("Error during send mail to: {} -> exception: {}", email, throwable.getMessage(), throwable))
                .onItem().invoke(() -> log.trace("Message sent successfully to: {}", email))
                .onFailure().recoverWithNull()
                .replaceWithVoid();
    }

}

