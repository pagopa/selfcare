package it.pagopa.selfcare.party.registry_proxy.web.controller;

import io.dapr.Topic;
import io.swagger.annotations.Api;
import it.pagopa.selfcare.party.registry_proxy.core.SearchService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping(value = "/dapr")
@Api(tags = "dapr")
public class EventController {
  public static final String RESPONSE_SUCCESS = "SUCCESS";
  private final SearchService searchService;

  @Autowired
  public EventController(SearchService searchService) {
    this.searchService = searchService;
  }

  @PostMapping("/subscribe")
  public List<Map<String, String>> subscribe() {
    return searchService.subscribe();
  }

  @Topic(name = "SC-Contracts", pubsubName = "selc-eventhub-pubsub")
  @PostMapping("/events")
  public ResponseEntity<Map<String, Object>> handleEvent(@RequestBody Map<String, Object> event) {
    log.debug("Received event on SC-Contracts topic, no-op");
    Map<String, Object> response = new HashMap<>();
    response.put("status", RESPONSE_SUCCESS);
    response.put("timestamp", System.currentTimeMillis());
    return ResponseEntity.ok(response);
  }

  @GetMapping("/health")
  public ResponseEntity<Map<String, String>> health() {
    Map<String, String> response = new HashMap<>();
    response.put("status", "healthy");
    response.put("service", "registry-proxy-events");
    response.put("timestamp", String.valueOf(System.currentTimeMillis()));
    return ResponseEntity.ok(response);
  }

}
