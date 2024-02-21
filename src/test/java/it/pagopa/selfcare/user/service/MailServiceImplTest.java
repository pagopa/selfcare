package it.pagopa.selfcare.user.service;

import io.quarkus.mailer.Mail;
import io.quarkus.mailer.Mailer;
import io.quarkus.mailer.reactive.ReactiveMailer;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import io.smallrye.mutiny.helpers.test.UniAssertSubscriber;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.util.Map;

import static org.mockito.Mockito.*;

@QuarkusTest
@TestProfile(MailServiceImplTest.MailerProfile.class)
class MailServiceImplTest {
    @Inject
    MailService mailService;

    @Inject
    ReactiveMailer mailer;

    public static class MailerProfile implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of("user-ms.mail.connector.type", "default");
        }
    }

    @BeforeEach
    void startup() {
        mailer = mock(ReactiveMailer.class);
        this.mailService = new MailServiceImpl(mailer);
    }

    @Test
    void testSendMailNotification() {

        mailService.sendMail("email", "content", "subject")
                .subscribe()
                .withSubscriber(UniAssertSubscriber.create())
                .assertCompleted();
        ArgumentCaptor<Mail> mailArgumentCaptor = ArgumentCaptor.forClass(Mail.class);
        Mockito.verify(mailer, Mockito.times(1))
                .send(mailArgumentCaptor.capture());
    }
}
