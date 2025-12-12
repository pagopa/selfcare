package it.pagopa.selfcare.webhook.service;

import io.quarkus.scheduler.Scheduled;
import io.smallrye.mutiny.Uni;
import io.vertx.core.http.HttpMethod;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.core.buffer.Buffer;
import io.vertx.mutiny.ext.web.client.HttpResponse;
import io.vertx.mutiny.ext.web.client.WebClient;
import it.pagopa.selfcare.webhook.entity.Webhook;
import it.pagopa.selfcare.webhook.entity.WebhookNotification;
import it.pagopa.selfcare.webhook.repository.WebhookNotificationRepository;
import it.pagopa.selfcare.webhook.repository.WebhookRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@ApplicationScoped
public class WebhookNotificationService {

  @Inject
  WebhookRepository webhookRepository;

  @Inject
  WebhookNotificationRepository notificationRepository;

  @Inject
  Vertx vertx;

  @ConfigProperty(name = "webhook.timeout.connect", defaultValue = "5000")
  int connectTimeout;

  @ConfigProperty(name = "webhook.timeout.read", defaultValue = "10000")
  int readTimeout;

  private WebClient webClient;

  public void init() {
    if (webClient == null) {
      WebClientOptions options = new WebClientOptions()
        .setConnectTimeout(connectTimeout)
        .setIdleTimeout(readTimeout)
        .setFollowRedirects(true);
      this.webClient = WebClient.create(vertx, options);
    }
  }

  @Scheduled(every = "30s")
  public Uni<Void> processFailedNotifications() {
    init();
    // Lock notifications for 5 minutes - if processing takes longer, lock expires
    return notificationRepository.findAndLockPendingNotifications(100, 5)
      .onItem().transformToUni(notifications -> {
        if (notifications.isEmpty()) {
          return Uni.createFrom().voidItem();
        }
        log.info("Processing {} pending notifications", notifications.size());
        List<Uni<Void>> processes = notifications.stream().map(notification ->
            processNotification(notification)
              .onFailure().recoverWithUni(error -> {
                log.error("Error processing notification {} {}", notification.getId(), error.getMessage());
                return notificationRepository.releaseProcessingLock(notification).replaceWithVoid();
              })
              .onItem().transformToUni(v -> notificationRepository.releaseProcessingLock(notification).replaceWithVoid())
          )
          .toList();
        return Uni.join().all(processes).andFailFast().replaceWithVoid();
      });
  }

  public Uni<Void> processNotification(String notificationId) {
    return notificationRepository.findById(new ObjectId(notificationId))
      .onItem().ifNull().continueWith(() -> {
        log.info("Notification not found: {}", notificationId);
        return null;
      })
      .onItem().ifNotNull().transformToUni(this::sendNotification);
  }

  private Uni<Void> sendNotification(WebhookNotification notification) {
    return webhookRepository.findById(notification.getWebhookId())
      .onItem().ifNull().continueWith(() -> {
        log.error("Webhook not found for notification: {}", notification.getId().toString());
        Uni.createFrom().item(markNotificationAsFailed(notification, "Webhook not found"))
          .subscribe().with(item -> log.error("markNotificationAsFailed: {}", notification.getId().toString()),
            failure -> log.error("Error when markNotificationAsFailed {} {}", notification.getId().toString(), failure.getMessage()));
        return null;
      })
      .onItem().ifNotNull().transformToUni(webhook ->
        processNotification(notification, webhook));
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
    return notificationRepository.update(notification)
      .onItem().transformToUni(updated -> sendHttpRequest(webhook, updated));
  }

  private Uni<Void> sendHttpRequest(Webhook webhook, WebhookNotification notification) {
    try {
      init();
      URI uri = URI.create(webhook.getUrl());
      int port = uri.getPort() != -1 ? uri.getPort() : (uri.getScheme().equals("https") ? 443 : 80);
      String path = uri.getPath().isEmpty() ? "/" : uri.getPath();

      var request = webClient
        .request(HttpMethod.valueOf(webhook.getHttpMethod().toUpperCase()), port, uri.getHost(), path)
        .ssl(uri.getScheme().equals("https"))
        .timeout(readTimeout)
        .putHeader("Content-Type", "application/json");

      // Add custom headers
      if (webhook.getHeaders() != null) {
        webhook.getHeaders().forEach(request::putHeader);
      }

      return request.sendBuffer(Buffer.buffer(notification.getPayload()))
        .onItem().transformToUni(response -> handleHttpResponse(webhook, notification, response))
        .onFailure().recoverWithUni(throwable -> handleHttpError(webhook, notification, throwable));

    } catch (Exception e) {
      log.error("Error sending webhook notification: {} {}", notification.getId(), e.getMessage());
      return handleHttpError(webhook, notification, e);
    }
  }

  private Uni<Void> handleHttpResponse(Webhook webhook, WebhookNotification notification, HttpResponse<Buffer> response) {
    int statusCode = response.statusCode();

    if (statusCode >= 200 && statusCode < 300) {
      notification.setStatus(WebhookNotification.NotificationStatus.SUCCESS);
      notification.setCompletedAt(LocalDateTime.now());
      log.info("Webhook notification sent successfully: {}, status: {}", notification.getId(), statusCode);
      return notificationRepository.update(notification).replaceWithVoid();
    } else {
      String errorMessage = String.format("HTTP error %d: %s", statusCode, response.bodyAsString());
      return handleFailure(webhook, notification, errorMessage);
    }
  }

  private Uni<Void> handleHttpError(Webhook webhook, WebhookNotification notification, Throwable throwable) {
    String errorMessage = throwable.getMessage();
    log.error("HTTP request failed for notification: {} {}", notification.getId(), throwable.getMessage());
    return handleFailure(webhook, notification, errorMessage);
  }

  private Uni<Void> handleFailure(Webhook webhook, WebhookNotification notification, String errorMessage) {
    notification.setAttemptCount(notification.getAttemptCount() + 1);
    notification.setLastError(errorMessage);

    int maxAttempts = webhook.getRetryPolicy() != null
      ? webhook.getRetryPolicy().getMaxAttempts()
      : 3;

    if (notification.getAttemptCount() >= maxAttempts) {
      return markNotificationAsFailed(notification, errorMessage).replaceWithVoid();
    } else {
      notification.setStatus(WebhookNotification.NotificationStatus.RETRY);
      log.warn("Webhook notification will be retried: {}, attempt: {}/{}", notification.getId(), notification.getAttemptCount(), maxAttempts);
      return notificationRepository.update(notification).replaceWithVoid();
    }
  }

  private Uni<WebhookNotification> markNotificationAsFailed(WebhookNotification notification, String errorMessage) {
    notification.setStatus(WebhookNotification.NotificationStatus.FAILED);
    notification.setLastError(errorMessage);
    notification.setCompletedAt(LocalDateTime.now());
    log.error("Webhook notification failed permanently: {}, error: {}", notification.getId(), errorMessage);
    return notificationRepository.update(notification);
  }
}
