package it.pagopa.selfcare.webhook.service;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.webhook.dto.NotificationRequest;
import it.pagopa.selfcare.webhook.dto.NotificationResendResponse;
import it.pagopa.selfcare.webhook.dto.WebhookRequest;
import it.pagopa.selfcare.webhook.dto.WebhookResponse;
import it.pagopa.selfcare.webhook.entity.RetryPolicy;
import it.pagopa.selfcare.webhook.entity.Webhook;
import it.pagopa.selfcare.webhook.entity.WebhookNotification;
import it.pagopa.selfcare.webhook.exception.WebhookAlreadyExistsException;
import it.pagopa.selfcare.webhook.repository.WebhookNotificationRepository;
import it.pagopa.selfcare.webhook.repository.WebhookRepository;
import it.pagopa.selfcare.webhook.util.DataEncryptionConfig;
import it.pagopa.selfcare.webhook.util.Sanitizer;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.time.LocalDateTime;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;

@Slf4j
@ApplicationScoped
public class WebhookService {

  public static final String DELETED_WEBHOOK_WITH_ID = "Deleted webhook with ID: {}";
  @Inject WebhookRepository webhookRepository;

  @Inject WebhookNotificationRepository notificationRepository;

  @Inject WebhookNotificationPublisher notificationPublisher;

  public Uni<WebhookResponse> createWebhook(WebhookRequest request) {
    Webhook webhook = new Webhook();
    webhook.setUrl(request.getUrl());
    webhook.setHttpMethod(Sanitizer.sanitizeString(request.getHttpMethod()));
    webhook.setHeaders(DataEncryptionConfig.encrypt(request.getHeaders()));
    webhook.setTenantId(Sanitizer.sanitizeString(request.getTenantId()));
    webhook.setProductId(Sanitizer.sanitizeString(request.getProductId()));
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
        .findWebhookByProduct(webhook.getProductId(), webhook.getTenantId())
        .onItem()
        .transformToUni(
            existingWebhook -> {
              if (existingWebhook != null) {
                return Uni.createFrom()
                    .failure(
                        new WebhookAlreadyExistsException(
                            "Webhook already exists for product: "
                                + webhook.getProductId()
                                + " and tenant: "
                                + webhook.getTenantId()));
              }
              return webhookRepository.persist(webhook);
            })
        .invoke(() -> log.info("Created webhook with ID: {}", webhook.getId()))
        .map(this::toResponse);
  }

  public Uni<List<WebhookResponse>> listWebhooks(String tenantId, int page, int size) {
    return webhookRepository
        .findWebhooks(Sanitizer.sanitizeString(tenantId), page, size)
        .map(webhooks -> webhooks.stream().map(this::toResponse).toList());
  }

  public Uni<WebhookResponse> getWebhook(String id) {
    return webhookRepository
        .findByIdOptional(Sanitizer.sanitizeString(id))
        .map(webhook -> webhook != null ? toResponse(webhook) : null);
  }

  public Uni<WebhookResponse> getWebhookByProductId(String productId, String tenantId) {
    return webhookRepository
        .findWebhookByProduct(productId, tenantId)
        .map(webhook -> webhook != null ? toResponse(webhook) : null);
  }

  public Uni<WebhookResponse> updateWebhook(WebhookRequest request, String productId) {
    return webhookRepository
        .findWebhookByProduct(productId, Sanitizer.sanitizeString(request.getTenantId()))
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
        .failWith(
            () ->
                new IllegalArgumentException("Webhook not found: " + Sanitizer.sanitizeString(id)))
        .call(webhook -> webhookRepository.deleteByIdSafe(Sanitizer.sanitizeString(id)))
        .invoke(() -> logDeleteWebhook(Sanitizer.sanitizeString(id)))
        .replaceWith(true);
  }

  private void logDeleteWebhook(String id) {
    log.info(DELETED_WEBHOOK_WITH_ID, Sanitizer.sanitizeString(id));
  }

  public Uni<Boolean> deleteWebhookByProductId(String productId, String tenantId) {
    return webhookRepository
        .findWebhookByProduct(productId, tenantId)
        .onItem()
        .ifNull()
        .failWith(() -> new IllegalArgumentException("Webhook not found: " + productId))
        .call(webhook -> webhookRepository.deleteByIdSafe(webhook.getId().toString()))
        .invoke(webhook -> logDeleteWebhook(webhook.getId().toString()))
        .replaceWith(true);
  }

  public Uni<Void> sendNotification(NotificationRequest request) {
    return webhookRepository
        .findActiveWebhooksByProduct(
            request.getProductId(), Sanitizer.sanitizeString(request.getTenantId()))
        .invoke(
            webhooks -> {
              if (webhooks.isEmpty()) {
                log.warn(
                    "No active webhooks found for product: {} and tenant: {}",
                    Sanitizer.sanitizeString(request.getProductId()),
                    Sanitizer.sanitizeString(request.getTenantId()));
              } else {
                log.info(
                    "Found {} active webhook(s) for product: {} and tenant: {}",
                    webhooks.size(),
                    Sanitizer.sanitizeString(request.getProductId()),
                    Sanitizer.sanitizeString(request.getTenantId()));
              }
            })
        .onItem()
        .transformToMulti(webhooks -> io.smallrye.mutiny.Multi.createFrom().iterable(webhooks))
        .onItem()
        .call(
            webhook -> {
              WebhookNotification notification = new WebhookNotification();
              notification.setWebhookId(webhook.getId());
              notification.setTenantId(webhook.getTenantId());
              notification.setPayload(DataEncryptionConfig.encrypt(request.getPayload()));
              notification.setStatus(WebhookNotification.NotificationStatus.PENDING);
              notification.setAttemptCount(0);
              notification.setCreatedAt(LocalDateTime.now());
              notification.setPublishing(true);
              notification.setPublishingUntil(LocalDateTime.now().plusMinutes(5));

              return notificationRepository
                  .persist(notification)
                  .invoke(
                      () ->
                          log.info(
                              "Created notification with ID: {} for webhook: {} (product: {}, tenant: {})",
                              notification.getId(),
                              webhook.getId(),
                              Sanitizer.sanitizeString(request.getProductId()),
                              Sanitizer.sanitizeString(request.getTenantId())))
                  .call(
                      n ->
                          notificationPublisher
                              .publish(n.getId().toHexString())
                              .call(ignored -> notificationRepository.markAsPublished(n.getId()))
                              .onFailure()
                              .call(
                                  error ->
                                      notificationRepository.releasePublishingLock(n.getId())));
            })
        .collect()
        .asList()
        .replaceWithVoid();
  }

