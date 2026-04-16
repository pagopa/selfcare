package it.pagopa.selfcare.mscore.connector.rest.client;

import it.pagopa.selfcare.mscore.connector.rest.config.EventHubRestClientConfig;
import it.pagopa.selfcare.mscore.model.DelegationNotificationToSend;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.Map;

@FeignClient(name = "${rest-client.event-hub.serviceCode}", url = "${rest-client.event-hub.base-url}", configuration = EventHubRestClientConfig.class)
public interface EventHubRestClient {

    @PostMapping(value = "/messages", consumes = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<Void> sendMessage(@RequestBody DelegationNotificationToSend notification,
                                     @RequestHeader Map<String, String> headers);

}
