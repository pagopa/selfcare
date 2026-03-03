package it.pagopa.selfcare.onboarding.utils;

import it.pagopa.selfcare.onboarding.config.NotificationConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;

import static it.pagopa.selfcare.onboarding.entity.Topic.*;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class NotificationBuilderFactoryTest {
    @InjectMocks
    private NotificationBuilderFactory notificationBuilderFactory;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
    }

    private NotificationConfig.Consumer createConsumer(String topic) {
        NotificationConfig.Consumer consumer = mock(NotificationConfig.Consumer.class);
        when(consumer.topic()).thenReturn(topic);
        return consumer;
    }
    @Test
    void createReturnsFdBuilderForFdTopic() {
        NotificationBuilder result = notificationBuilderFactory.create(createConsumer(SC_CONTRACTS_FD.getValue()));
        assertTrue(result instanceof FdNotificationBuilder);
    }

    @Test
    void createReturnsSapBuilderForSapTopic() {
        NotificationBuilder result = notificationBuilderFactory.create(createConsumer(SC_CONTRACTS_SAP.getValue()));
        assertTrue(result instanceof SapNotificationBuilder);
    }

    @Test
    void createReturnsCommonBuilderForCommonTopic() {
        NotificationBuilder result = notificationBuilderFactory.create(createConsumer(SC_CONTRACTS.getValue()));
        assertTrue(result instanceof BaseNotificationBuilder);
    }

    @Test
    void createThrowsExceptionForUnsupportedTopic() {
        assertThrows(IllegalArgumentException.class, () -> notificationBuilderFactory.create(createConsumer("unsupported_topic")));
    }

}