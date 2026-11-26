package it.pagopa.selfcare.user.service;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.tenant.TenantContext;
import it.pagopa.selfcare.tenant.UnknownTenantException;
import it.pagopa.selfcare.user.client.OneMailClient;
import jakarta.inject.Inject;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.openapi.quarkus.one_mail_json.model.EmailHighPriorityBodyDTO;
import org.openapi.quarkus.one_mail_json.model.EmailSuccessResponseDTO;

import java.time.Duration;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verifyNoInteractions;

@QuarkusTest
class OneMailServiceImplTest {
  @Inject TenantContext tenantContext;

  @Inject
  MailService mailService;

  @InjectMock
  @RestClient
  OneMailClient oneMailClient;


  @Test
  void usesExplicitTenantEvenWithAnotherTenantInTheRequestContext() {
    tenantContext.setTenantId("PNPG");

    when(oneMailClient.sendHighPriority(eq("ar-test-mail-key"), eq(false), any()))
        .thenReturn(Uni.createFrom().item(new EmailSuccessResponseDTO()));

    mailService.sendOneMail("AR", "userId", "email", "templateId", Map.of("key", "value"))
        .await().atMost(Duration.ofSeconds(5));

    ArgumentCaptor<EmailHighPriorityBodyDTO> emailArgumentCaptor =
      ArgumentCaptor.forClass(EmailHighPriorityBodyDTO.class);

    verify(oneMailClient).sendHighPriority(eq("ar-test-mail-key"), eq(false), emailArgumentCaptor.capture());
    assertEquals("noreply@selfcare.pagopa.it", emailArgumentCaptor.getValue().getFrom().getEmail());
  }

  @Test
  void doesNotSendMailWhenDisabledForTheExplicitTenant() {
    tenantContext.setTenantId("AR");
    mailService.sendOneMail("PNPG", "userId", "email", "templateId", Map.of())
        .await().atMost(Duration.ofSeconds(5));
    verifyNoInteractions(oneMailClient);
  }

  @Test
  void rejectsUnknownTenantWithoutSendingMail() {
    assertThrows(UnknownTenantException.class,
        () -> mailService.sendOneMail("UNKNOWN", "userId", "email", "templateId", Map.of()));
    verifyNoInteractions(oneMailClient);
  }
}
