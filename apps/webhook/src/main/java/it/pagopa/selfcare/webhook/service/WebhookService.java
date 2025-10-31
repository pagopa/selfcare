package it.pagopa.selfcare.webhook.service;

import io.quarkus.logging.Log;
import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.webhook.dto.NotificationRequest;
import it.pagopa.selfcare.webhook.dto.WebhookRequest;
import it.pagopa.selfcare.webhook.dto.WebhookResponse;
import it.pagopa.selfcare.webhook.entity.RetryPolicy;
import it.pagopa.selfcare.webhook.entity.Webhook;
import it.pagopa.selfcare.webhook.entity.WebhookNotification;
import it.pagopa.selfcare.webhook.repository.WebhookNotificationRepository;
import it.pagopa.selfcare.webhook.repository.WebhookRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.time.LocalDateTime;
import java.util.List;

@ApplicationScoped
public class WebhookService {
    
    @Inject
    WebhookRepository webhookRepository;
    
    @Inject
    WebhookNotificationRepository notificationRepository;
    
    @Inject
    WebhookNotificationService notificationService;
    
    public Uni<WebhookResponse> createWebhook(WebhookRequest request) {
        Webhook webhook = new Webhook();
        webhook.setName(request.getName());
        webhook.setDescription(request.getDescription());
        webhook.setUrl(request.getUrl());
        webhook.setHttpMethod(request.getHttpMethod());
        webhook.setHeaders(request.getHeaders());
        webhook.setProducts(request.getProducts());
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
        
        return webhookRepository.persist(webhook)
                .invoke(() -> Log.infof("Created webhook with ID: %s", webhook.getId()))
                .map(this::toResponse);
    }
    
    public Uni<List<WebhookResponse>> listWebhooks() {
        return webhookRepository.listAll()
                .map(webhooks -> webhooks.stream()
                        .map(this::toResponse)
                        .toList());
    }
    
    public Uni<WebhookResponse> getWebhook(String id) {
        return webhookRepository.findByIdOptional(id)
                .map(webhook -> webhook != null ? toResponse(webhook) : null);
    }
    
    public Uni<WebhookResponse> updateWebhook(String id, WebhookRequest request) {
        return webhookRepository.findByIdOptional(id)
                .onItem().ifNull().failWith(() -> new IllegalArgumentException("Webhook not found: " + id))
                .invoke(webhook -> {
                    webhook.setName(request.getName());
                    webhook.setDescription(request.getDescription());
                    webhook.setUrl(request.getUrl());
                    webhook.setHttpMethod(request.getHttpMethod());
                    webhook.setHeaders(request.getHeaders());
                    webhook.setProducts(request.getProducts());
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
                .invoke(() -> Log.infof("Updated webhook with ID: %s", id))
                .map(this::toResponse);
    }
    
    public Uni<Boolean> deleteWebhook(String id) {
        return webhookRepository.findByIdOptional(id)
                .onItem().ifNull().failWith(() -> new IllegalArgumentException("Webhook not found: " + id))
                .call(webhook -> webhookRepository.deleteByIdSafe(id))
                .invoke(() -> Log.infof("Deleted webhook with ID: %s", id))
                .replaceWith(true);
    }
    
    public Uni<Void> sendNotification(NotificationRequest request) {
        return webhookRepository.findActiveWebhooksByProduct(request.getProductId())
                .invoke(webhooks -> {
                    if (webhooks.isEmpty()) {
                        Log.warnf("No active webhooks found for product: %s", request.getProductId());
                    } else {
                        Log.infof("Found %d active webhook(s) for product: %s", webhooks.size(), request.getProductId());
                    }
                })
                .onItem().transformToMulti(webhooks -> io.smallrye.mutiny.Multi.createFrom().iterable(webhooks))
                .onItem().call(webhook -> {
                    WebhookNotification notification = new WebhookNotification();
                    notification.setWebhookId(webhook.getId());
                    notification.setPayload(request.getPayload());
                    notification.setStatus(WebhookNotification.NotificationStatus.PENDING);
                    notification.setAttemptCount(0);
                    notification.setCreatedAt(LocalDateTime.now());
                    
                    return notificationRepository.persist(notification)
                            .invoke(() -> Log.infof("Created notification with ID: %s for webhook: %s (product: %s)", 
                                    notification.getId(), webhook.getId(), request.getProductId()))
                            .call(n -> notificationService.processNotification(n, webhook));
                })
                .collect().asList()
                .replaceWithVoid();
    }
    
    private WebhookResponse toResponse(Webhook webhook) {
        WebhookResponse response = new WebhookResponse();
        response.setId(webhook.getId().toString());
        response.setName(webhook.getName());
        response.setDescription(webhook.getDescription());
        response.setUrl(webhook.getUrl());
        response.setHttpMethod(webhook.getHttpMethod());
        response.setHeaders(webhook.getHeaders());
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
