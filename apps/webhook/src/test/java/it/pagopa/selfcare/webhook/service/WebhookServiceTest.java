package it.pagopa.selfcare.webhook.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.test.UniAssertSubscriber;
import it.pagopa.selfcare.webhook.dto.NotificationRequest;
import it.pagopa.selfcare.webhook.dto.WebhookRequest;
import it.pagopa.selfcare.webhook.dto.WebhookResponse;
import it.pagopa.selfcare.webhook.entity.Webhook;
import it.pagopa.selfcare.webhook.entity.WebhookNotification;
import it.pagopa.selfcare.webhook.repository.WebhookNotificationRepository;
import it.pagopa.selfcare.webhook.repository.WebhookRepository;
import jakarta.inject.Inject;
import java.util.Collections;
import java.util.List;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.Test;

@QuarkusTest
class WebhookServiceTest {

  private static final String PROD_TEST = "prod-test";
  @Inject WebhookService webhookService;

  @InjectMock WebhookRepository webhookRepository;

  @InjectMock WebhookNotificationRepository notificationRepository;

  @InjectMock WebhookNotificationService notificationService;

  @Test
  void createWebhook_shouldCreateAndReturnWebhook() {
    WebhookRequest request = new WebhookRequest();
    request.setUrl("http://example.com");
    request.setHttpMethod("POST");
    request.setProductId(PROD_TEST);

    WebhookRequest.RetryPolicyRequest retryPolicyRequest = new WebhookRequest.RetryPolicyRequest();
    retryPolicyRequest.setMaxAttempts(3);
    request.setRetryPolicy(retryPolicyRequest);

    when(webhookRepository.persist(any(Webhook.class)))
        .thenAnswer(
            invocation -> {
              Webhook webhook = invocation.getArgument(0);
              webhook.setId(new ObjectId());
              return Uni.createFrom().item(webhook);
            });

    UniAssertSubscriber<WebhookResponse> subscriber =
        webhookService
            .createWebhook(request)
            .subscribe()
            .withSubscriber(UniAssertSubscriber.create());

    WebhookResponse response = subscriber.awaitItem().getItem();
    assertEquals(PROD_TEST, response.getProductId());
    assertEquals("ACTIVE", response.getStatus());
    assertEquals(1, response.getProducts().size());
    assertEquals(PROD_TEST, response.getProducts().get(0));
    assertNotNull(response.getRetryPolicy());
    assertEquals(3, response.getRetryPolicy().getMaxAttempts());

    verify(webhookRepository).persist(any(Webhook.class));
  }

  @Test
  void listWebhooks_shouldReturnListOfWebhooks() {
    Webhook webhook = new Webhook();
    webhook.setId(new ObjectId());
    webhook.setProductId(PROD_TEST);
    webhook.setStatus(Webhook.WebhookStatus.ACTIVE);

    when(webhookRepository.listAll()).thenReturn(Uni.createFrom().item(List.of(webhook)));

    UniAssertSubscriber<List<WebhookResponse>> subscriber =
        webhookService.listWebhooks().subscribe().withSubscriber(UniAssertSubscriber.create());

    List<WebhookResponse> responses = subscriber.awaitItem().getItem();
    assertEquals(1, responses.size());
    assertEquals(PROD_TEST, responses.get(0).getProductId());
  }

  @Test
  void getWebhook_shouldReturnWebhook_whenFound() {
    ObjectId id = new ObjectId();
    Webhook webhook = new Webhook();
    webhook.setId(id);
    webhook.setProductId(PROD_TEST);
    webhook.setStatus(Webhook.WebhookStatus.ACTIVE);

    when(webhookRepository.findByIdOptional(anyString()))
        .thenReturn(Uni.createFrom().item(webhook));

    UniAssertSubscriber<WebhookResponse> subscriber =
        webhookService
            .getWebhook(id.toHexString())
            .subscribe()
            .withSubscriber(UniAssertSubscriber.create());

    WebhookResponse response = subscriber.awaitItem().getItem();
    assertNotNull(response);
    assertEquals(PROD_TEST, response.getProductId());
  }

  @Test
  void getWebhook_shouldReturnNull_whenNotFound() {
    when(webhookRepository.findByIdOptional(anyString())).thenReturn(Uni.createFrom().nullItem());

    UniAssertSubscriber<WebhookResponse> subscriber =
        webhookService
            .getWebhook(new ObjectId().toHexString())
            .subscribe()
            .withSubscriber(UniAssertSubscriber.create());

    assertNull(subscriber.awaitItem().getItem());
  }

