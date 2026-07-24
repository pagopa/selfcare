package it.pagopa.selfcare.webhook.service;

import com.azure.core.credential.TokenCredential;
import com.azure.identity.DefaultAzureCredentialBuilder;
import com.azure.storage.queue.QueueClient;
import com.azure.storage.queue.QueueClientBuilder;
import io.quarkus.runtime.StartupEvent;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@Slf4j
@ApplicationScoped
public class WebhookNotificationPublisher {

  @ConfigProperty(name = "webhook.storage-queue.enabled", defaultValue = "false")
  boolean enabled;

  @ConfigProperty(name = "webhook.storage-queue.endpoint")
  String endpoint;

  @ConfigProperty(name = "webhook.storage-queue.queue")
  String queue;

  @ConfigProperty(name = "webhook.storage-queue.connection-string", defaultValue = "none")
  String connectionString;

  private QueueClient client;

  void start(@Observes StartupEvent event) {
    if (!enabled) {
      return;
    }
    client = buildClientBuilder().buildClient();
    client.createIfNotExists();
  }

  QueueClientBuilder buildClientBuilder() {
    QueueClientBuilder clientBuilder = new QueueClientBuilder().queueName(queue);
    if (hasConnectionString()) {
      clientBuilder.connectionString(connectionString);
    } else {
      TokenCredential credential = new DefaultAzureCredentialBuilder().build();
      clientBuilder.endpoint(endpoint).credential(credential);
    }
    return clientBuilder;
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
              Objects.requireNonNull(client, "Storage Queue client is not initialized")
                  .sendMessage(notificationId);
              return true;
            })
        .runSubscriptionOn(Infrastructure.getDefaultWorkerPool())
        .replaceWithVoid();
  }
}
