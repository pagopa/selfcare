package it.pagopa.selfcare.mscore.core;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.selfcare.mscore.api.FileStorageConnector;
import it.pagopa.selfcare.mscore.config.CoreConfig;
import it.pagopa.selfcare.mscore.model.onboarding.MailTemplate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.ses.model.SendEmailRequest;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserNotificationServiceImplTest {

    @InjectMocks
    private UserNotificationServiceImpl userNotificationService;

    @Mock
    private FileStorageConnector fileStorageConnector;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private CoreConfig coreConfig;

    @Mock
    private SesClient sesClient;


    @Test
    void sendDelegationUserNotification() throws JsonProcessingException {
        final String to = "id";
        when(fileStorageConnector.getTemplateFile(any())).thenReturn("template");
        MailTemplate mailTemplate = new MailTemplate();
        mailTemplate.setBody(Base64.getEncoder().encodeToString("test".getBytes(StandardCharsets.UTF_8)));
        mailTemplate.setSubject(Base64.getEncoder().encodeToString("test".getBytes(StandardCharsets.UTF_8)));
        when(objectMapper.readValue(anyString(),any(Class.class))).thenReturn(mailTemplate);
        assertDoesNotThrow(() -> userNotificationService.sendDelegationUserNotification(List.of(to),"institution","product", new HashMap<>()));

        ArgumentCaptor<SendEmailRequest> actual = ArgumentCaptor.forClass(SendEmailRequest.class);
        verify(sesClient, times(1)).sendEmail(actual.capture());
        assertEquals(to, actual.getValue().destination().toAddresses().get(0));
    }
}
