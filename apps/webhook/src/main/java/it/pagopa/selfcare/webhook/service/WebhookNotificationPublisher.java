package it.pagopa.selfcare.webhook.service;

import com.azure.core.credential.TokenCredential;
import com.azure.identity.DefaultAzureCredentialBuilder;
import com.azure.messaging.servicebus.ServiceBusClientBuilder;
import com.azure.messaging.servicebus.ServiceBusMessage;
import com.azure.messaging.servicebus.ServiceBusSenderClient;
import io.quarkus.runtime.StartupEvent;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@Slf4j
@ApplicationScoped
public class WebhookNotificationPublisher {

  @ConfigProperty(name = "webhook.service-bus.enabled", defaultValue = "false")
  boolean enabled;

  @ConfigProperty(name = "webhook.service-bus.namespace")
  String namespace;

  @ConfigProperty(name = "webhook.service-bus.queue")
  String queue;

  @ConfigProperty(name = "webhook.service-bus.connection-string", defaultValue = "none")
  String connectionString;

  private ServiceBusSenderClient sender;

  void start(@Observes StartupEvent event) {
    if (!enabled) {
      return;
    }
    ServiceBusClientBuilder clientBuilder = new ServiceBusClientBuilder();
    if (hasConnectionString()) {
      clientBuilder.connectionString(connectionString);
    } else {
      TokenCredential credential = new DefaultAzureCredentialBuilder().build();
      clientBuilder.fullyQualifiedNamespace(namespace).credential(credential);
    }
    sender = clientBuilder.sender().queueName(queue).buildClient();
  }

  private boolean hasConnectionString() {
    return !"none".equals(connectionString);
  }

  public Uni<Void> publish(String notificationId) {
    if (!enabled) {
      return Uni.createFrom().voidItem();
    }
    return Uni.createFrom()
        .item(
            () -> {
              Objects.requireNonNull(sender, "Service Bus sender is not initialized")
                  .sendMessage(new ServiceBusMessage(notificationId).setMessageId(notificationId));
              return true;
            })
        .runSubscriptionOn(Infrastructure.getDefaultWorkerPool())
        .replaceWithVoid();
  }

  @PreDestroy
  void stop() {
    if (sender != null) {
      sender.close();
    }
  }
}
