package it.pagopa.selfcare.mscore.connector.rest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.selfcare.mscore.connector.rest.client.OneMailRestClient;
import it.pagopa.selfcare.one_mail.generated.openapi.v1.dto.EmailAddress;
import it.pagopa.selfcare.one_mail.generated.openapi.v1.dto.EmailHighPriorityBodyDTO;
import it.pagopa.selfcare.one_mail.generated.openapi.v1.dto.EmailSuccessResponseDTO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ContextConfiguration(classes = {OneMailConnectorImpl.class})
@TestPropertySource(properties = "rest-client.one-mail.sender=noreply@test.it")
@ExtendWith(SpringExtension.class)
class OneMailConnectorImplTest {

    @Autowired
    private OneMailConnectorImpl oneMailConnector;

    @MockBean
    private OneMailRestClient oneMailRestClient;

    @Test
    void sendMail() {
        Map<String, String> templateAttributes = Map.of("productName", "product");
        when(oneMailRestClient._v1EmailsSendHighPost(any(), any()))
                .thenReturn(ResponseEntity.accepted().body(new EmailSuccessResponseDTO().requestId("requestId")));

        String requestId = oneMailConnector.sendMail("user@test.it", "templateId", templateAttributes);

        assertEquals("requestId", requestId);
        ArgumentCaptor<EmailHighPriorityBodyDTO> captor = ArgumentCaptor.forClass(EmailHighPriorityBodyDTO.class);
        verify(oneMailRestClient)._v1EmailsSendHighPost(eq(false), captor.capture());
        EmailHighPriorityBodyDTO request = captor.getValue();
        assertEquals("noreply@test.it", request.getFrom().getEmail());
        assertEquals("user@test.it", request.getTo().getEmail());
        assertEquals(Map.of("templateId", "templateId", "templateAttributes", templateAttributes),
                request.getTemplateContent());
    }

    @Test
    void emailHighPriorityBody_doesNotSerializeNullFields() {
        EmailHighPriorityBodyDTO body = EmailHighPriorityBodyDTO.builder()
                .from(new EmailAddress().email("noreply@test.it"))
                .to(new EmailAddress().email("user@test.it"))
                .templateContent(Map.of("templateId", "templateId"))
                .build();

        JsonNode json = new ObjectMapper().valueToTree(body);

        assertEquals("templateId", json.at("/templateContent/templateId").asText());
        assertFalse(json.has("emailContent"));
        assertFalse(json.has("extendedHeaders"));
        assertFalse(json.has("tag"));
        assertFalse(json.at("/from").has("name"));
    }

    @Test
    void sendMail_withoutEmail() {
        Map<String, String> templateAttributes = Map.of();
        assertThrows(IllegalArgumentException.class,
                () -> oneMailConnector.sendMail(null, "templateId", templateAttributes));
        verifyNoInteractions(oneMailRestClient);
    }

    @Test
    void sendMail_withoutTemplateId() {
        Map<String, String> templateAttributes = Map.of();
        assertThrows(IllegalArgumentException.class,
                () -> oneMailConnector.sendMail("user@test.it", null, templateAttributes));
        verifyNoInteractions(oneMailRestClient);
    }
}
