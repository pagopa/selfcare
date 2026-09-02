package it.pagopa.selfcare.internalevents.service.impl;

import com.azure.identity.DefaultAzureCredentialBuilder;
import com.azure.messaging.servicebus.ServiceBusClientBuilder;
import com.azure.messaging.servicebus.ServiceBusMessage;
import com.azure.messaging.servicebus.ServiceBusSenderClient;
import it.pagopa.selfcare.internalevents.model.InternalEvent;
import it.pagopa.selfcare.internalevents.service.InternalEventSender;
import it.pagopa.selfcare.internalevents.util.InternalEventUtils;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class AzureServiceBusEventSender implements InternalEventSender {

  private final ServiceBusSenderClient serviceBusSenderClient;
  private final String topic;

  public AzureServiceBusEventSender(String namespace, String managedIdentityClientId, String topic) {
    final ServiceBusClientBuilder builder = new ServiceBusClientBuilder().fullyQualifiedNamespace(namespace)
      .credential(new DefaultAzureCredentialBuilder().managedIdentityClientId(managedIdentityClientId).build());
    this.serviceBusSenderClient = buildClient(builder, topic);
    this.topic = topic;
  }

  public AzureServiceBusEventSender(String connectionString, String topic) {
    final ServiceBusClientBuilder builder = new ServiceBusClientBuilder().connectionString(connectionString);
    this.serviceBusSenderClient = buildClient(builder, topic);
    this.topic = topic;
  }

  @Override
  public boolean publish(InternalEvent event) {
    final String eventTopic = InternalEventUtils.getTopicName(event.getClass());
    if (!eventTopic.equals(topic)) {
      log.error("Event type {} is not allowed for topic {}", event.getClass().getSimpleName(), topic);
      return false;
    }

    try {
      final ServiceBusMessage message = new ServiceBusMessage(InternalEventUtils.MAPPER.writeValueAsString(event));
      message.setSessionId(InternalEventUtils.getSessionId(event));
      message.getApplicationProperties().put("eventType", event.getEventType());
      message.getApplicationProperties().put("eventTenant", event.getEventTenant());
      message.getApplicationProperties().put("eventSource", event.getEventSource());
      serviceBusSenderClient.sendMessage(message);
      return true;
    } catch (Exception e) {
      log.error("Error while publishing event: {}", event, e);
      return false;
    }
  }

  @Override
  public void close() {
    serviceBusSenderClient.close();
  }

  private ServiceBusSenderClient buildClient(ServiceBusClientBuilder builder, String topic) {
    return builder.sender()
      .topicName(topic)
      .buildClient();
  }

}
