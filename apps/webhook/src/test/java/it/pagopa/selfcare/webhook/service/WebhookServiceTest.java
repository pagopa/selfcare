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
import it.pagopa.selfcare.webhook.exception.WebhookAlreadyExistsException;
import it.pagopa.selfcare.webhook.repository.WebhookNotificationRepository;
import it.pagopa.selfcare.webhook.repository.WebhookRepository;
import it.pagopa.selfcare.webhook.util.DataEncryptionConfig;
import jakarta.inject.Inject;
import java.util.Collections;
import java.util.List;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

@QuarkusTest
class WebhookServiceTest {

  private static final String PROD_TEST = "prod-test";
  private static final String TENANT_ID = "SELC";
  @Inject WebhookService webhookService;

  @InjectMock WebhookRepository webhookRepository;

  @InjectMock WebhookNotificationRepository notificationRepository;

  @InjectMock WebhookNotificationPublisher notificationPublisher;

  @Test
  void createWebhook_shouldCreateAndReturnWebhook() {
    WebhookRequest request = new WebhookRequest();
    request.setUrl("http://example.com");
    request.setHttpMethod("POST");
    request.setTenantId(TENANT_ID);
    request.setProductId(PROD_TEST);

    WebhookRequest.RetryPolicyRequest retryPolicyRequest = new WebhookRequest.RetryPolicyRequest();
    retryPolicyRequest.setMaxAttempts(3);
    request.setRetryPolicy(retryPolicyRequest);

    when(webhookRepository.findWebhookByProduct(PROD_TEST, TENANT_ID))
        .thenReturn(Uni.createFrom().nullItem());
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
    assertEquals(TENANT_ID, response.getTenantId());
    assertEquals(PROD_TEST, response.getProductId());
    assertEquals("ACTIVE", response.getStatus());
    assertEquals(1, response.getProducts().size());
    assertEquals(PROD_TEST, response.getProducts().get(0));
    assertNotNull(response.getRetryPolicy());
    assertEquals(3, response.getRetryPolicy().getMaxAttempts());

    verify(webhookRepository).findWebhookByProduct(PROD_TEST, TENANT_ID);
    verify(webhookRepository).persist(any(Webhook.class));
  }

  @Test
  void createWebhook_shouldFailWhenWebhookWithSameProductAndTenantAlreadyExists() {
    // given
    WebhookRequest request = new WebhookRequest();
    request.setUrl("http://example.com");
    request.setHttpMethod("POST");
    request.setTenantId(TENANT_ID);
    request.setProductId(PROD_TEST);
    when(webhookRepository.findWebhookByProduct(PROD_TEST, TENANT_ID))
        .thenReturn(Uni.createFrom().item(new Webhook()));

    // when
    UniAssertSubscriber<WebhookResponse> subscriber =
        webhookService
            .createWebhook(request)
            .subscribe()
            .withSubscriber(UniAssertSubscriber.create());

    // then
    subscriber
        .awaitFailure()
        .assertFailedWith(WebhookAlreadyExistsException.class, "Webhook already exists");
    verify(webhookRepository, never()).persist(any(Webhook.class));
  }

  @Test
  void listWebhooks_shouldReturnListOfWebhooks() {
    // given
    Webhook webhook = new Webhook();
    webhook.setId(new ObjectId());
    webhook.setTenantId(TENANT_ID);
    webhook.setProductId(PROD_TEST);
    webhook.setStatus(Webhook.WebhookStatus.ACTIVE);

    when(webhookRepository.findWebhooks(TENANT_ID, 0, 20))
        .thenReturn(Uni.createFrom().item(List.of(webhook)));

    // when
    UniAssertSubscriber<List<WebhookResponse>> subscriber =
        webhookService
            .listWebhooks(TENANT_ID, 0, 20)
            .subscribe()
            .withSubscriber(UniAssertSubscriber.create());

    // then
    List<WebhookResponse> responses = subscriber.awaitItem().getItem();
    assertEquals(1, responses.size());
    assertEquals(TENANT_ID, responses.get(0).getTenantId());
    assertEquals(PROD_TEST, responses.get(0).getProductId());
    verify(webhookRepository).findWebhooks(TENANT_ID, 0, 20);
  }

