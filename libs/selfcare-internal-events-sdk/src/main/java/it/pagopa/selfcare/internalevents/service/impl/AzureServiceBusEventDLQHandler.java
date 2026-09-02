package it.pagopa.selfcare.internalevents.service.impl;

import com.azure.core.util.IterableStream;
import com.azure.identity.DefaultAzureCredentialBuilder;
import com.azure.messaging.servicebus.ServiceBusClientBuilder;
import com.azure.messaging.servicebus.ServiceBusReceivedMessage;
import com.azure.messaging.servicebus.ServiceBusReceiverClient;
import com.azure.messaging.servicebus.models.DeadLetterOptions;
import com.azure.messaging.servicebus.models.ServiceBusReceiveMode;
import com.azure.messaging.servicebus.models.SubQueue;
import it.pagopa.selfcare.internalevents.model.InternalEvent;
import it.pagopa.selfcare.internalevents.service.InternalEventDLQHandler;
import it.pagopa.selfcare.internalevents.util.InternalEventUtils;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.function.Consumer;

@Slf4j
public class AzureServiceBusEventDLQHandler implements InternalEventDLQHandler {

  private final ServiceBusReceiverClient serviceBusReceiverClient;

  public AzureServiceBusEventDLQHandler(String namespace, String managedIdentityClientId, String topic, String subscription) {
    final ServiceBusClientBuilder builder = new ServiceBusClientBuilder().fullyQualifiedNamespace(namespace)
      .credential(new DefaultAzureCredentialBuilder().managedIdentityClientId(managedIdentityClientId).build());
    this.serviceBusReceiverClient = buildClient(builder, topic, subscription);
  }

  public AzureServiceBusEventDLQHandler(String connectionString, String topic, String subscription) {
    final ServiceBusClientBuilder builder = new ServiceBusClientBuilder().connectionString(connectionString);
    this.serviceBusReceiverClient = buildClient(builder, topic, subscription);
  }

  @Override
  public boolean handle(int maxMessages, int timeoutSeconds, Consumer<InternalEvent> eventHandler) {
    final IterableStream<ServiceBusReceivedMessage> messages = serviceBusReceiverClient.receiveMessages(maxMessages, Duration.ofSeconds(timeoutSeconds));
    int messageCounter = 0;

    for (ServiceBusReceivedMessage m : messages) {
      try {
        messageCounter++;

        final String eventType = (String) m.getApplicationProperties().get("eventType");
        if (eventType == null || eventType.isBlank()) {
          log.info("Removing message without eventType");
          serviceBusReceiverClient.complete(m);
          continue;
        }

        final InternalEvent event = InternalEventUtils.MAPPER.readValue(m.getBody().toString(),
          Class.forName("it.pagopa.selfcare.internalevents.model." + eventType).asSubclass(InternalEvent.class));
        eventHandler.accept(event);

        if (event.getEventTargetStatus() == null || event.getEventTargetStatus() == InternalEvent.Status.ABANDON) {
          serviceBusReceiverClient.abandon(m);
        } else if (event.getEventTargetStatus() == InternalEvent.Status.DEAD_LETTER) {
          final DeadLetterOptions deadLetterOptions = new DeadLetterOptions()
            .setDeadLetterReason(event.getEventDeadLetterReason())
            .setDeadLetterErrorDescription(event.getEventDeadLetterDescription());
          serviceBusReceiverClient.deadLetter(m, deadLetterOptions);
        } else {
          serviceBusReceiverClient.complete(m);
        }
      } catch (Exception e) {
        log.error("Error while handling message: {}", m, e);
        serviceBusReceiverClient.abandon(m);
      }
    }

    return messageCounter > 0;
  }

  @Override
  public void close() {
    serviceBusReceiverClient.close();
  }

  private ServiceBusReceiverClient buildClient(ServiceBusClientBuilder builder, String topic, String subscription) {
    return builder.receiver()
      .topicName(topic)
      .subscriptionName(subscription)
      .subQueue(SubQueue.DEAD_LETTER_QUEUE)
      .receiveMode(ServiceBusReceiveMode.PEEK_LOCK)
      .disableAutoComplete()
      .buildClient();
  }

}
