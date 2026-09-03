package it.pagopa.selfcare.internalevents.service.impl;

import com.azure.identity.DefaultAzureCredentialBuilder;
import com.azure.messaging.servicebus.ServiceBusClientBuilder;
import com.azure.messaging.servicebus.ServiceBusErrorContext;
import com.azure.messaging.servicebus.ServiceBusProcessorClient;
import com.azure.messaging.servicebus.ServiceBusReceivedMessageContext;
import com.azure.messaging.servicebus.models.DeadLetterOptions;
import com.azure.messaging.servicebus.models.ServiceBusReceiveMode;
import it.pagopa.selfcare.internalevents.model.InternalEvent;
import it.pagopa.selfcare.internalevents.service.InternalEventReceiver;
import it.pagopa.selfcare.internalevents.util.InternalEventUtils;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

@Slf4j
public class AzureServiceBusEventReceiver implements InternalEventReceiver {

  private final ServiceBusProcessorClient serviceBusProcessorClient;
  private final Map<String, Consumer<InternalEvent>> eventHandlers = new ConcurrentHashMap<>();

  public AzureServiceBusEventReceiver(String namespace, String managedIdentityClientId, String topic, String subscription,
                                      int maxConcurrentSessions, int maxConcurrentCalls) {
    final ServiceBusClientBuilder builder = new ServiceBusClientBuilder().fullyQualifiedNamespace(namespace)
      .credential(new DefaultAzureCredentialBuilder().managedIdentityClientId(managedIdentityClientId).build());
    this.serviceBusProcessorClient = buildClient(builder, topic, subscription, maxConcurrentSessions, maxConcurrentCalls);
  }

  public AzureServiceBusEventReceiver(String connectionString, String topic, String subscription,
                                      int maxConcurrentSessions, int maxConcurrentCalls) {
    final ServiceBusClientBuilder builder = new ServiceBusClientBuilder().connectionString(connectionString);
    this.serviceBusProcessorClient = buildClient(builder, topic, subscription, maxConcurrentSessions, maxConcurrentCalls);
  }

  @Override
  public <T extends InternalEvent> void subscribe(Class<T> eventType, Consumer<T> eventHandler) {
    eventHandlers.put(eventType.getSimpleName(), event -> eventHandler.accept(eventType.cast(event)));
    serviceBusProcessorClient.start();
  }

  @Override
  public void close() {
    serviceBusProcessorClient.stop();
    serviceBusProcessorClient.close();
  }

  private ServiceBusProcessorClient buildClient(ServiceBusClientBuilder builder, String topic, String subscription,
                                                int maxConcurrentSessions, int maxConcurrentCalls) {
    return builder.sessionProcessor()
      .maxConcurrentSessions(maxConcurrentSessions)
      .maxConcurrentCalls(maxConcurrentCalls)
      .receiveMode(ServiceBusReceiveMode.PEEK_LOCK)
      .topicName(topic)
      .subscriptionName(subscription)
      .processMessage(this::processMessage)
      .processError(this::processError)
      .disableAutoComplete()
      .buildProcessorClient();
  }

  private void processMessage(ServiceBusReceivedMessageContext ctx) {
    final String eventType = (String) ctx.getMessage().getApplicationProperties().get("eventType");
    if (eventType == null || eventType.isBlank() || !eventHandlers.containsKey(eventType)) {
      log.info("Removing message with unsubscribed eventType: {}", eventType);
      ctx.complete();
      return;
    }

    try {
      final InternalEvent event = InternalEventUtils.MAPPER.readValue(ctx.getMessage().getBody().toString(),
        Class.forName("it.pagopa.selfcare.internalevents.model." + eventType).asSubclass(InternalEvent.class));
      eventHandlers.get(eventType).accept(event);

      if (event.getEventTargetStatus() == null || event.getEventTargetStatus() == InternalEvent.Status.ABANDON) {
        ctx.abandon();
      } else if (event.getEventTargetStatus() == InternalEvent.Status.DEAD_LETTER) {
        final DeadLetterOptions deadLetterOptions = new DeadLetterOptions()
          .setDeadLetterReason(event.getEventDeadLetterReason())
          .setDeadLetterErrorDescription(event.getEventDeadLetterDescription());
        ctx.deadLetter(deadLetterOptions);
      } else {
        ctx.complete();
      }
    } catch (Exception ex) {
      log.error("Error processing message: {}", ctx.getMessage().getMessageId(), ex);
      ctx.abandon();
    }
  }

  private void processError(ServiceBusErrorContext ctx) {
    log.error("Error processing message: {}", ctx.getException().getMessage(), ctx.getException());
  }

}
