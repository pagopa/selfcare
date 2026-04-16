package it.pagopa.selfcare.institution.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.mailer.Mail;
import io.quarkus.mailer.reactive.ReactiveMailer;
import io.quarkus.test.junit.QuarkusTest;
import it.pagopa.selfcare.azurestorage.AzureBlobClient;
import it.pagopa.selfcare.institution.exception.GenericException;
import it.pagopa.selfcare.institution.model.MailTemplate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

@QuarkusTest
class MailServiceImplTest {

    AzureBlobClient azureBlobClient;

    ObjectMapper objectMapper;

    ReactiveMailer mailer;

    MailServiceImpl mailService;

    @BeforeEach
    void setUp() {
        mailer = mock(ReactiveMailer.class);
        azureBlobClient = mock(AzureBlobClient.class);
        objectMapper = mock(ObjectMapper.class);
        this.mailService = new MailServiceImpl( azureBlobClient, objectMapper, "pec@pec.it", Boolean.TRUE,
                "test@test.it", mailer);

    }

    @Test
    void shouldSendMailToTestAddress() throws JsonProcessingException {
        List<String> destinationMails = Collections.singletonList("realRecipient@example.com");
        Map<String, String> mailParameters = new HashMap<>();
        mailParameters.put("name","John Doe");
        MailTemplate mailTemplate = new MailTemplate();
        mailTemplate.setSubject("VGVzdCBTdWJqZWN0");
        mailTemplate.setBody("SGVsbG8gJHtuYW1lfSB0aGlzIGlzIHRoZSBib2R5");
        final String mailTemplateString = "template";
        Mockito.when(azureBlobClient.getFileAsText("templateName")).thenReturn(mailTemplateString);
        Mockito.when(objectMapper.readValue(Mockito.anyString(), Mockito.eq(MailTemplate.class)))
                .thenReturn(mailTemplate);
        // When
        mailService.sendMail(destinationMails, "templateName", mailParameters, null);

        // Then
        ArgumentCaptor<Mail> mailCaptor = ArgumentCaptor.forClass(Mail.class);
        Mockito.verify(mailer).send(mailCaptor.capture());
        assertEquals("test@test.it", mailCaptor.getValue().getTo().iterator().next());
    }

    @Test
    void shouldThrowGenericExceptionOnMailerFailure() {
        // Given
        Mockito.doThrow(new RuntimeException("Mailer exception")).when(mailer).send(Mockito.any(Mail.class));

        // Then
        assertThrows(GenericException.class, () -> {
            // When
            mailService.sendMail(Collections.singletonList("recipient@example.com"), "templateName", Collections.singletonMap("name", "John Doe"), null);
        });
    }
}