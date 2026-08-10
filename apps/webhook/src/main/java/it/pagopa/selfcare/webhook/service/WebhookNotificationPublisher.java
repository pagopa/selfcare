package it.pagopa.selfcare.webhook.service;

import com.azure.core.credential.TokenCredential;
import com.azure.identity.DefaultAzureCredentialBuilder;
import com.azure.storage.queue.QueueClient;
import com.azure.storage.queue.QueueClientBuilder;
import io.quarkus.runtime.StartupEvent;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import it.pagopa.selfcare.webhook.metrics.WebhookMetrics;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@Slf4j
@ApplicationScoped
public class WebhookNotificationPublisher {

  @Inject WebhookMetrics metrics;

  @ConfigProperty(name = "webhook.storage-queue.enabled", defaultValue = "false")
  boolean enabled;

  @ConfigProperty(name = "webhook.storage-queue.endpoint")
  String endpoint;

  @ConfigProperty(name = "webhook.storage-queue.queue")
  String queue;

  @ConfigProperty(name = "webhook.storage-queue.connection-string", defaultValue = "none")
  String connectionString;

  @ConfigProperty(name = "webhook.storage-queue.auto-create", defaultValue = "false")
  boolean autoCreate;

  private volatile QueueClient client;

  void start(@Observes StartupEvent event) {
    if (!enabled) {
      return;
    }
    client = buildClientBuilder().buildClient();
    ensureQueueExists();
  }

  /**
   * Creates the queue only when explicitly enabled (local/emulator setups). In the cloud the queue
   * is provisioned by Terraform and the managed identity only holds message level roles, so the
   * create call would fail with a 403 and abort the whole application startup.
   */
  private void ensureQueueExists() {
    if (!autoCreate) {
      return;
    }
    try {
      client.createIfNotExists();
    } catch (RuntimeException e) {
      log.warn("Unable to auto-create Storage Queue {}: {}", queue, e.getMessage());
    }
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
    long startNanos = System.nanoTime();
    return Uni.createFrom()
        .item(
            () -> {
              Objects.requireNonNull(client, "Storage Queue client is not initialized")
                  .sendMessage(notificationId);
              return true;
            })
        .runSubscriptionOn(Infrastructure.getDefaultWorkerPool())
        .onItemOrFailure()
        .invoke(
            (ignored, failure) ->
                metrics.recordPublish(failure == null, elapsedMs(startNanos)))
        .replaceWithVoid();
  }

  private static long elapsedMs(long startNanos) {
    return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos);
  }
}
