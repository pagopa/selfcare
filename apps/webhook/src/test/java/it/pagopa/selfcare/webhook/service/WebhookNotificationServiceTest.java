package it.pagopa.selfcare.webhook.service;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.test.UniAssertSubscriber;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.core.buffer.Buffer;
import io.vertx.mutiny.ext.web.client.HttpRequest;
import io.vertx.mutiny.ext.web.client.HttpResponse;
import io.vertx.mutiny.ext.web.client.WebClient;
import it.pagopa.selfcare.webhook.entity.RetryPolicy;
import it.pagopa.selfcare.webhook.entity.Webhook;
import it.pagopa.selfcare.webhook.entity.WebhookNotification;
import it.pagopa.selfcare.webhook.repository.WebhookNotificationRepository;
import it.pagopa.selfcare.webhook.repository.WebhookRepository;
import jakarta.inject.Inject;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.lang.reflect.Field;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@QuarkusTest
class WebhookNotificationServiceTest {

  @Inject
  WebhookNotificationService notificationService;

  @InjectMock
  WebhookRepository webhookRepository;

  @InjectMock
  WebhookNotificationRepository notificationRepository;

  Vertx vertx;

  private WebClient webClient;
  private HttpRequest<Buffer> httpRequest;
  private HttpResponse<Buffer> httpResponse;

  @BeforeEach
  void setUp() throws IllegalAccessException, NoSuchFieldException {
    webClient = mock(WebClient.class);
    httpRequest = mock(HttpRequest.class);
    httpResponse = mock(HttpResponse.class);

    when(webClient.request(any(), anyInt(), anyString(), anyString())).thenReturn(httpRequest);
    when(httpRequest.ssl(anyBoolean())).thenReturn(httpRequest);
    when(httpRequest.timeout(anyLong())).thenReturn(httpRequest);
    when(httpRequest.putHeader(anyString(), anyString())).thenReturn(httpRequest);

    Object serviceInstance = io.quarkus.arc.ClientProxy.unwrap(notificationService);
    Field field = WebhookNotificationService.class.getDeclaredField("webClient");
    field.setAccessible(true);
    field.set(serviceInstance, webClient);
  }

  @Test
  void processNotification_shouldSendSuccessfully() {
    Webhook webhook = createWebhook();
    WebhookNotification notification = createNotification(webhook.getId());

    when(notificationRepository.update(any(WebhookNotification.class))).thenReturn(Uni.createFrom().item(notification));
    when(httpRequest.sendBuffer(any())).thenReturn(Uni.createFrom().item(httpResponse));
    when(httpResponse.statusCode()).thenReturn(200);

    UniAssertSubscriber<Void> subscriber = notificationService.processNotification(notification, webhook)
      .subscribe().withSubscriber(UniAssertSubscriber.create());

    subscriber.awaitItem();
    ArgumentCaptor<WebhookNotification> captor = ArgumentCaptor.forClass(WebhookNotification.class);
    verify(notificationRepository, atLeastOnce()).update(captor.capture());
    WebhookNotification captured = captor.getValue();
    assertEquals(WebhookNotification.NotificationStatus.SUCCESS, captured.getStatus());
    assertNotNull(captured.getCompletedAt());
  }

  @Test
  void processNotification_shouldRetry_whenHttpError() {
    Webhook webhook = createWebhook();
    WebhookNotification notification = createNotification(webhook.getId());

    when(notificationRepository.update(any(WebhookNotification.class))).thenReturn(Uni.createFrom().item(notification));
    when(httpRequest.sendBuffer(any())).thenReturn(Uni.createFrom().failure(new RuntimeException("Connection refused")));

    UniAssertSubscriber<Void> subscriber = notificationService.processNotification(notification, webhook)
      .subscribe().withSubscriber(UniAssertSubscriber.create());

    subscriber.awaitItem();

    ArgumentCaptor<WebhookNotification> captor = ArgumentCaptor.forClass(WebhookNotification.class);
    verify(notificationRepository, atLeastOnce()).update(captor.capture());
    WebhookNotification captured = captor.getValue();

    assertEquals(WebhookNotification.NotificationStatus.RETRY, captured.getStatus());
    assertEquals(1, captured.getAttemptCount());
    org.junit.jupiter.api.Assertions.assertTrue(captured.getLastError().contains("Connection refused"));
  }

