package it.pagopa.selfcare.webhook.service;

import com.azure.identity.DefaultAzureCredentialBuilder;
import com.azure.storage.queue.QueueClient;
import com.azure.storage.queue.QueueClientBuilder;
import com.azure.storage.queue.models.QueueMessageItem;
import com.azure.storage.queue.models.QueueStorageException;
import io.quarkus.runtime.StartupEvent;
import io.quarkus.scheduler.Scheduled;
import io.quarkus.vertx.core.runtime.context.VertxContextSafetyToggle;
import io.smallrye.common.vertx.VertxContext;
import io.smallrye.mutiny.Uni;
import io.vertx.core.Context;
import io.vertx.mutiny.core.Vertx;
import it.pagopa.selfcare.webhook.entity.RetryPolicy;
import it.pagopa.selfcare.webhook.entity.WebhookNotification;
import it.pagopa.selfcare.webhook.repository.WebhookNotificationRepository;
import it.pagopa.selfcare.webhook.repository.WebhookRepository;
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
  @Inject WebhookRepository webhookRepository;
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
    } catch (QueueStorageException e) {
      if (e.getStatusCode() == 404) {
        // The queue may not be fully provisioned yet right after startup (e.g. local
        // emulator): recreate it and retry on the next poll instead of failing loudly.
        log.warn("Storage Queue {} not found yet, attempting to recreate it", queue);
        client.createIfNotExists();
      } else {
        log.error("Storage Queue polling error: {}", e.getMessage(), e);
      }
    } catch (Exception e) {
      log.error("Storage Queue polling error: {}", e.getMessage(), e);
    }
  }

  private void processMessage(QueueMessageItem message) {
    String notificationId = getMessageBody(message);
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
            notification ->
                notification == null
                    ? shouldDiscardUnclaimedMessage(notificationId)
                    : processClaimedNotification(notification, message))
        .subscribe()
        .with(
            shouldDelete -> {
              if (Boolean.TRUE.equals(shouldDelete)) {
                deleteMessage(message);
              } else {
                // Leave the message in the queue: its visibility has been (re)scheduled
                // according to the webhook's retry policy, or it will fall back to the default
                // visibility timeout, triggering a natural retry.
                log.debug(
                    "Leaving Storage Queue message {} in queue for notification {}",
                    message.getMessageId(),
                    notificationId);
              }
            },
            error -> {
              log.error("Unable to process Storage Queue notification {}", notificationId, error);
              // Leave the message in the queue for retry after the visibility timeout expires.
            });
  }

  private Uni<Boolean> processClaimedNotification(
      WebhookNotification notification, QueueMessageItem message) {
    return notificationService
        .processNotification(notification)
        // Release the lock regardless of success or failure: without this, a failure raised
        // after the claim (e.g. an unexpected exception) would leave the lock held until it
        // expires, delaying any retry.
        .eventually(() -> notificationRepository.releaseProcessingLock(notification))
        .onItem()
        .transformToUni(ignored -> applyRetryBackoffIfNeeded(notification, message));
  }

  private Uni<Boolean> applyRetryBackoffIfNeeded(
      WebhookNotification notification, QueueMessageItem message) {
    if (notification.getStatus() != WebhookNotification.NotificationStatus.RETRY) {
      return Uni.createFrom().item(true);
    }
    // Honor the webhook's configured retry policy (initialDelayMs / backoffMultiplier /
    // maxDelayMs) by extending the Storage Queue message visibility for the computed backoff
    // duration, instead of relying on the fixed visibility-timeout-seconds for every attempt.
    return webhookRepository
        .findById(notification.getWebhookId())
        .onItem()
        .transform(webhook -> webhook != null ? webhook.getRetryPolicy() : null)
        .onFailure()
        .recoverWithItem((RetryPolicy) null)
        .onItem()
        .invoke(
            retryPolicy -> {
              Duration delay = computeRetryDelay(retryPolicy, notification.getAttemptCount());
              try {
                client.updateMessage(
                    message.getMessageId(), message.getPopReceipt(), getMessageBody(message), delay);
              } catch (Exception e) {
                log.warn(
                    "Unable to apply retry backoff to Storage Queue message {}: {}",
                    message.getMessageId(),
                    e.getMessage());
              }
            })
        .onItem()
        .transform(ignored -> false);
  }

  private Duration computeRetryDelay(RetryPolicy retryPolicy, Integer attemptCount) {
    long initialDelayMs =
        retryPolicy != null && retryPolicy.getInitialDelayMs() != null
            ? retryPolicy.getInitialDelayMs()
            : 1000L;
    long maxDelayMs =
        retryPolicy != null && retryPolicy.getMaxDelayMs() != null
            ? retryPolicy.getMaxDelayMs()
            : 10000L;
    double backoffMultiplier =
        retryPolicy != null && retryPolicy.getBackoffMultiplier() != null
            ? retryPolicy.getBackoffMultiplier()
            : 2.0;
    int attempt = attemptCount != null ? Math.max(attemptCount, 1) : 1;
    long delayMs = Math.round(initialDelayMs * Math.pow(backoffMultiplier, attempt - 1));
    return Duration.ofMillis(Math.min(delayMs, maxDelayMs));
  }

  private Uni<Boolean> shouldDiscardUnclaimedMessage(String notificationId) {
    // claimForProcessing returned null: either the notification no longer exists / already
    // reached a terminal status (safe to discard the message), or it is still locked by another
    // in-flight attempt (still being processed, or abandoned mid-flight by a worker that crashed
    // before releasing the lock). Deleting the message in the latter case would permanently lose
    // the notification once the active lock eventually expires with nobody left to retry it, so
    // only delete when the notification is genuinely missing or terminal.
    return notificationRepository
        .findById(new ObjectId(notificationId))
        .onItem()
        .transform(
            existing ->
                existing == null
                    || existing.getStatus() == WebhookNotification.NotificationStatus.DELIVERED
                    || existing.getStatus() == WebhookNotification.NotificationStatus.FAILED);
  }

  private void deleteMessage(QueueMessageItem message) {
    client.deleteMessage(message.getMessageId(), message.getPopReceipt());
  }

  /**
   * Reads the queue message content via the non-deprecated {@link QueueMessageItem#getBody()}
   * (returns {@link com.azure.core.util.BinaryData}) instead of the deprecated {@code
   * getMessageText()}.
   */
  private static String getMessageBody(QueueMessageItem message) {
    return message.getBody() == null ? null : message.getBody().toString();
  }
}