  @Test
  void updateWebhook_shouldUpdateAndReturnWebhook() {
    ObjectId id = new ObjectId();
    Webhook existingWebhook = new Webhook();
    existingWebhook.setId(id);
    existingWebhook.setProductId(PROD_TEST);
    existingWebhook.setStatus(Webhook.WebhookStatus.ACTIVE);
    existingWebhook.setUrl("http://old-url.com");

    WebhookRequest request = new WebhookRequest();
    request.setUrl("http://new-url.com");
    request.setHttpMethod("PUT");

    when(webhookRepository.findWebhookByProduct(anyString()))
        .thenReturn(Uni.createFrom().item(existingWebhook));
    when(webhookRepository.update(any(Webhook.class)))
        .thenReturn(Uni.createFrom().item(existingWebhook));

    UniAssertSubscriber<WebhookResponse> subscriber =
        webhookService
            .updateWebhook(request, PROD_TEST)
            .subscribe()
            .withSubscriber(UniAssertSubscriber.create());

    WebhookResponse response = subscriber.awaitItem().getItem();
    assertEquals("http://new-url.com", response.getUrl());

    verify(webhookRepository).update(any(Webhook.class));
  }

  @Test
  void updateWebhook_shouldFail_whenNotFound() {
    WebhookRequest request = new WebhookRequest();
    request.setUrl("http://404-url.com");

    when(webhookRepository.findByIdOptional(anyString())).thenReturn(Uni.createFrom().nullItem());

    UniAssertSubscriber<WebhookResponse> subscriber =
        webhookService
            .updateWebhook(request, PROD_TEST)
            .subscribe()
            .withSubscriber(UniAssertSubscriber.create());

    subscriber.awaitFailure();
  }

  @Test
  void deleteWebhook_shouldDeleteAndReturnTrue() {
    ObjectId id = new ObjectId();
    Webhook webhook = new Webhook();
    webhook.setId(id);

    when(webhookRepository.findByIdOptional(anyString()))
        .thenReturn(Uni.createFrom().item(webhook));
    when(webhookRepository.deleteByIdSafe(anyString())).thenReturn(Uni.createFrom().item(true));

    UniAssertSubscriber<Boolean> subscriber =
        webhookService
            .deleteWebhook(id.toHexString())
            .subscribe()
            .withSubscriber(UniAssertSubscriber.create());

    assertTrue(subscriber.awaitItem().getItem());
    verify(webhookRepository).deleteByIdSafe(id.toHexString());
  }

  @Test
  void deleteWebhook_shouldFail_whenNotFound() {
    when(webhookRepository.findByIdOptional(anyString())).thenReturn(Uni.createFrom().nullItem());

    UniAssertSubscriber<Boolean> subscriber =
        webhookService
            .deleteWebhook(new ObjectId().toHexString())
            .subscribe()
            .withSubscriber(UniAssertSubscriber.create());

    subscriber.awaitFailure();
  }

  @Test
  void sendNotification_shouldCreateNotificationsForActiveWebhooks() {
    String productId = "prod-io";
    NotificationRequest request = new NotificationRequest();
    request.setProductId(productId);
    request.setPayload("{}");

    Webhook webhook = new Webhook();
    webhook.setId(new ObjectId());
    webhook.setProducts(List.of(productId));
    webhook.setStatus(Webhook.WebhookStatus.ACTIVE);

    when(webhookRepository.findActiveWebhooksByProduct(productId))
        .thenReturn(Uni.createFrom().item(List.of(webhook)));
    when(notificationRepository.persist(any(WebhookNotification.class)))
        .thenAnswer(
            invocation -> {
              WebhookNotification notification = invocation.getArgument(0);
              notification.setId(new ObjectId());
              return Uni.createFrom().item(notification);
            });
    when(notificationService.processNotification(any(), any()))
        .thenReturn(Uni.createFrom().voidItem());

    UniAssertSubscriber<Void> subscriber =
        webhookService
            .sendNotification(request)
            .subscribe()
            .withSubscriber(UniAssertSubscriber.create());

    subscriber.awaitItem();

    verify(notificationRepository).persist(any(WebhookNotification.class));
    verify(notificationService).processNotification(any(WebhookNotification.class), eq(webhook));
  }

  @Test
  void sendNotification_shouldDoNothing_whenNoActiveWebhooks() {
    String productId = "prod-io";
    NotificationRequest request = new NotificationRequest();
    request.setProductId(productId);

    when(webhookRepository.findActiveWebhooksByProduct(productId))
        .thenReturn(Uni.createFrom().item(Collections.emptyList()));

    UniAssertSubscriber<Void> subscriber =
        webhookService
            .sendNotification(request)
            .subscribe()
            .withSubscriber(UniAssertSubscriber.create());

    subscriber.awaitItem();

    verify(notificationRepository, never()).persist(any(WebhookNotification.class));
  }
}