  @Test
  void processNotification_shouldFailPermanently_whenMaxAttemptsReached() {
    Webhook webhook = createWebhook();
    webhook.getRetryPolicy().setMaxAttempts(1); // Only 1 attempt allowed

    WebhookNotification notification = createNotification(webhook.getId());
    notification.setAttemptCount(1); // Already tried once (this is the retry)

    when(notificationRepository.update(any(WebhookNotification.class))).thenReturn(Uni.createFrom().item(notification));
    when(httpRequest.sendBuffer(any())).thenReturn(Uni.createFrom().failure(new RuntimeException("Connection refused")));

    UniAssertSubscriber<Void> subscriber = notificationService.processNotification(notification, webhook)
      .subscribe().withSubscriber(UniAssertSubscriber.create());

    subscriber.awaitItem();

    ArgumentCaptor<WebhookNotification> captor = ArgumentCaptor.forClass(WebhookNotification.class);
    verify(notificationRepository, atLeastOnce()).update(captor.capture());
    WebhookNotification captured = captor.getValue();

    assertEquals(WebhookNotification.NotificationStatus.FAILED, captured.getStatus());
    assertNotNull(captured.getCompletedAt());
  }

  @Test
  void processNotification_shouldFail_whenWebhookNotActive() {
    Webhook webhook = createWebhook();
    webhook.setStatus(Webhook.WebhookStatus.INACTIVE);
    WebhookNotification notification = createNotification(webhook.getId());

    when(notificationRepository.update(any(WebhookNotification.class))).thenReturn(Uni.createFrom().item(notification));

    UniAssertSubscriber<Void> subscriber = notificationService.processNotification(notification, webhook)
      .subscribe().withSubscriber(UniAssertSubscriber.create());

    subscriber.awaitItem();

    ArgumentCaptor<WebhookNotification> captor = ArgumentCaptor.forClass(WebhookNotification.class);
    verify(notificationRepository).update(captor.capture());
    WebhookNotification captured = captor.getValue();

    assertEquals(WebhookNotification.NotificationStatus.FAILED, captured.getStatus());
    assertEquals("Webhook is not active", captured.getLastError());
    verifyNoInteractions(webClient);
  }

  @Test
  void processFailedNotifications_shouldProcessPending() {
    Webhook webhook = createWebhook();
    WebhookNotification notification = createNotification(webhook.getId());

    when(notificationRepository.findAndLockPendingNotifications(anyInt(), anyInt()))
      .thenReturn(Uni.createFrom().item(List.of(notification)));

    when(notificationRepository.findById(any(ObjectId.class))).thenReturn(Uni.createFrom().item(notification));
    when(webhookRepository.findById(any(ObjectId.class))).thenReturn(Uni.createFrom().item(webhook));
    when(notificationRepository.update(any(WebhookNotification.class))).thenReturn(Uni.createFrom().item(notification));
    when(notificationRepository.releaseProcessingLock(any())).thenReturn(Uni.createFrom().item(notification));

    when(httpRequest.sendBuffer(any())).thenReturn(Uni.createFrom().item(httpResponse));
    when(httpResponse.statusCode()).thenReturn(200);

    UniAssertSubscriber<Void> subscriber = notificationService.processFailedNotifications()
      .subscribe().withSubscriber(UniAssertSubscriber.create());

    subscriber.awaitItem();

    verify(notificationRepository).findAndLockPendingNotifications(anyInt(), anyInt());
    verify(notificationRepository).releaseProcessingLock(any());
  }

  private Webhook createWebhook() {
    Webhook webhook = new Webhook();
    webhook.setId(new ObjectId());
    webhook.setUrl("http://example.com/webhook");
    webhook.setHttpMethod("POST");
    webhook.setStatus(Webhook.WebhookStatus.ACTIVE);

    RetryPolicy retryPolicy = new RetryPolicy();
    retryPolicy.setMaxAttempts(3);
    webhook.setRetryPolicy(retryPolicy);

    return webhook;
  }

  private WebhookNotification createNotification(ObjectId webhookId) {
    WebhookNotification notification = new WebhookNotification();
    notification.setId(new ObjectId());
    notification.setWebhookId(webhookId);
    notification.setPayload("{}");
    notification.setStatus(WebhookNotification.NotificationStatus.PENDING);
    notification.setAttemptCount(0);
    return notification;
  }
}