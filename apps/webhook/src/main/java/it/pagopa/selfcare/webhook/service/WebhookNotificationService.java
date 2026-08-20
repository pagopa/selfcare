package it.pagopa.selfcare.webhook.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.vertx.core.http.HttpMethod;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.ext.web.codec.BodyCodec;
import io.vertx.mutiny.ext.web.client.HttpRequest;
import io.vertx.mutiny.ext.web.client.HttpResponse;
import io.vertx.mutiny.ext.web.client.WebClient;
import it.pagopa.selfcare.webhook.entity.Webhook;
import it.pagopa.selfcare.webhook.entity.WebhookNotification;
import it.pagopa.selfcare.webhook.entity.WebhookNotificationAttempt;
import it.pagopa.selfcare.webhook.metrics.WebhookMetrics;
import it.pagopa.selfcare.webhook.repository.WebhookNotificationAttemptRepository;
import it.pagopa.selfcare.webhook.repository.WebhookNotificationRepository;
import it.pagopa.selfcare.webhook.repository.WebhookRepository;
import it.pagopa.selfcare.webhook.util.DataEncryptionConfig;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import java.net.URI;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@Slf4j
@ApplicationScoped
public class WebhookNotificationService {

  @Inject WebhookRepository webhookRepository;

  @Inject WebhookNotificationRepository notificationRepository;

  @Inject WebhookNotificationAttemptRepository notificationAttemptRepository;

  @Inject WebhookMetrics metrics;

  @Inject Vertx vertx;

  @Inject WebhookJwtService webhookJwtService;

  @Inject ObjectMapper objectMapper;

  @ConfigProperty(name = "webhook.timeout.connect", defaultValue = "5000")
  int connectTimeout;

  @ConfigProperty(name = "webhook.timeout.read", defaultValue = "10000")
  int readTimeout;

  @ConfigProperty(name = "webhook.http.max-pool-size", defaultValue = "10")
  int maxPoolSize;

  @ConfigProperty(name = "webhook.http.max-wait-queue-size", defaultValue = "50")
  int maxWaitQueueSize;

  @ConfigProperty(name = "webhook.http.max-concurrent-deliveries", defaultValue = "10")
  int maxConcurrentDeliveries;

  @ConfigProperty(name = "webhook.jwt.header-name", defaultValue = "Authorization")
  String jwtHeaderName;

  @ConfigProperty(name = "webhook.jwt.header-prefix", defaultValue = "Bearer ")
  String jwtHeaderPrefix;

  private volatile WebClient webClient;

  /**
   * Eagerly creates the shared {@link WebClient} once, at application startup. CDI startup
   * observers run single-threaded before any request is processed, so this avoids the previous
   * lazy, unsynchronized {@code init()} check-then-act pattern that could race under concurrent
   * notification processing and create (and leak) multiple clients/connection pools.
   */
  void onStart(@Observes StartupEvent event) {
    WebClientOptions options =
        new WebClientOptions()
            .setConnectTimeout(connectTimeout)
            .setIdleTimeoutUnit(TimeUnit.MILLISECONDS)
            .setIdleTimeout(readTimeout)
            .setMaxPoolSize(maxPoolSize)
            .setMaxWaitQueueSize(maxWaitQueueSize)
            .setKeepAlive(true)
            .setFollowRedirects(true);
    this.webClient = WebClient.create(vertx, options);
  }

  /** Releases the underlying HTTP connection pool on shutdown. */
  void onStop(@Observes ShutdownEvent event) {
    if (webClient != null) {
      webClient.close();
    }
  }

  public Uni<Void> processFailedNotifications() {
    // Lock notifications for 5 minutes - if processing takes longer, lock expires
    return notificationRepository
        .findAndLockPendingNotifications(100, 5)
        .onItem()
        .invoke(notifications -> metrics.recordClaim("batch", notifications.size()))
        .onItem()
        .transformToUni(
            notifications -> {
              if (notifications.isEmpty()) {
                return Uni.createFrom().voidItem();
              }
              log.info("Processing {} pending notifications", notifications.size());
              return Multi.createFrom()
                  .iterable(notifications)
                  .onItem()
                  .transformToUni(
                      notification ->
                          processNotification(notification)
                              .onFailure()
                              .recoverWithUni(
                                  error -> {
                                    log.error(
                                        "Error processing notification {} {}",
                                        notification.getId(),
                                        error.getMessage());
                                    return notificationRepository
                                        .releaseProcessingLock(notification)
                                        .replaceWithVoid();
                                  })
                              .onItem()
                              .transformToUni(
                                  v ->
                                      notificationRepository
                                          .releaseProcessingLock(notification)
                                          .replaceWithVoid()))
                  .merge(maxConcurrentDeliveries)
                  .collect()
                  .asList()
                  .replaceWithVoid();
            });
  }

