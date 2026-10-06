package it.pagopa.selfcare.onboarding.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.selfcare.onboarding.common.OnboardingStatus;
import it.pagopa.selfcare.onboarding.common.WorkflowType;
import java.util.List;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

@QuarkusTest
class AttachmentTemplateTest {

  private final ObjectMapper objectMapper = new ObjectMapper();

  @Test
  void readsAndWritesTheHistoricalDurablePayloadShape() throws Exception {
    String payload = """
        {
          "templatePath": "templates/contract.pdf",
          "templateVersion": "v1",
          "name": "Contract",
          "mandatory": true,
          "generated": false,
          "workflowType": ["IMPORT"],
          "workflowState": "REQUEST",
          "order": 2
        }
        """;

    AttachmentTemplate attachment = objectMapper.readValue(payload, AttachmentTemplate.class);

    assertEquals("templates/contract.pdf", attachment.getTemplatePath());
    assertEquals("v1", attachment.getTemplateVersion());
    assertEquals("Contract", attachment.getName());
    assertTrue(attachment.isMandatory());
    assertFalse(attachment.isGenerated());
    assertEquals(List.of(WorkflowType.IMPORT), attachment.getWorkflowType());
    assertEquals(OnboardingStatus.REQUEST, attachment.getWorkflowState());
    assertEquals(2, attachment.getOrder());

    JsonNode serialized = objectMapper.readTree(objectMapper.writeValueAsString(attachment));
    assertEquals("templates/contract.pdf", serialized.get("templatePath").asText());
    assertEquals("v1", serialized.get("templateVersion").asText());
    assertEquals("Contract", serialized.get("name").asText());
    assertTrue(serialized.get("mandatory").asBoolean());
    assertFalse(serialized.get("generated").asBoolean());
    assertEquals("IMPORT", serialized.get("workflowType").get(0).asText());
    assertEquals("REQUEST", serialized.get("workflowState").asText());
    assertEquals(2, serialized.get("order").asInt());
  }
}

