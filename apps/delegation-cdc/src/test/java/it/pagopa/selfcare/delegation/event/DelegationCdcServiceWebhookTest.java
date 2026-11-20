package it.pagopa.selfcare.delegation.event;

import com.azure.data.tables.TableClient;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.applicationinsights.TelemetryClient;
import io.quarkus.mongodb.reactive.ReactiveMongoClient;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.mongodb.MongoTestResource;
import it.pagopa.selfcare.delegation.event.config.ConfigUtilsBean;
import it.pagopa.selfcare.delegation.event.model.DelegationNotificationToSend;
import it.pagopa.selfcare.delegation.event.repository.DelegationRepository;
import it.pagopa.selfcare.delegation.event.repository.InstitutionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openapi.quarkus.webhook_ms_json.api.WebhookApi;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

// @QuarkusTest so quarkus-jacoco records coverage; the service is built by hand to set its injected fields
@QuarkusTest
@QuarkusTestResource(MongoTestResource.class)
class DelegationCdcServiceWebhookTest {

    private DelegationCdcService service;
    private WebhookApi webhookApi;

    @BeforeEach
    void setUp() {
        service = new DelegationCdcService(
                mock(ReactiveMongoClient.class, RETURNS_DEEP_STUBS), "testDatabase",
                mock(TelemetryClient.class, RETURNS_DEEP_STUBS), mock(TableClient.class),
                mock(ConfigUtilsBean.class), mock(InstitutionRepository.class), mock(DelegationRepository.class),
                1, 1, 0, List.of("prod-pagopa"), true);
        webhookApi = mock(WebhookApi.class);
        service.webhookApi = webhookApi;
        service.webhookTenantId = "AR";
        service.webhookTopic = "SC-Delegations";
    }

    @Test
    void sendWebhookNotification_disabled() {
        service.webhookEnabled = false;
        service.objectMapper = new ObjectMapper();

        assertDoesNotThrow(() -> service.sendWebhookNotification(new DelegationNotificationToSend()).await().indefinitely());

        verifyNoInteractions(webhookApi);
    }

    @Test
    void sendWebhookNotification_serializationError() throws JsonProcessingException {
        ObjectMapper objectMapper = mock(ObjectMapper.class);
        when(objectMapper.writeValueAsString(any())).thenThrow(new JsonProcessingException("boom") {});
        service.webhookEnabled = true;
        service.objectMapper = objectMapper;

        assertDoesNotThrow(() -> service.sendWebhookNotification(new DelegationNotificationToSend()).await().indefinitely());

        verifyNoInteractions(webhookApi);
    }
}