  @Test
  void listWebhooks_shouldReturnAllTenants_whenTenantIdIsNotProvided() {
    // given
    Webhook webhook = new Webhook();
    webhook.setId(new ObjectId());
    webhook.setTenantId(TENANT_ID);
    webhook.setProductId(PROD_TEST);
    webhook.setStatus(Webhook.WebhookStatus.ACTIVE);

    when(webhookRepository.findWebhooks(null, 0, 20))
        .thenReturn(Uni.createFrom().item(List.of(webhook)));

    // when
    UniAssertSubscriber<List<WebhookResponse>> subscriber =
        webhookService
            .listWebhooks(null, 0, 20)
            .subscribe()
            .withSubscriber(UniAssertSubscriber.create());

    // then
    assertEquals(1, subscriber.awaitItem().getItem().size());
    verify(webhookRepository).findWebhooks(null, 0, 20);
  }

  @Test
  void getWebhook_shouldReturnWebhook_whenFound() {
    ObjectId id = new ObjectId();
    Webhook webhook = new Webhook();
    webhook.setId(id);
    webhook.setTenantId(TENANT_ID);
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
    assertEquals(TENANT_ID, response.getTenantId());
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
    existingWebhook.setTenantId(TENANT_ID);
    existingWebhook.setProductId(PROD_TEST);
    existingWebhook.setStatus(Webhook.WebhookStatus.ACTIVE);
    existingWebhook.setUrl("http://old-url.com");

    WebhookRequest request = new WebhookRequest();
    request.setUrl("http://new-url.com");
    request.setHttpMethod("PUT");
    request.setTenantId(TENANT_ID);

    when(webhookRepository.findWebhookByProduct(anyString(), anyString()))
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
    request.setTenantId(TENANT_ID);

    when(webhookRepository.findWebhookByProduct(anyString(), anyString()))
        .thenReturn(Uni.createFrom().nullItem());

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
  void deleteWebhookByProductId_shouldDeleteAndReturnTrue() {
    // given
    ObjectId id = new ObjectId();
    Webhook webhook = new Webhook();
    webhook.setId(id);
    webhook.setTenantId(TENANT_ID);
    webhook.setProductId(PROD_TEST);

    when(webhookRepository.findWebhookByProduct(PROD_TEST, TENANT_ID))
        .thenReturn(Uni.createFrom().item(webhook));
    when(webhookRepository.deleteByIdSafe(id.toHexString()))
        .thenReturn(Uni.createFrom().item(true));

    // when
    UniAssertSubscriber<Boolean> subscriber =
        webhookService
            .deleteWebhookByProductId(PROD_TEST, TENANT_ID)
            .subscribe()
            .withSubscriber(UniAssertSubscriber.create());

    // then
    assertTrue(subscriber.awaitItem().getItem());
    verify(webhookRepository).findWebhookByProduct(PROD_TEST, TENANT_ID);
    verify(webhookRepository).deleteByIdSafe(id.toHexString());
  }

  @Test
  void deleteWebhookByProductId_shouldFail_whenWebhookIsNotFound() {
    // given
    when(webhookRepository.findWebhookByProduct(PROD_TEST, TENANT_ID))
        .thenReturn(Uni.createFrom().nullItem());

    // when
    UniAssertSubscriber<Boolean> subscriber =
        webhookService
            .deleteWebhookByProductId(PROD_TEST, TENANT_ID)
            .subscribe()
            .withSubscriber(UniAssertSubscriber.create());

    // then
    subscriber.awaitFailure();
    verify(webhookRepository).findWebhookByProduct(PROD_TEST, TENANT_ID);
    verify(webhookRepository, never()).deleteByIdSafe(anyString());
  }

  @Test
  void sendNotification_shouldCreateNotificationsForActiveWebhooks() {
    // given
    String productId = "prod-io";
    NotificationRequest request = new NotificationRequest();
    request.setProductId(productId);
    request.setTenantId(TENANT_ID);
    request.setPayload("{}");
    request.setTopic("SC-Contracts");

    Webhook webhook = new Webhook();
    webhook.setId(new ObjectId());
    webhook.setTenantId(TENANT_ID);
    webhook.setProducts(List.of(productId));
    webhook.setStatus(Webhook.WebhookStatus.ACTIVE);

    when(webhookRepository.findActiveWebhooksByProduct(productId, TENANT_ID))
        .thenReturn(Uni.createFrom().item(List.of(webhook)));
    when(notificationRepository.persist(any(WebhookNotification.class)))
        .thenAnswer(
            invocation -> {
              WebhookNotification notification = invocation.getArgument(0);
              notification.setId(new ObjectId());
              return Uni.createFrom().item(notification);
            });
    when(notificationPublisher.publish(anyString())).thenReturn(Uni.createFrom().voidItem());
    when(notificationRepository.markAsPublished(any())).thenReturn(Uni.createFrom().voidItem());

    // when
    UniAssertSubscriber<Void> subscriber =
        webhookService
            .sendNotification(request)
            .subscribe()
            .withSubscriber(UniAssertSubscriber.create());

    subscriber.awaitItem();

    // then
    ArgumentCaptor<WebhookNotification> captor = ArgumentCaptor.forClass(WebhookNotification.class);
    verify(notificationRepository).persist(captor.capture());
    assertNotEquals(request.getPayload(), captor.getValue().getPayload());
    assertEquals(
        request.getPayload(), DataEncryptionConfig.decrypt(captor.getValue().getPayload()));
    assertEquals(TENANT_ID, captor.getValue().getTenantId());
    assertEquals(WebhookNotification.NotificationStatus.PENDING, captor.getValue().getStatus());
    verify(notificationPublisher).publish(captor.getValue().getId().toHexString());
    verify(notificationRepository).markAsPublished(captor.getValue().getId());
  }

  @Test
  void sendNotification_shouldDoNothing_whenNoActiveWebhooks() {
    // given
    String productId = "prod-io";
    NotificationRequest request = new NotificationRequest();
    request.setProductId(productId);
    request.setTenantId(TENANT_ID);

    when(webhookRepository.findActiveWebhooksByProduct(productId, TENANT_ID))
        .thenReturn(Uni.createFrom().item(Collections.emptyList()));

    // when
    UniAssertSubscriber<Void> subscriber =
        webhookService
            .sendNotification(request)
            .subscribe()
            .withSubscriber(UniAssertSubscriber.create());

    subscriber.awaitItem();

    // then
    verify(notificationRepository, never()).persist(any(WebhookNotification.class));
  }

  @Test
  void sendNotification_shouldOnlyNotifyWebhooksSubscribedToTopic() {
    // given
    String productId = "prod-io";
    NotificationRequest request = new NotificationRequest();
    request.setProductId(productId);
    request.setTenantId(TENANT_ID);
    request.setPayload("{}");
    request.setTopic("SC-Users");

    Webhook subscribedWebhook = new Webhook();
    subscribedWebhook.setId(new ObjectId());
    subscribedWebhook.setTenantId(TENANT_ID);
    subscribedWebhook.setProducts(List.of(productId));
    subscribedWebhook.setTopics(List.of("SC-Contracts", "SC-Users"));
    subscribedWebhook.setStatus(Webhook.WebhookStatus.ACTIVE);

    Webhook notSubscribedWebhook = new Webhook();
    notSubscribedWebhook.setId(new ObjectId());
    notSubscribedWebhook.setTenantId(TENANT_ID);
    notSubscribedWebhook.setProducts(List.of(productId));
    notSubscribedWebhook.setTopics(List.of("SC-Delegate"));
    notSubscribedWebhook.setStatus(Webhook.WebhookStatus.ACTIVE);

    when(webhookRepository.findActiveWebhooksByProduct(productId, TENANT_ID))
        .thenReturn(Uni.createFrom().item(List.of(subscribedWebhook, notSubscribedWebhook)));
    when(notificationRepository.persist(any(WebhookNotification.class)))
        .thenAnswer(
            invocation -> {
              WebhookNotification notification = invocation.getArgument(0);
              notification.setId(new ObjectId());
              return Uni.createFrom().item(notification);
            });
    when(notificationPublisher.publish(anyString())).thenReturn(Uni.createFrom().voidItem());
    when(notificationRepository.markAsPublished(any())).thenReturn(Uni.createFrom().voidItem());

    // when
    UniAssertSubscriber<Void> subscriber =
        webhookService
            .sendNotification(request)
            .subscribe()
            .withSubscriber(UniAssertSubscriber.create());

    subscriber.awaitItem();

    // then
    ArgumentCaptor<WebhookNotification> captor = ArgumentCaptor.forClass(WebhookNotification.class);
    verify(notificationRepository, times(1)).persist(captor.capture());
    assertEquals(subscribedWebhook.getId(), captor.getValue().getWebhookId());
    assertEquals("SC-Users", captor.getValue().getTopic());
  }

  @Test
  void sendNotification_shouldNotifyWebhooksWithoutTopicFilter_forAnyTopic() {
    // given
    String productId = "prod-io";
    NotificationRequest request = new NotificationRequest();
    request.setProductId(productId);
    request.setTenantId(TENANT_ID);
    request.setPayload("{}");
    request.setTopic("SC-Delegate");

    Webhook webhook = new Webhook();
    webhook.setId(new ObjectId());
    webhook.setTenantId(TENANT_ID);
    webhook.setProducts(List.of(productId));
    webhook.setTopics(null);
    webhook.setStatus(Webhook.WebhookStatus.ACTIVE);

    when(webhookRepository.findActiveWebhooksByProduct(productId, TENANT_ID))
        .thenReturn(Uni.createFrom().item(List.of(webhook)));
    when(notificationRepository.persist(any(WebhookNotification.class)))
        .thenAnswer(
            invocation -> {
              WebhookNotification notification = invocation.getArgument(0);
              notification.setId(new ObjectId());
              return Uni.createFrom().item(notification);
            });
    when(notificationPublisher.publish(anyString())).thenReturn(Uni.createFrom().voidItem());
    when(notificationRepository.markAsPublished(any())).thenReturn(Uni.createFrom().voidItem());

    // when
    UniAssertSubscriber<Void> subscriber =
        webhookService
            .sendNotification(request)
            .subscribe()
            .withSubscriber(UniAssertSubscriber.create());

    subscriber.awaitItem();

    // then
    verify(notificationRepository, times(1)).persist(any(WebhookNotification.class));
  }

  @Test
  void resendNotificationById_shouldResetAndPublishNotification() {
    // given
    ObjectId notificationId = new ObjectId();
    WebhookNotification notification = new WebhookNotification();
    notification.setId(notificationId);
    notification.setWebhookId(new ObjectId());
    notification.setStatus(WebhookNotification.NotificationStatus.FAILED);
    notification.setAttemptCount(3);
    notification.setLastError("boom");
    notification.setCompletedAt(java.time.LocalDateTime.now());

    when(notificationRepository.findById(eq(notificationId)))
        .thenReturn(Uni.createFrom().item(notification));
    when(notificationRepository.update(any(WebhookNotification.class)))
        .thenAnswer(
            invocation -> {
              WebhookNotification updated = invocation.getArgument(0);
              return Uni.createFrom().item(updated);
            });
    when(notificationPublisher.publish(anyString())).thenReturn(Uni.createFrom().voidItem());
    when(notificationRepository.markAsPublished(any())).thenReturn(Uni.createFrom().voidItem());

    // when
    UniAssertSubscriber<it.pagopa.selfcare.webhook.dto.NotificationResendResponse> subscriber =
        webhookService
            .resendNotificationById(notificationId.toHexString())
            .subscribe()
            .withSubscriber(UniAssertSubscriber.create());

    // then
    it.pagopa.selfcare.webhook.dto.NotificationResendResponse response =
        subscriber.awaitItem().getItem();
    assertEquals(1, response.getResentCount());
    assertEquals(List.of(notificationId.toHexString()), response.getNotificationIds());
    assertEquals(WebhookNotification.NotificationStatus.PENDING, notification.getStatus());
    assertEquals(0, notification.getAttemptCount());
    assertNull(notification.getLastError());
    assertNull(notification.getCompletedAt());
    verify(notificationPublisher).publish(notificationId.toHexString());
    verify(notificationRepository).markAsPublished(notificationId);
  }

  @Test
  void resendNotificationById_shouldFail_whenNotificationIsNotFound() {
    // given
    ObjectId notificationId = new ObjectId();
    when(notificationRepository.findById(eq(notificationId)))
        .thenReturn(Uni.createFrom().nullItem());

    // when
    UniAssertSubscriber<it.pagopa.selfcare.webhook.dto.NotificationResendResponse> subscriber =
        webhookService
            .resendNotificationById(notificationId.toHexString())
            .subscribe()
            .withSubscriber(UniAssertSubscriber.create());

    // then
    subscriber.awaitFailure().assertFailedWith(IllegalArgumentException.class);
  }

  @Test
  void resendNotificationById_shouldFail_whenIdIsInvalid() {
    // when
    UniAssertSubscriber<it.pagopa.selfcare.webhook.dto.NotificationResendResponse> subscriber =
        webhookService
            .resendNotificationById("not-an-object-id")
            .subscribe()
            .withSubscriber(UniAssertSubscriber.create());

    // then
    subscriber.awaitFailure().assertFailedWith(IllegalArgumentException.class);
    verify(notificationRepository, never()).findById(any(ObjectId.class));
  }

  @Test
  void resendNotificationsByStatus_shouldResendAllMatchingNotifications() {
    // given
    ObjectId webhookId = new ObjectId();
    WebhookNotification first = new WebhookNotification();
    first.setId(new ObjectId());
    first.setStatus(WebhookNotification.NotificationStatus.FAILED);
    first.setAttemptCount(2);
    WebhookNotification second = new WebhookNotification();
    second.setId(new ObjectId());
    second.setStatus(WebhookNotification.NotificationStatus.FAILED);
    second.setAttemptCount(3);

    when(notificationRepository.findByStatus(
            WebhookNotification.NotificationStatus.FAILED, webhookId))
        .thenReturn(Uni.createFrom().item(List.of(first, second)));
    when(notificationRepository.update(any(WebhookNotification.class)))
        .thenAnswer(
            invocation -> {
              WebhookNotification updated = invocation.getArgument(0);
              return Uni.createFrom().item(updated);
            });
    when(notificationPublisher.publish(anyString())).thenReturn(Uni.createFrom().voidItem());
    when(notificationRepository.markAsPublished(any())).thenReturn(Uni.createFrom().voidItem());

    // when
    UniAssertSubscriber<it.pagopa.selfcare.webhook.dto.NotificationResendResponse> subscriber =
        webhookService
            .resendNotificationsByStatus(
                WebhookNotification.NotificationStatus.FAILED, webhookId.toHexString())
            .subscribe()
            .withSubscriber(UniAssertSubscriber.create());

    // then
    it.pagopa.selfcare.webhook.dto.NotificationResendResponse response =
        subscriber.awaitItem().getItem();
    assertEquals(2, response.getResentCount());
    assertEquals(WebhookNotification.NotificationStatus.PENDING, first.getStatus());
    assertEquals(WebhookNotification.NotificationStatus.PENDING, second.getStatus());
    verify(notificationPublisher, times(2)).publish(anyString());
  }

  @Test
  void resendNotificationsByStatus_shouldReturnEmptyResponse_whenNoneMatch() {
    // given
    when(notificationRepository.findByStatus(WebhookNotification.NotificationStatus.FAILED, null))
        .thenReturn(Uni.createFrom().item(Collections.emptyList()));

    // when
    UniAssertSubscriber<it.pagopa.selfcare.webhook.dto.NotificationResendResponse> subscriber =
        webhookService
            .resendNotificationsByStatus(WebhookNotification.NotificationStatus.FAILED, null)
            .subscribe()
            .withSubscriber(UniAssertSubscriber.create());

    // then
    it.pagopa.selfcare.webhook.dto.NotificationResendResponse response =
        subscriber.awaitItem().getItem();
    assertEquals(0, response.getResentCount());
    assertTrue(response.getNotificationIds().isEmpty());
    verify(notificationPublisher, never()).publish(anyString());
  }

  @Test
  void resendNotificationsByStatus_shouldFail_whenWebhookIdIsInvalid() {
    // when
    UniAssertSubscriber<it.pagopa.selfcare.webhook.dto.NotificationResendResponse> subscriber =
        webhookService
            .resendNotificationsByStatus(
                WebhookNotification.NotificationStatus.FAILED, "not-an-object-id")
            .subscribe()
            .withSubscriber(UniAssertSubscriber.create());

    // then
    subscriber.awaitFailure().assertFailedWith(IllegalArgumentException.class);
    verify(notificationRepository, never())
        .findByStatus(any(WebhookNotification.NotificationStatus.class), any());
  }

  @Test
  void resendNotificationsByDateRange_shouldResendAllNotificationsInRange() {
    // given
    java.time.LocalDateTime from = java.time.LocalDateTime.now().minusDays(1);
    java.time.LocalDateTime to = java.time.LocalDateTime.now();
    WebhookNotification notification = new WebhookNotification();
    notification.setId(new ObjectId());
    notification.setStatus(WebhookNotification.NotificationStatus.FAILED);

    when(notificationRepository.findByCreatedAtRange(from, to))
        .thenReturn(Uni.createFrom().item(List.of(notification)));
    when(notificationRepository.update(any(WebhookNotification.class)))
        .thenAnswer(
            invocation -> {
              WebhookNotification updated = invocation.getArgument(0);
              return Uni.createFrom().item(updated);
            });
    when(notificationPublisher.publish(anyString())).thenReturn(Uni.createFrom().voidItem());
    when(notificationRepository.markAsPublished(any())).thenReturn(Uni.createFrom().voidItem());

    // when
    UniAssertSubscriber<it.pagopa.selfcare.webhook.dto.NotificationResendResponse> subscriber =
        webhookService
            .resendNotificationsByDateRange(from, to)
            .subscribe()
            .withSubscriber(UniAssertSubscriber.create());

    // then
    it.pagopa.selfcare.webhook.dto.NotificationResendResponse response =
        subscriber.awaitItem().getItem();
    assertEquals(1, response.getResentCount());
    verify(notificationRepository).findByCreatedAtRange(from, to);
  }

  @Test
  void resendNotificationById_shouldStillSucceed_whenPublishFails() {
    // given
    ObjectId notificationId = new ObjectId();
    WebhookNotification notification = new WebhookNotification();
    notification.setId(notificationId);
    notification.setStatus(WebhookNotification.NotificationStatus.FAILED);

    when(notificationRepository.findById(eq(notificationId)))
        .thenReturn(Uni.createFrom().item(notification));
    when(notificationRepository.update(any(WebhookNotification.class)))
        .thenAnswer(
            invocation -> {
              WebhookNotification updated = invocation.getArgument(0);
              return Uni.createFrom().item(updated);
            });
    when(notificationPublisher.publish(anyString()))
        .thenReturn(Uni.createFrom().failure(new RuntimeException("queue unavailable")));
    when(notificationRepository.releasePublishingLock(any()))
        .thenReturn(Uni.createFrom().voidItem());

    // when
    UniAssertSubscriber<it.pagopa.selfcare.webhook.dto.NotificationResendResponse> subscriber =
        webhookService
            .resendNotificationById(notificationId.toHexString())
            .subscribe()
            .withSubscriber(UniAssertSubscriber.create());

    // then
    // the resend still succeeds (notification reset to PENDING) even if the immediate publish
    // fails: WebhookNotificationOutboxService will retry the publish on its next scheduled run
    it.pagopa.selfcare.webhook.dto.NotificationResendResponse response =
        subscriber.awaitItem().getItem();
    assertEquals(1, response.getResentCount());
    verify(notificationRepository).releasePublishingLock(notificationId);
  }
}
