package it.pagopa.selfcare.party.registry_proxy.web.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.selfcare.party.registry_proxy.connector.exception.ResourceNotFoundException;
import it.pagopa.selfcare.party.registry_proxy.core.ApplicationInsightsLogger;
import it.pagopa.selfcare.party.registry_proxy.core.SearchService;
import it.pagopa.selfcare.party.registry_proxy.web.config.WebTestConfig;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@WebMvcTest(value = {EventController.class}, excludeAutoConfiguration = SecurityAutoConfiguration.class)
@ContextConfiguration(classes = {EventController.class, WebTestConfig.class})
public class EventControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  protected ObjectMapper mapper;

  @MockBean
  private SearchService searchService;

  @MockBean
  private ApplicationInsightsLogger applicationInsightsLogger;


  @Test
  void handleSubscribe_shouldProcessSuccessfully() throws Exception {
    List<Map<String, String>> eventSubscription = new ArrayList<>();
    Map<String, String> map = new HashMap<>();
    map.put("route", "/dapr/events");
    map.put("pubsubname", "selc-eventhub-pubsub");
    map.put("topic", "SC-Contracts");
    eventSubscription.add(map);

    when(searchService.subscribe()).thenReturn(eventSubscription);

    mockMvc
      .perform(MockMvcRequestBuilders.post("/dapr/subscribe"))
      .andExpect(MockMvcResultMatchers.status().isOk())
      .andExpect(MockMvcResultMatchers.content().contentType("application/json"))
      .andExpect(MockMvcResultMatchers.content()
        .string("[{\"route\":\"/dapr/events\",\"pubsubname\":\"selc-eventhub-pubsub\",\"topic\":\"SC-Contracts\"}]"));

  }


  @Test
  void health_shouldReturnOk() throws Exception {
    // when & then
    mockMvc.perform(MockMvcRequestBuilders.get("/dapr/health"))
      .andExpect(MockMvcResultMatchers.status().isOk())
      .andExpect(jsonPath("$.status").value("healthy"));
  }

  @Test
  void handleEvent_shouldProcessEventSuccessfully() throws Exception {
    // given
    String institutionId = "test-institution-id";
    Map<String, Object> event = Map.of("id", "event-123", "institutionId", institutionId);
    when(searchService.indexInstitution(institutionId)).thenReturn(true);

    // when & then
    mockMvc.perform(MockMvcRequestBuilders.post("/dapr/events")
        .contentType(MediaType.APPLICATION_JSON)
        .content(mapper.writeValueAsString(event)))
      .andExpect(MockMvcResultMatchers.status().isOk())
      .andExpect(jsonPath("$.status").value("SUCCESS"))
      .andExpect(jsonPath("$.institutionId").value(institutionId));

    verify(searchService, times(1)).indexInstitution(institutionId);
  }

  @Test
  void handleEvent_shouldReturnErrorWhenInstitutionIdIsMissing() throws Exception {
    // given
    Map<String, Object> event = Map.of("id", "event-123"); // No institutionId

    // when & then
    mockMvc.perform(MockMvcRequestBuilders.post("/dapr/events")
        .contentType(MediaType.APPLICATION_JSON)
        .content(mapper.writeValueAsString(event)))
      .andExpect(MockMvcResultMatchers.status().isUnprocessableEntity()) // 422
      .andExpect(jsonPath("$.status").value("FAILED"))
      .andExpect(jsonPath("$.error").value("Missing institution id"));

    verify(searchService, never()).indexInstitution(anyString());
  }

  @Test
  void handleEvent_shouldReturnErrorWhenInstitutionIsNotFound() throws Exception {
    // given
    String institutionId = "not-found-id";
    Map<String, Object> event = Map.of("id", "event-123", "institutionId", institutionId);
    doThrow(new ResourceNotFoundException("Institution not found"))
      .when(searchService).indexInstitution(institutionId);

    // when & then
    mockMvc.perform(MockMvcRequestBuilders.post("/dapr/events")
        .contentType(MediaType.APPLICATION_JSON)
        .content(mapper.writeValueAsString(event)))
      .andExpect(MockMvcResultMatchers.status().isUnprocessableEntity()) // 422
      .andExpect(jsonPath("$.status").value("FAILED"))
      .andExpect(jsonPath("$.error").value("Indexing failed - Missing institution ID"))
      .andExpect(jsonPath("$.details").value("Institution not found"));

    verify(searchService, times(1)).indexInstitution(institutionId);
  }
}
