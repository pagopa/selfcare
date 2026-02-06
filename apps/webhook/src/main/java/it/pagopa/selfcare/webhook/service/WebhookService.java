package it.pagopa.selfcare.webhook.service;

import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.webhook.dto.NotificationRequest;
import it.pagopa.selfcare.webhook.dto.WebhookRequest;
import it.pagopa.selfcare.webhook.dto.WebhookResponse;
import it.pagopa.selfcare.webhook.entity.RetryPolicy;
import it.pagopa.selfcare.webhook.entity.Webhook;
import it.pagopa.selfcare.webhook.entity.WebhookNotification;
import it.pagopa.selfcare.webhook.repository.WebhookNotificationRepository;
import it.pagopa.selfcare.webhook.repository.WebhookRepository;
import it.pagopa.selfcare.webhook.util.DataEncryptionConfig;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.time.LocalDateTime;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@ApplicationScoped
public class WebhookService {

  public static final String DELETED_WEBHOOK_WITH_ID = "Deleted webhook with ID: {}";
  @Inject
  WebhookRepository webhookRepository;

  @Inject
  WebhookNotificationRepository notificationRepository;

  @Inject
  WebhookNotificationService notificationService;

  public Uni<WebhookResponse> createWebhook(WebhookRequest request) {
    Webhook webhook = new Webhook();
    webhook.setUrl(request.getUrl());
    webhook.setHttpMethod(sanitizeString(request.getHttpMethod()));
    webhook.setHeaders(DataEncryptionConfig.encrypt(request.getHeaders()));
    webhook.setProductId(request.getProductId());
    webhook.setDescription("");
    webhook.setProducts(List.of(request.getProductId()));
    webhook.setStatus(Webhook.WebhookStatus.ACTIVE);
    webhook.setCreatedAt(LocalDateTime.now());
    webhook.setUpdatedAt(LocalDateTime.now());

    if (request.getRetryPolicy() != null) {
      RetryPolicy retryPolicy = new RetryPolicy();
      retryPolicy.setMaxAttempts(request.getRetryPolicy().getMaxAttempts());
      retryPolicy.setInitialDelayMs(request.getRetryPolicy().getInitialDelayMs());
      retryPolicy.setMaxDelayMs(request.getRetryPolicy().getMaxDelayMs());
      retryPolicy.setBackoffMultiplier(request.getRetryPolicy().getBackoffMultiplier());
      webhook.setRetryPolicy(retryPolicy);
    } else {
      webhook.setRetryPolicy(new RetryPolicy());
    }

    return webhookRepository
        .persist(webhook)
        .invoke(() -> log.info("Created webhook with ID: {}", webhook.getId()))
        .map(this::toResponse);
  }

  public Uni<List<WebhookResponse>> listWebhooks() {
    return webhookRepository
        .listAll()
        .map(webhooks -> webhooks.stream().map(this::toResponse).toList());
  }

  public Uni<WebhookResponse> getWebhook(String id) {
    return webhookRepository
        .findByIdOptional(sanitizeString(id))
        .map(webhook -> webhook != null ? toResponse(webhook) : null);
  }

  public Uni<WebhookResponse> getWebhookByProductId(String productId) {
    return webhookRepository
        .findWebhookByProduct(productId)
        .map(webhook -> webhook != null ? toResponse(webhook) : null);
  }

  public Uni<WebhookResponse> updateWebhook(WebhookRequest request, String productId) {
    return webhookRepository
        .findWebhookByProduct(productId)
        .onItem()
        .ifNull()
        .failWith(() -> new IllegalArgumentException("Webhook not found: " + productId))
        .invoke(
            webhook -> {
              webhook.setUrl(request.getUrl());
              webhook.setHttpMethod(request.getHttpMethod());
              webhook.setHeaders(DataEncryptionConfig.encrypt(request.getHeaders()));
              webhook.setUpdatedAt(LocalDateTime.now());

              if (request.getRetryPolicy() != null) {
                RetryPolicy retryPolicy = new RetryPolicy();
                retryPolicy.setMaxAttempts(request.getRetryPolicy().getMaxAttempts());
                retryPolicy.setInitialDelayMs(request.getRetryPolicy().getInitialDelayMs());
                retryPolicy.setMaxDelayMs(request.getRetryPolicy().getMaxDelayMs());
                retryPolicy.setBackoffMultiplier(request.getRetryPolicy().getBackoffMultiplier());
                webhook.setRetryPolicy(retryPolicy);
              }
            })
        .call(webhook -> webhookRepository.update(webhook))
        .invoke(() -> log.info("Updated webhook with ID: {}", productId))
        .map(this::toResponse);
  }