  public Uni<Void> processNotification(String notificationId) {
    return notificationRepository
        .findById(new ObjectId(notificationId))
        .onItem()
        .transformToUni(
            notification -> {
              if (notification == null) {
                log.info("Notification not found: {}", notificationId);
                return Uni.createFrom().voidItem();
              }
              return sendNotification(notification);
            });
  }

  private Uni<Void> sendNotification(WebhookNotification notification) {
    return webhookRepository
        .findById(notification.getWebhookId())
        .onItem()
        .transformToUni(
            webhook -> {
              if (webhook == null) {
                log.error(
                    "Webhook not found for notification: {}", notification.getId().toString());
                return markNotificationAsFailed(notification, "Webhook not found")
                    .replaceWithVoid();
              }
              return processNotification(notification, webhook);
            });
  }

  public Uni<Void> processNotification(WebhookNotification notification) {
    return Uni.createFrom().item(notification).onItem().transformToUni(this::sendNotification);
  }

  public Uni<Void> processNotification(WebhookNotification notification, Webhook webhook) {
    if (webhook.getStatus() != Webhook.WebhookStatus.ACTIVE) {
      log.warn("Webhook is not active: {}", webhook.getId());
      return markNotificationAsFailed(notification, "Webhook is not active").replaceWithVoid();
    }

    notification.setStatus(WebhookNotification.NotificationStatus.SENDING);
    notification.setLastAttemptAt(LocalDateTime.now());
    return notificationRepository
        .update(notification)
        .onItem()
        .transformToUni(updated -> sendHttpRequest(webhook, updated));
  }

  private Uni<Void> sendHttpRequest(Webhook webhook, WebhookNotification notification) {
    long startNanos = System.nanoTime();
    HttpRequest<Void> request;
    try {
      request = buildRequest(webhook, notification);
    } catch (Exception e) {
      log.error("Error sending webhook notification: {} {}", notification.getId(), e.getMessage());
      metrics.recordDeliveryDuration(elapsedMs(startNanos));
      return handleHttpError(webhook, notification, e);
    }

    HttpRequest<Void> preparedRequest = request;
    return webhookJwtService
        .generateNotificationToken(webhook, notification)
        .onItem()
        .invoke(token -> preparedRequest.putHeader(jwtHeaderName, jwtHeaderPrefix + token))
        .onItem()
        .transformToUni(token -> sendDecodedPayload(preparedRequest, notification))
        // Both branches are handled in a single stage on purpose: attaching a generic
        // .onFailure().recoverWithUni(...) *after* the response-handling stage would also catch
        // failures raised by handleHttpResponse itself (e.g. the MongoDB update after a 2xx).
        // That would record the delivery twice, then overwrite an already DELIVERED notification
        // with RETRY/FAILED and append a duplicate attempt record for the same attempt number.
        .onItemOrFailure()
        .transformToUni(
            (response, throwable) -> {
              metrics.recordDeliveryDuration(elapsedMs(startNanos));
              return throwable != null
                  ? handleHttpError(webhook, notification, throwable)
                  : handleHttpResponse(webhook, notification, response);
            });
  }

  private HttpRequest<Void> buildRequest(Webhook webhook, WebhookNotification notification) {
    URI uri = URI.create(webhook.getUrl());
    int port = uri.getPort() != -1 ? uri.getPort() : (uri.getScheme().equals("https") ? 443 : 80);
    String path = uri.getPath().isEmpty() ? "/" : uri.getPath();
    if (uri.getRawQuery() != null && !uri.getRawQuery().isBlank()) {
      path = path + "?" + uri.getRawQuery();
    }

    HttpRequest<Void> request =
        webClient
            .request(
                HttpMethod.valueOf(webhook.getHttpMethod().toUpperCase()), port, uri.getHost(), path)
            // Webhook responses are not part of the delivery contract. Discard each response
            // chunk as it arrives instead of buffering an untrusted response body in memory.
            .as(BodyCodec.none())
            .ssl(uri.getScheme().equals("https"))
            .timeout(readTimeout)
            .putHeader("Content-Type", "application/json")
            .putHeader("X-Webhook-Notification-Id", notification.getId().toHexString());

    // Add custom headers
    if (webhook.getHeaders() != null) {
      DataEncryptionConfig.decrypt(webhook.getHeaders()).forEach(request::putHeader);
    }
    return request;
  }