  /** Resend a single notification identified by its ID. */
  public Uni<NotificationResendResponse> resendNotificationById(String notificationId) {
    if (!ObjectId.isValid(notificationId)) {
      return Uni.createFrom()
          .failure(new IllegalArgumentException("Invalid notification ID: " + notificationId));
    }
    return notificationRepository
        .findById(new ObjectId(notificationId))
        .onItem()
        .ifNull()
        .failWith(
            () -> new IllegalArgumentException("Notification not found: " + notificationId))
        .onItem()
        .transformToUni(this::resendAndPublish)
        .map(notification -> toResendResponse(List.of(notification)));
  }

  /**
   * Resend every notification matching the given status, optionally restricted to a single
   * webhook.
   */
  public Uni<NotificationResendResponse> resendNotificationsByStatus(
      WebhookNotification.NotificationStatus status, String webhookId) {
    ObjectId webhookObjectId = null;
    if (webhookId != null && !webhookId.isBlank()) {
      if (!ObjectId.isValid(webhookId)) {
        return Uni.createFrom()
            .failure(new IllegalArgumentException("Invalid webhook ID: " + webhookId));
      }
      webhookObjectId = new ObjectId(webhookId);
    }
    return notificationRepository
        .findByStatus(status, webhookObjectId)
        .onItem()
        .transformToUni(this::resendAll);
  }

  /** Resend every notification created within the given (inclusive) date-time range. */
  public Uni<NotificationResendResponse> resendNotificationsByDateRange(
      LocalDateTime from, LocalDateTime to) {
    return notificationRepository
        .findByCreatedAtRange(from, to)
        .onItem()
        .transformToUni(this::resendAll);
  }

  private Uni<NotificationResendResponse> resendAll(List<WebhookNotification> notifications) {
    if (notifications.isEmpty()) {
      return Uni.createFrom().item(toResendResponse(List.of()));
    }
    return Multi.createFrom()
        .iterable(notifications)
        .onItem()
        .transformToUniAndMerge(this::resendAndPublish)
        .collect()
        .asList()
        .map(this::toResendResponse);
  }

  /**
   * Reset a notification to PENDING (with a fresh attempt count) and re-publish it to the
   * Storage Queue, mirroring the initial send flow in {@link
   * #sendNotification(NotificationRequest)}.
   *
   * <p>A failure while publishing is swallowed (after releasing the publishing lock): the
   * notification has already been reset to PENDING with {@code busPublishedAt} left unset, so
   * {@link WebhookNotificationOutboxService} will pick it up and retry the publish on its next
   * scheduled run. This keeps a bulk resend from failing entirely because of one transient queue
   * error.
   */
  private Uni<WebhookNotification> resendAndPublish(WebhookNotification notification) {
    notification.setStatus(WebhookNotification.NotificationStatus.PENDING);
    notification.setAttemptCount(0);
    notification.setLastError(null);
    notification.setLastAttemptAt(null);
    notification.setCompletedAt(null);
    notification.setBusPublishedAt(null);
    notification.setProcessing(false);
    notification.setProcessingUntil(null);
    notification.setPublishing(true);
    notification.setPublishingUntil(LocalDateTime.now().plusMinutes(5));

    return notificationRepository
        .update(notification)
        .invoke(n -> log.info("Resending notification with ID: {}", n.getId()))
        .call(this::publishResentNotification);
  }

  private Uni<Void> publishResentNotification(WebhookNotification notification) {
    return notificationPublisher
        .publish(notification.getId().toHexString())
        .call(ignored -> notificationRepository.markAsPublished(notification.getId()))
        .onFailure()
        .recoverWithUni(
            error ->
                notificationRepository
                    .releasePublishingLock(notification.getId())
                    .invoke(
                        () ->
                            log.error(
                                "Unable to publish resent notification {}",
                                notification.getId(),
                                error))
                    .replaceWithVoid());
  }

  private NotificationResendResponse toResendResponse(List<WebhookNotification> notifications) {
    NotificationResendResponse response = new NotificationResendResponse();
    response.setResentCount(notifications.size());
    response.setNotificationIds(notifications.stream().map(n -> n.getId().toHexString()).toList());
    return response;
  }

  private WebhookResponse toResponse(Webhook webhook) {
    WebhookResponse response = new WebhookResponse();
    //        response.setId(webhook.getId().toString());
    response.setTenantId(webhook.getTenantId());
    response.setProductId(webhook.getProductId());
    response.setDescription(webhook.getDescription());
    response.setUrl(webhook.getUrl());
    response.setHttpMethod(webhook.getHttpMethod());
    response.setHeaders(DataEncryptionConfig.decrypt(webhook.getHeaders()));
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
}
