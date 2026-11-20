package it.pagopa.selfcare.mscore.connector.rest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.selfcare.commons.tenant.TenantContext;
import it.pagopa.selfcare.commons.tenant.TenantDefinition.OneMailCredentials;
import it.pagopa.selfcare.commons.tenant.TenantRegistry;
import it.pagopa.selfcare.commons.tenant.UnresolvedTenantException;
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
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ContextConfiguration(classes = {OneMailConnectorImpl.class})
@ExtendWith(SpringExtension.class)
class OneMailConnectorImplTest {

    @Autowired
    private OneMailConnectorImpl oneMailConnector;

    @MockBean
    private OneMailRestClient oneMailRestClient;

    @MockBean
    private TenantRegistry tenantRegistry;

    @MockBean
    private TenantContext tenantContext;

    @Test
    void sendMail() {
        when(tenantContext.requiredTenantId()).thenReturn("AR", "PNPG");
        when(tenantRegistry.oneMailCredentials("AR"))
                .thenReturn(new OneMailCredentials("https://ar.test", "noreply@test.it", "ar-key"));
        when(tenantRegistry.oneMailCredentials("PNPG"))
                .thenReturn(new OneMailCredentials("https://pnpg.test", "pnpg@test.it", "pnpg-key"));
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
        oneMailConnector.sendMail("user@test.it", "templateId", templateAttributes);
        verify(oneMailRestClient, times(2))._v1EmailsSendHighPost(eq(false), captor.capture());
        assertEquals("pnpg@test.it", captor.getValue().getFrom().getEmail());
    }

    @Test
    void missingTenantOrDefinitionDoesNotSend() {
        when(tenantContext.requiredTenantId()).thenThrow(new UnresolvedTenantException());
        assertThrows(UnresolvedTenantException.class,
                () -> oneMailConnector.sendMail("user@test.it", "template", Map.of()));
        doReturn("AR").when(tenantContext).requiredTenantId();
        when(tenantRegistry.oneMailCredentials("AR")).thenThrow(new IllegalStateException("Missing OneMail"));
        assertThrows(IllegalStateException.class,
                () -> oneMailConnector.sendMail("user@test.it", "template", Map.of()));
        verifyNoInteractions(oneMailRestClient);
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