  private static long elapsedMs(long startNanos) {
    return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos);
  }

  private Uni<HttpResponse<Void>> sendDecodedPayload(
      HttpRequest<Void> request, WebhookNotification notification) {
    try {
      return request.sendJson(decodePayload(notification));
    } catch (JsonProcessingException e) {
      return Uni.createFrom().failure(e);
    }
  }

  private Map<String, Object> decodePayload(WebhookNotification notification)
      throws JsonProcessingException {
    return objectMapper.readValue(
        DataEncryptionConfig.decrypt(notification.getPayload()), new TypeReference<>() {});
  }

  private Uni<Void> handleHttpResponse(
      Webhook webhook, WebhookNotification notification, HttpResponse<Void> response) {
    int statusCode = response.statusCode();

    if (statusCode >= 200 && statusCode < 300) {
      int attemptNumber = notification.getAttemptCount() + 1;
      notification.setStatus(WebhookNotification.NotificationStatus.DELIVERED);
      notification.setCompletedAt(LocalDateTime.now());
      log.info(
          "Webhook notification delivered: {}, status: {}",
          notification.getId(),
          statusCode);
      metrics.recordDelivery("delivered");
      return recordAttempt(
              notification,
              attemptNumber,
              WebhookNotification.NotificationStatus.DELIVERED,
              statusCode,
              null)
          .onItem()
          .transformToUni(ignored -> notificationRepository.update(notification).replaceWithVoid());
    } else {
      String errorMessage = String.format("HTTP error %d", statusCode);
      return handleFailure(webhook, notification, errorMessage, statusCode);
    }
  }

  private Uni<Void> handleHttpError(
      Webhook webhook, WebhookNotification notification, Throwable throwable) {
    String errorMessage = throwable.getMessage();
    log.error(
        "HTTP request failed for notification: {} {}",
        notification.getId(),
        throwable.getMessage());
    return handleFailure(webhook, notification, errorMessage, null);
  }

  private Uni<Void> handleFailure(
      Webhook webhook, WebhookNotification notification, String errorMessage, Integer statusCode) {
    int attemptNumber = notification.getAttemptCount() + 1;
    notification.setAttemptCount(attemptNumber);
    notification.setLastError(errorMessage);

    int maxAttempts =
        webhook.getRetryPolicy() != null ? webhook.getRetryPolicy().getMaxAttempts() : 3;

    if (attemptNumber >= maxAttempts) {
      return markNotificationAsFailed(notification, errorMessage, statusCode, attemptNumber)
          .replaceWithVoid();
    } else {
      notification.setStatus(WebhookNotification.NotificationStatus.RETRY);
      log.warn(
          "Webhook notification will be retried: {}, attempt: {}/{}",
          notification.getId(),
          attemptNumber,
          maxAttempts);
      metrics.recordDelivery("retry");
      return recordAttempt(
              notification, attemptNumber, WebhookNotification.NotificationStatus.RETRY, statusCode, errorMessage)
          .onItem()
          .transformToUni(ignored -> notificationRepository.update(notification).replaceWithVoid());
    }
  }

  private Uni<WebhookNotification> markNotificationAsFailed(
      WebhookNotification notification, String errorMessage) {
    return markNotificationAsFailed(
        notification, errorMessage, null, notification.getAttemptCount() + 1);
  }

  private Uni<WebhookNotification> markNotificationAsFailed(
      WebhookNotification notification, String errorMessage, Integer statusCode, int attemptNumber) {
    notification.setStatus(WebhookNotification.NotificationStatus.FAILED);
    notification.setLastError(errorMessage);
    notification.setCompletedAt(LocalDateTime.now());
    log.error(
        "Webhook notification failed permanently: {}, error: {}",
        notification.getId(),
        errorMessage);
    metrics.recordDelivery("failed");
    return recordAttempt(
            notification, attemptNumber, WebhookNotification.NotificationStatus.FAILED, statusCode, errorMessage)
        .onItem()
        .transformToUni(ignored -> notificationRepository.update(notification));
  }

  /**
   * Appends an immutable history record for the current delivery attempt instead of overwriting
   * the notification's own {@code lastError}/{@code lastAttemptAt} fields. This preserves the
   * full retry history even though the parent {@link WebhookNotification} document is reused and
   * mutated across every retry. Failing to persist the history entry does not interrupt the main
   * delivery flow: the error is logged and the attempt record is returned as-is.
   */
  private Uni<WebhookNotificationAttempt> recordAttempt(
      WebhookNotification notification,
      int attemptNumber,
      WebhookNotification.NotificationStatus outcome,
      Integer statusCode,
      String errorMessage) {
    WebhookNotificationAttempt attempt = new WebhookNotificationAttempt();
    attempt.setNotificationId(notification.getId());
    attempt.setWebhookId(notification.getWebhookId());
    attempt.setAttemptNumber(attemptNumber);
    attempt.setOutcome(outcome);
    attempt.setStatusCode(statusCode);
    attempt.setErrorMessage(errorMessage);
    attempt.setStartedAt(
        notification.getLastAttemptAt() != null ? notification.getLastAttemptAt() : LocalDateTime.now());
    attempt.setFinishedAt(LocalDateTime.now());
    return notificationAttemptRepository
        .persist(attempt)
        .onFailure()
        .recoverWithItem(
            error -> {
              log.error(
                  "Unable to persist delivery attempt history for notification {} (attempt {})",
                  notification.getId(),
                  attemptNumber,
                  error);
              return attempt;
            });
  }
}
