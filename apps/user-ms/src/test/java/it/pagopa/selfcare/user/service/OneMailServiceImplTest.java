package it.pagopa.selfcare.user.service;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.test.UniAssertSubscriber;
import jakarta.inject.Inject;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.openapi.quarkus.one_mail_json.api.EmailsApi;
import org.openapi.quarkus.one_mail_json.model.EmailHighPriorityBodyDTO;
import org.openapi.quarkus.one_mail_json.model.EmailSuccessResponseDTO;

import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@QuarkusTest
class OneMailServiceImplTest {

  @Inject
  MailService mailService;

  @InjectMock
  @RestClient
  EmailsApi emailsApi;


  @Test
  void testSendMailNotification() {

    when(emailsApi.v1EmailsSendHighPost(
      anyBoolean(),
      any(EmailHighPriorityBodyDTO.class)
    )).thenReturn(Uni.createFrom().item(new EmailSuccessResponseDTO()));

    UniAssertSubscriber<Void> subscriber = mailService
      .sendOneMail(
        "userId",
        "email",
        "templateId",
        Map.of("key", "value")
      )
      .subscribe()
      .withSubscriber(UniAssertSubscriber.create());

    subscriber
      .awaitItem()
      .assertCompleted();

    ArgumentCaptor<EmailHighPriorityBodyDTO> emailArgumentCaptor =
      ArgumentCaptor.forClass(EmailHighPriorityBodyDTO.class);

    verify(emailsApi, times(1))
      .v1EmailsSendHighPost(
        anyBoolean(),
        emailArgumentCaptor.capture()
      );
  }
}
