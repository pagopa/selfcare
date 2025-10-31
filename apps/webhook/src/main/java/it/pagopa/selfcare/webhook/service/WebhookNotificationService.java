package it.pagopa.selfcare.webhook.service;

import io.quarkus.logging.Log;
import io.quarkus.scheduler.Scheduled;
import io.smallrye.mutiny.Uni;
import io.vertx.core.http.HttpMethod;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.core.buffer.Buffer;
import io.vertx.mutiny.ext.web.client.HttpResponse;
import io.vertx.mutiny.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import it.pagopa.selfcare.webhook.entity.Webhook;
import it.pagopa.selfcare.webhook.entity.WebhookNotification;
import it.pagopa.selfcare.webhook.repository.WebhookNotificationRepository;
import it.pagopa.selfcare.webhook.repository.WebhookRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.bson.types.ObjectId;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.List;

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
        return notificationRepository.findPendingNotifications()
                .onItem().transformToUni(notifications -> {
                    if (notifications.isEmpty()) {
                        return Uni.createFrom().voidItem();
                    }
                    List<Uni<Void>> processes = notifications.stream()
                            .map(notification -> processNotification(notification.getId().toString())
                                    .onFailure().recoverWithNull())
                            .toList();
                    return Uni.join().all(processes).andFailFast()
                            .replaceWithVoid();
                });
    }
    
    public Uni<Void> processNotification(String notificationId) {
        init();
        return notificationRepository.findById(new ObjectId(notificationId))
                .onItem().ifNotNull().transformToUni(notification ->
                        webhookRepository.findById(notification.getWebhookId())
                                .onItem().ifNotNull().transformToUni(webhook ->
                                        processNotification(notification, webhook))
                                .onItem().ifNull().continueWith(() -> {
                                    Log.errorf("Webhook not found for notification: %s", notificationId);
                                    return markNotificationAsFailed(notification, "Webhook not found")
                                            .replaceWithVoid();
                                }))
                .onItem().ifNull().continueWith(() -> {
                    Log.warnf("Notification not found: %s", notificationId);
                    return Uni.createFrom().voidItem();
                })
                .onItem().ifNotNull().transformToUni(uni -> uni);
    }

    public Uni<Void> processNotification(WebhookNotification notification, Webhook webhook) {
        if (webhook.getStatus() != Webhook.WebhookStatus.ACTIVE) {
            Log.warnf("Webhook is not active: %s", webhook.getId());
            return markNotificationAsFailed(notification, "Webhook is not active")
                    .replaceWithVoid();
        }
        
        notification.setStatus(WebhookNotification.NotificationStatus.SENDING);
        notification.setLastAttemptAt(LocalDateTime.now());
        
        return notificationRepository.update(notification)
                .onItem().transformToUni(updated -> sendHttpRequest(webhook, updated));
    }
    
    private Uni<Void> sendHttpRequest(Webhook webhook, WebhookNotification notification) {
        try {
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
            Log.errorf(e, "Error sending webhook notification: %s", notification.getId());
            return handleHttpError(webhook, notification, e);
        }
    }
    
    private Uni<Void> handleHttpResponse(Webhook webhook, WebhookNotification notification, HttpResponse<Buffer> response) {
        int statusCode = response.statusCode();
        
        if (statusCode >= 200 && statusCode < 300) {
            notification.setStatus(WebhookNotification.NotificationStatus.SUCCESS);
            notification.setCompletedAt(LocalDateTime.now());
            Log.infof("Webhook notification sent successfully: %s, status: %d", notification.getId(), statusCode);
            return notificationRepository.update(notification).replaceWithVoid();
        } else {
            String errorMessage = String.format("HTTP error %d: %s", statusCode, response.bodyAsString());
            return handleFailure(webhook, notification, errorMessage);
        }
    }
    
    private Uni<Void> handleHttpError(Webhook webhook, WebhookNotification notification, Throwable throwable) {
        String errorMessage = throwable.getMessage();
        Log.errorf(throwable, "HTTP request failed for notification: %s", notification.getId());
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
            Log.warnf("Webhook notification will be retried: %s, attempt: %d/%d", 
                    notification.getId(), notification.getAttemptCount(), maxAttempts);
            return notificationRepository.update(notification).replaceWithVoid();
        }
    }
    
    private Uni<WebhookNotification> markNotificationAsFailed(WebhookNotification notification, String errorMessage) {
        notification.setStatus(WebhookNotification.NotificationStatus.FAILED);
        notification.setLastError(errorMessage);
        notification.setCompletedAt(LocalDateTime.now());
        Log.errorf("Webhook notification failed permanently: %s, error: %s", notification.getId(), errorMessage);
        return notificationRepository.update(notification);
    }
}
