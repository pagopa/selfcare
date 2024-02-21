package it.pagopa.selfcare.user.service;

import io.quarkus.mailer.Mail;
import io.quarkus.mailer.Mailer;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import io.smallrye.mutiny.helpers.test.UniAssertSubscriber;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.ses.model.SendEmailRequest;
import software.amazon.awssdk.services.ses.model.SendEmailResponse;


import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@QuarkusTest
@TestProfile(AwsMailServiceImplTest.AwsProfile.class)
class AwsMailServiceImplTest {

    @Inject
    MailService awsMailService;

    @Inject
    SesClient sesClient;


    public static class AwsProfile implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of("user-ms.mail.connector.type", "aws");
        }
    }

    @BeforeEach
    void startup() {
        sesClient = mock(SesClient.class);
        this.awsMailService = new AwsMailServiceImpl(sesClient);
    }

    @Test
    void testSendMailNotification() {

        when(sesClient.sendEmail(any(SendEmailRequest.class)))
                .thenReturn(SendEmailResponse.builder().build());

        awsMailService.sendMail("email", "content", "subject")
                .subscribe()
                .withSubscriber(UniAssertSubscriber.create());

        ArgumentCaptor<SendEmailRequest> mailArgumentCaptor = ArgumentCaptor.forClass(SendEmailRequest.class);
        Mockito.verify(sesClient, Mockito.times(1))
                .sendEmail(mailArgumentCaptor.capture());
    }
}
