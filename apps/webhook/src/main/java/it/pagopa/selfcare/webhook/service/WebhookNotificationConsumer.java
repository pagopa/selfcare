package it.pagopa.selfcare.webhook.service;

import com.azure.identity.DefaultAzureCredentialBuilder;
import com.azure.storage.queue.QueueClient;
import com.azure.storage.queue.QueueClientBuilder;
import com.azure.storage.queue.models.QueueMessageItem;
import com.azure.storage.queue.models.QueueStorageException;
import io.quarkus.runtime.StartupEvent;
import io.quarkus.scheduler.Scheduled;
import io.quarkus.scheduler.Scheduled.ConcurrentExecution;
import io.quarkus.vertx.core.runtime.context.VertxContextSafetyToggle;
import io.smallrye.common.vertx.VertxContext;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import io.vertx.core.Context;
import io.vertx.mutiny.core.Vertx;
import it.pagopa.selfcare.webhook.entity.RetryPolicy;
import it.pagopa.selfcare.webhook.entity.WebhookNotification;
import it.pagopa.selfcare.webhook.metrics.WebhookMetrics;
import it.pagopa.selfcare.webhook.repository.WebhookNotificationRepository;
import it.pagopa.selfcare.webhook.repository.WebhookRepository;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;
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

  @ConfigProperty(name = "webhook.storage-queue.max-messages-per-poll", defaultValue = "32")
  int maxMessagesPerPoll;

  /**
   * A single tick keeps draining the queue while it returns full batches, instead of delivering at
   * most {@code max-messages-per-poll} messages per interval (32 every 5s would cap a replica at
   * ~6 messages/s regardless of the available capacity). Bounded so that a long backlog cannot
   * hold the scheduler worker thread indefinitely.
   */
  @ConfigProperty(name = "webhook.storage-queue.max-batches-per-poll", defaultValue = "4")
  int maxBatchesPerPoll;

  @ConfigProperty(name = "webhook.storage-queue.visibility-timeout-seconds", defaultValue = "300")
  int visibilityTimeoutSeconds;

  /**
   * Extra time the MongoDB processing lock is held on top of the message visibility timeout. The
   * lock must outlive the invisibility window: were they equal (or the lock shorter), a delivery
   * slower than the visibility timeout would let the redelivered message be claimed by another
   * replica while the first one is still in flight, sending the webhook twice.
   */
  @ConfigProperty(name = "webhook.storage-queue.processing-lock-margin-seconds", defaultValue = "60")
  int processingLockMarginSeconds;

  @ConfigProperty(name = "webhook.storage-queue.max-in-flight", defaultValue = "32")
  int maxInFlight;

  /**
   * Azure Storage Queues have no native dead-letter support: a message that keeps failing (e.g.
   * because MongoDB is unreachable) would be redelivered until its 7-day TTL expires, burning
   * in-flight capacity forever. Past this dequeue count the message is moved to the poison queue
   * for manual inspection.
   */
  @ConfigProperty(name = "webhook.storage-queue.max-dequeue-count", defaultValue = "5")
  int maxDequeueCount;

  /** Defaults to {@code <queue>-poison}, the naming convention used by Azure Functions. */
  @ConfigProperty(name = "webhook.storage-queue.poison-queue")
  Optional<String> poisonQueue;

  private volatile QueueClient client;
  private volatile QueueClient poisonClient;
  Semaphore inFlight;

  @PostConstruct
  void initConcurrencyLimit() {
    if (maxInFlight <= 0) {
      throw new IllegalStateException("webhook.storage-queue.max-in-flight must be greater than 0");
    }
    if (maxMessagesPerPoll > maxInFlight) {
      log.warn(
          "webhook.storage-queue.max-messages-per-poll ({}) exceeds max-in-flight ({}): each poll"
              + " is capped at the free capacity anyway, the extra value has no effect",
          maxMessagesPerPoll,
          maxInFlight);
    }
    inFlight = new Semaphore(maxInFlight);
  }

  /**
   * Duration of the MongoDB processing lock, always longer than the message visibility timeout so
   * that a redelivered message cannot be claimed while the previous attempt is still in flight.
   */
  int processingLockMinutes() {
    return (int)
        Math.max(1, Math.ceil((visibilityTimeoutSeconds + processingLockMarginSeconds) / 60.0));
  }

  void start(@Observes StartupEvent event) {
    if (!enabled) {
      return;
    }
    client = buildClientBuilder(queue).buildClient();
    poisonClient = buildClientBuilder(poisonQueueName()).buildClient();
    ensureQueueExists(client, queue);
    ensureQueueExists(poisonClient, poisonQueueName());
  }

  String poisonQueueName() {
    return poisonQueue == null
        ? queue + "-poison"
        : poisonQueue.filter(name -> !name.isBlank()).orElseGet(() -> queue + "-poison");
  }

  /**
   * Creates the queue only when explicitly enabled (local/emulator setups). In the cloud the queue
   * is provisioned by Terraform and the managed identity only holds message level roles, so the
   * create call would fail with a 403 and abort the whole application startup.
   */
  private void ensureQueueExists(QueueClient queueClient, String queueName) {
    if (!autoCreate || queueClient == null) {
      return;
    }
    try {
      queueClient.createIfNotExists();
    } catch (RuntimeException e) {
      log.warn("Unable to auto-create Storage Queue {}: {}", queueName, e.getMessage());
    }
  }

  QueueClientBuilder buildClientBuilder(String queueName) {
    QueueClientBuilder clientBuilder = new QueueClientBuilder().queueName(queueName);
    if ("none".equals(connectionString)) {
      clientBuilder.endpoint(endpoint).credential(new DefaultAzureCredentialBuilder().build());
    } else {
      clientBuilder.connectionString(connectionString);
    }
    return clientBuilder;
  }

  /**
   * Runs a blocking Storage Queue call on the worker pool. Every operation reached from {@link
   * #processNotification} executes on a Vert.x duplicated context (i.e. an event loop thread), so
   * calling the synchronous Azure SDK inline would block the event loop for the whole REST
   * round-trip, stalling every other delivery and MongoDB callback sharing that loop.
   */
  private Uni<Void> onWorkerPool(Runnable queueOperation) {
    return Uni.createFrom()
        .item(
            () -> {
              queueOperation.run();
              return null;
            })
        .runSubscriptionOn(Infrastructure.getDefaultWorkerPool())
        .replaceWithVoid();
  }

  /**
   * {@link ConcurrentExecution#SKIP} is required because the poll is blocking (the Storage Queue
   * SDK is synchronous): if a poll outlives the interval, a second one would start and dequeue
   * messages that the in-flight limiter is unable to dispatch, hiding them for a whole visibility
   * timeout for nothing.
   */
  @Scheduled(
      every = "${webhook.storage-queue.poll-interval:5s}",
      concurrentExecution = ConcurrentExecution.SKIP)
  void poll() {
    if (!enabled || client == null) {
      return;
    }
    try {
      for (int batch = 0; batch < maxBatchesPerPoll; batch++) {
        int available = inFlight.availablePermits();
        if (available <= 0) {
          log.debug(
              "Stopping Storage Queue poll, {} deliveries already in flight (max {})",
              maxInFlight - available,
              maxInFlight);
          return;
        }
        int toReceive = Math.min(maxMessagesPerPoll, available);
        if (receiveAndDispatch(toReceive) < toReceive) {
          // Fewer messages than requested means the queue is drained: stop here and let the next
          // tick pick up whatever arrives in the meantime.
          return;
        }
      }
    } catch (QueueStorageException e) {
      if (e.getStatusCode() == 404) {
        // The queue may not be fully provisioned yet right after startup (e.g. local
        // emulator): recreate it and retry on the next poll instead of failing loudly.
        log.warn("Storage Queue {} not found yet, attempting to recreate it", queue);
        ensureQueueExists(client, queue);
        ensureQueueExists(poisonClient, poisonQueueName());
      } else {
        log.error("Storage Queue polling error: {}", e.getMessage(), e);
      }
    } catch (Exception e) {
      log.error("Storage Queue polling error: {}", e.getMessage(), e);
    }
  }

  private int receiveAndDispatch(int toReceive) {
    int received = 0;
    for (QueueMessageItem message :
        client.receiveMessages(toReceive, Duration.ofSeconds(visibilityTimeoutSeconds), null, null)) {
      received++;
      processMessage(message);
    }
    return received;
  }

  private void processMessage(QueueMessageItem message) {
    String notificationId = getMessageBody(message);
    // ObjectId.isValid(null) throws IllegalArgumentException, which would escape processMessage,
    // abort the rest of the poll batch and leave this poison message in the queue forever.
    if (notificationId == null || !ObjectId.isValid(notificationId)) {
      log.error("Discarding Storage Queue message with invalid notification ID: {}", notificationId);
      metrics.recordDiscarded("invalid_notification_id");
      deleteMessageBlocking(message);
      return;
    }

    Context processingContext = VertxContext.getOrCreateDuplicatedContext(vertx.getDelegate());
    VertxContextSafetyToggle.setContextSafe(processingContext, true);
    if (!inFlight.tryAcquire()) {
      log.debug(
          "Leaving Storage Queue message {} unprocessed because the in-flight limit ({}) was reached",
          message.getMessageId(),
          maxInFlight);
      makeMessageVisibleAgain(message);
      return;
    }
    AtomicBoolean permitReleased = new AtomicBoolean(false);
    try {
      processingContext.runOnContext(
          ignored -> processNotification(message, notificationId, permitReleased));
    } catch (RuntimeException e) {
      releaseInFlightPermit(permitReleased);
      throw e;
    }
  }

  /**
   * The message has already been dequeued with the configured visibility timeout, so simply
   * skipping it would hide it for minutes even though capacity is usually freed within seconds.
   * Resetting its visibility to zero puts it back at the disposal of the next poll (of this or any
   * other replica) immediately. Runs on the scheduler worker thread, so the blocking Storage Queue
   * call is safe here.
   */
  private void makeMessageVisibleAgain(QueueMessageItem message) {
    try {
      // A null message text leaves the payload untouched, only the visibility timeout is updated.
      client.updateMessage(message.getMessageId(), message.getPopReceipt(), null, Duration.ZERO);
    } catch (Exception e) {
      log.warn(
          "Unable to reset visibility of Storage Queue message {}: {}",
          message.getMessageId(),
          e.getMessage());
    }
  }

  /**
   * The permit must be given back exactly once per dispatched message: releasing it twice (e.g.
   * from both a subscription callback and an exception handler) would permanently inflate the
   * semaphore and silently break the concurrency limit.
   */
  private void releaseInFlightPermit(AtomicBoolean permitReleased) {
    if (permitReleased.compareAndSet(false, true)) {
      inFlight.release();
    }
  }

  private void processNotification(
      QueueMessageItem message, String notificationId, AtomicBoolean permitReleased) {
    try {
      handleMessage(message, notificationId)
          // Single release point: covers completion, failure and cancellation (e.g. the Vert.x
          // context being closed on shutdown), which per-callback releases could not. Being at
          // the end of the chain, the permit is now given back only once the queue operation
          // (delete / visibility update) has actually completed.
          .onTermination()
          .invoke(() -> releaseInFlightPermit(permitReleased))
          .subscribe()
          .with(
              ignored -> {},
              error ->
                  log.error(
                      "Unable to process Storage Queue notification {}", notificationId, error)
              // Leave the message in the queue for retry after the visibility timeout expires.
              );
    } catch (RuntimeException e) {
      releaseInFlightPermit(permitReleased);
      throw e;
    }
  }

  private Uni<Void> handleMessage(QueueMessageItem message, String notificationId) {
    if (message.getDequeueCount() > maxDequeueCount) {
      return moveToPoisonQueue(message, notificationId);
    }
    return notificationRepository
        .claimForProcessing(notificationId, processingLockMinutes())
        .onItem()
        .invoke(notification -> metrics.recordClaim("queue", notification != null ? 1 : 0))
        .onItem()
        .transformToUni(
            notification ->
                notification == null
                    ? shouldDiscardUnclaimedMessage(notificationId)
                    : processClaimedNotification(notification, message))
        .onItem()
        .transformToUni(
            shouldDelete ->
                Boolean.TRUE.equals(shouldDelete)
                    ? deleteMessage(message)
                    // Leave the message in the queue: its visibility has been (re)scheduled
                    // according to the webhook's retry policy, or it will fall back to the default
                    // visibility timeout, triggering a natural retry.
                    : Uni.createFrom()
                        .voidItem()
                        .invoke(
                            () ->
                                log.debug(
                                    "Leaving Storage Queue message {} in queue for notification {}",
                                    message.getMessageId(),
                                    notificationId)));
  }

  /**
   * Copies the message to the poison queue and only then removes it from the delivery queue, so a
   * failure of either step leaves the message in place for a later attempt rather than losing it.
   * The notification is also marked as permanently failed: with its message gone, nothing would
   * ever move it out of PENDING/RETRY again.
   */
  private Uni<Void> moveToPoisonQueue(QueueMessageItem message, String notificationId) {
    String reason =
        String.format(
            "Message exceeded the maximum dequeue count (%d) and was moved to the poison queue",
            maxDequeueCount);
    log.error(
        "Storage Queue message {} for notification {} was dequeued {} times, moving it to {}",
        message.getMessageId(),
        notificationId,
        message.getDequeueCount(),
        poisonQueueName());
    return onWorkerPool(() -> poisonClient.sendMessage(getMessageBody(message)))
        .onItem()
        .transformToUni(
            ignored ->
                notificationRepository.markAsPermanentlyFailed(
                    new ObjectId(notificationId), reason))
        .onItem()
        .transformToUni(ignored -> deleteMessage(message))
        .onItem()
        .invoke(() -> metrics.recordDiscarded("max_dequeue_count"))
        .onFailure()
        .recoverWithUni(
            error -> {
              log.error(
                  "Unable to move Storage Queue message {} to the poison queue",
                  message.getMessageId(),
                  error);
              return Uni.createFrom().voidItem();
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
        .transformToUni(
            retryPolicy -> {
              Duration delay = computeRetryDelay(retryPolicy, notification.getAttemptCount());
              return onWorkerPool(
                      () ->
                          client.updateMessage(
                              message.getMessageId(),
                              message.getPopReceipt(),
                              getMessageBody(message),
                              delay))
                  .onFailure()
                  .recoverWithUni(
                      error -> {
                        log.warn(
                            "Unable to apply retry backoff to Storage Queue message {}: {}",
                            message.getMessageId(),
                            error.getMessage());
                        return Uni.createFrom().voidItem();
                      });
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
                    || existing.getStatus() == WebhookNotification.NotificationStatus.FAILED)
        .onItem()
        .invoke(
            shouldDiscard -> {
              if (Boolean.TRUE.equals(shouldDiscard)) {
                metrics.recordDiscarded("notification_missing_or_terminal");
              }
            });
  }

  /**
   * Deletes the message off the event loop: this runs at the tail of the reactive delivery
   * pipeline, whose thread must never be blocked by the synchronous Storage Queue SDK.
   */
  private Uni<Void> deleteMessage(QueueMessageItem message) {
    return onWorkerPool(() -> deleteMessageBlocking(message));
  }

  /** Variant for callers already running on a worker thread (the scheduler poll). */
  private void deleteMessageBlocking(QueueMessageItem message) {
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

  /** Exposes the underlying Storage Queue client for the readiness probe. Returns {@code null}
   * when the Storage Queue integration is disabled or not yet initialized. */
  public QueueClient getClient() {
    return client;
  }
}