  public Uni<Boolean> deleteWebhook(String id) {
    return webhookRepository
        .findByIdOptional(id)
        .onItem()
        .ifNull()
        .failWith(() -> new IllegalArgumentException("Webhook not found: " + sanitizeString(id)))
        .call(webhook -> webhookRepository.deleteByIdSafe(sanitizeString(id)))
        .invoke(() -> logDeleteWebhook(sanitizeString(id)))
        .replaceWith(true);
  }

  private void logDeleteWebhook(String id) {
    log.info(DELETED_WEBHOOK_WITH_ID, sanitizeString(id));
  }

  public Uni<Boolean> deleteWebhookByProductId(String productId) {
    return webhookRepository
        .findWebhookByProduct(productId)
        .onItem()
        .ifNull()
        .failWith(() -> new IllegalArgumentException("Webhook not found: " + productId))
        .call(webhook -> webhookRepository.deleteByIdSafe(webhook.getId().toString()))
        .invoke(webhook -> logDeleteWebhook(webhook.getId().toString()))
        .replaceWith(true);
  }

  public Uni<Void> sendNotification(NotificationRequest request) {
    return webhookRepository
        .findActiveWebhooksByProduct(request.getProductId())
        .invoke(
            webhooks -> {
              if (webhooks.isEmpty()) {
                log.warn(
                    "No active webhooks found for product: {}",
                    sanitizeString(request.getProductId()));
              } else {
                log.info(
                    "Found {} active webhook(s) for product: {}",
                    webhooks.size(),
                    sanitizeString(request.getProductId()));
              }
            })
        .onItem()
        .transformToMulti(webhooks -> io.smallrye.mutiny.Multi.createFrom().iterable(webhooks))
        .onItem()
        .call(
            webhook -> {
              WebhookNotification notification = new WebhookNotification();
              notification.setWebhookId(webhook.getId());
              notification.setPayload(request.getPayload());
              notification.setStatus(WebhookNotification.NotificationStatus.SENDING);
              notification.setAttemptCount(0);
              notification.setCreatedAt(LocalDateTime.now());

              return notificationRepository
                  .persist(notification)
                  .invoke(
                      () ->
                          log.info(
                              "Created notification with ID: {} for webhook: {} (product: {})",
                              notification.getId(),
                              webhook.getId(),
                              sanitizeString(request.getProductId())))
                  .call(n -> notificationService.processNotification(n, webhook));
            })
        .collect()
        .asList()
        .replaceWithVoid();
  }

  private WebhookResponse toResponse(Webhook webhook) {
    WebhookResponse response = new WebhookResponse();
    //        response.setId(webhook.getId().toString());
    response.setProductId(webhook.getProductId());
    response.setDescription(webhook.getDescription());
    response.setUrl(webhook.getUrl());
    response.setHttpMethod(webhook.getHttpMethod());
    webhook.setHeaders(DataEncryptionConfig.decrypt(webhook.getHeaders()));
    response.setProducts(webhook.getProducts());
    response.setStatus(webhook.getStatus().toString());
    response.setCreatedAt(webhook.getCreatedAt());
    response.setUpdatedAt(webhook.getUpdatedAt());

    if (webhook.getRetryPolicy() != null) {
      WebhookResponse.RetryPolicyResponse retryResponse = new WebhookResponse.RetryPolicyResponse();
      retryResponse.setMaxAttempts(webhook.getRetryPolicy().getMaxAttempts());
      retryResponse.setInitialDelayMs(webhook.getRetryPolicy().getInitialDelayMs());
      retryResponse.setMaxDelayMs(webhook.getRetryPolicy().getMaxDelayMs());
      retryResponse.setBackoffMultiplier(webhook.getRetryPolicy().getBackoffMultiplier());
      response.setRetryPolicy(retryResponse);
    }

    return response;
  }

  /**
   * Sanitize user input for safe logging: remove line breaks, control characters, and allow only
   * alphanumerics plus minimal punctuation (underscore, dash).
   */
  private String sanitizeString(String input) {
    if (input == null) {
      return null;
    }
    // Remove all non-alphanumerics, dash, and underscore
    return input.replaceAll("[^A-Za-z0-9_-]", "");
  }
}
