package it.pagopa.selfcare.webhook.service;

import com.azure.core.credential.TokenCredential;
import com.azure.identity.DefaultAzureCredentialBuilder;
import com.azure.storage.queue.QueueClient;
import com.azure.storage.queue.QueueClientBuilder;
import com.azure.storage.queue.models.QueueMessageItem;
import io.quarkus.runtime.StartupEvent;
import io.quarkus.scheduler.Scheduled;
import io.quarkus.vertx.core.runtime.context.VertxContextSafetyToggle;
import io.smallrye.common.vertx.VertxContext;
import io.vertx.core.Context;
import io.vertx.mutiny.core.Vertx;
import it.pagopa.selfcare.webhook.entity.WebhookNotification;
import it.pagopa.selfcare.webhook.repository.WebhookNotificationRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import java.time.Duration;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@Slf4j
@ApplicationScoped
public class WebhookNotificationConsumer {

  @Inject WebhookNotificationRepository notificationRepository;
  @Inject WebhookNotificationService notificationService;
  @Inject Vertx vertx;

  @ConfigProperty(name = "webhook.storage-queue.enabled", defaultValue = "false")
  boolean enabled;

  @ConfigProperty(name = "webhook.storage-queue.endpoint")
  String endpoint;

  @ConfigProperty(name = "webhook.storage-queue.queue")
  String queue;

  @ConfigProperty(name = "webhook.storage-queue.connection-string", defaultValue = "none")
  String connectionString;

  @ConfigProperty(name = "webhook.storage-queue.max-messages-per-poll", defaultValue = "32")
  int maxMessagesPerPoll;

  @ConfigProperty(name = "webhook.storage-queue.visibility-timeout-seconds", defaultValue = "300")
  int visibilityTimeoutSeconds;

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
    if ("none".equals(connectionString)) {
      clientBuilder.endpoint(endpoint).credential(new DefaultAzureCredentialBuilder().build());
    } else {
      clientBuilder.connectionString(connectionString);
    }
    return clientBuilder;
  }

  @Scheduled(every = "${webhook.storage-queue.poll-interval:5s}")
  void poll() {
    if (!enabled || client == null) {
      return;
    }
    try {
      client
          .receiveMessages(
              maxMessagesPerPoll, Duration.ofSeconds(visibilityTimeoutSeconds), null, null)
          .forEach(this::processMessage);
    } catch (Exception e) {
      log.error("Storage Queue polling error: {}", e.getMessage(), e);
    }
  }

  private void processMessage(QueueMessageItem message) {
    String notificationId = message.getMessageText();
    if (!ObjectId.isValid(notificationId)) {
      log.error("Discarding Storage Queue message with invalid notification ID: {}", notificationId);
      deleteMessage(message);
      return;
    }

    Context processingContext = VertxContext.getOrCreateDuplicatedContext(vertx.getDelegate());
    VertxContextSafetyToggle.setContextSafe(processingContext, true);
    processingContext.runOnContext(ignored -> processNotification(message, notificationId));
  }

  private void processNotification(QueueMessageItem message, String notificationId) {
    notificationRepository
        .claimForProcessing(notificationId, 5)
        .onItem()
        .transformToUni(
            notification -> {
              if (notification == null) {
                return io.smallrye.mutiny.Uni.createFrom().<WebhookNotification>nullItem();
              }
              return notificationService
                  .processNotification(notification)
                  .call(ignored -> notificationRepository.releaseProcessingLock(notification))
                  .replaceWith(notification);
            })
        .subscribe()
        .with(
            notification -> {
              if (notification != null
                  && notification.getStatus() == WebhookNotification.NotificationStatus.RETRY) {
                // Leave the message in the queue: it will become visible again once the
                // visibility timeout expires, triggering a natural retry.
                log.debug(
                    "Leaving Storage Queue message {} for retry of notification {}",
                    message.getMessageId(),
                    notificationId);
              } else {
                deleteMessage(message);
              }
            },
            error -> {
              log.error("Unable to process Storage Queue notification {}", notificationId, error);
              // Leave the message in the queue for retry after the visibility timeout expires.
            });
  }

  private void deleteMessage(QueueMessageItem message) {
    client.deleteMessage(message.getMessageId(), message.getPopReceipt());
  }
}
