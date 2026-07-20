package it.pagopa.selfcare.webhook.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

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
import it.pagopa.selfcare.webhook.util.DataEncryptionConfig;
import jakarta.inject.Inject;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

@QuarkusTest
class WebhookNotificationServiceTest {

  @Inject WebhookNotificationService notificationService;

  @InjectMock WebhookRepository webhookRepository;

  @InjectMock WebhookNotificationRepository notificationRepository;

  @InjectMock WebhookJwtService webhookJwtService;

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
    when(webhookJwtService.generateNotificationToken(
            any(Webhook.class), any(WebhookNotification.class)))
        .thenReturn(Uni.createFrom().item("signed-token"));

    Object serviceInstance = io.quarkus.arc.ClientProxy.unwrap(notificationService);
    Field field = WebhookNotificationService.class.getDeclaredField("webClient");
    field.setAccessible(true);
    field.set(serviceInstance, webClient);
  }

  @Test
  void processNotification_shouldSendSuccessfully() {
    // given
    Webhook webhook = createWebhook();
    WebhookNotification notification = createNotification(webhook.getId());

    when(notificationRepository.update(any(WebhookNotification.class)))
        .thenReturn(Uni.createFrom().item(notification));
    when(httpRequest.sendJson(any())).thenReturn(Uni.createFrom().item(httpResponse));
    when(httpResponse.statusCode()).thenReturn(200);

    // when
    UniAssertSubscriber<Void> subscriber =
        notificationService
            .processNotification(notification, webhook)
            .subscribe()
            .withSubscriber(UniAssertSubscriber.create());

    subscriber.awaitItem();

    // then
    ArgumentCaptor<WebhookNotification> captor = ArgumentCaptor.forClass(WebhookNotification.class);
    verify(notificationRepository, atLeastOnce()).update(captor.capture());
    WebhookNotification captured = captor.getValue();
    assertEquals(WebhookNotification.NotificationStatus.DELIVERED, captured.getStatus());
    assertNotNull(captured.getCompletedAt());
    verify(httpRequest).putHeader("Authorization", "Bearer signed-token");
    verify(httpRequest)
        .sendJson(argThat(payload -> payload instanceof Map<?, ?> map && map.isEmpty()));
  }

  @Test
  void processNotification_shouldPreserveQueryStringInRequestPath() {
    // given
    Webhook webhook = createWebhook("https://example.com/api/webhook?code=test-function-key");
    WebhookNotification notification = createNotification(webhook.getId());

    when(notificationRepository.update(any(WebhookNotification.class)))
        .thenReturn(Uni.createFrom().item(notification));
    when(httpRequest.sendJson(any())).thenReturn(Uni.createFrom().item(httpResponse));
    when(httpResponse.statusCode()).thenReturn(200);

    // when
    UniAssertSubscriber<Void> subscriber =
        notificationService
            .processNotification(notification, webhook)
            .subscribe()
            .withSubscriber(UniAssertSubscriber.create());

    subscriber.awaitItem();

    // then
    verify(webClient)
        .request(
            eq(io.vertx.core.http.HttpMethod.POST),
            eq(443),
            eq("example.com"),
            eq("/api/webhook?code=test-function-key"));
  }

  @Test
  void processNotification_shouldPopulateAdditionalHeadersFromWebhook() {
    // given
    Webhook webhook = createWebhook();
    webhook.setHeaders(
        DataEncryptionConfig.encrypt(
            Map.of("x-functions-key", "function-secret", "x-custom-header", "custom-value")));
    WebhookNotification notification = createNotification(webhook.getId());

    when(notificationRepository.update(any(WebhookNotification.class)))
        .thenReturn(Uni.createFrom().item(notification));
    when(httpRequest.sendJson(any())).thenReturn(Uni.createFrom().item(httpResponse));
    when(httpResponse.statusCode()).thenReturn(200);

    // when
    UniAssertSubscriber<Void> subscriber =
        notificationService
            .processNotification(notification, webhook)
            .subscribe()
            .withSubscriber(UniAssertSubscriber.create());

    subscriber.awaitItem();

    // then
    verify(httpRequest).putHeader("x-functions-key", "function-secret");
    verify(httpRequest).putHeader("x-custom-header", "custom-value");
  }

  @Test
  void processNotification_shouldRetry_whenHttpError() {
    Webhook webhook = createWebhook();
    WebhookNotification notification = createNotification(webhook.getId());

    when(notificationRepository.update(any(WebhookNotification.class)))
        .thenReturn(Uni.createFrom().item(notification));
    when(httpRequest.sendJson(any()))
        .thenReturn(Uni.createFrom().failure(new RuntimeException("Connection refused")));

    UniAssertSubscriber<Void> subscriber =
        notificationService
            .processNotification(notification, webhook)
            .subscribe()
            .withSubscriber(UniAssertSubscriber.create());

    subscriber.awaitItem();

    ArgumentCaptor<WebhookNotification> captor = ArgumentCaptor.forClass(WebhookNotification.class);
    verify(notificationRepository, atLeastOnce()).update(captor.capture());
    WebhookNotification captured = captor.getValue();

    assertEquals(WebhookNotification.NotificationStatus.RETRY, captured.getStatus());
    assertEquals(1, captured.getAttemptCount());
    org.junit.jupiter.api.Assertions.assertTrue(
        captured.getLastError().contains("Connection refused"));
  }

  @Test
  void processNotification_shouldFailPermanently_whenMaxAttemptsReached() {
    Webhook webhook = createWebhook();
    webhook.getRetryPolicy().setMaxAttempts(1); // Only 1 attempt allowed

    WebhookNotification notification = createNotification(webhook.getId());
    notification.setAttemptCount(1); // Already tried once (this is the retry)

    when(notificationRepository.update(any(WebhookNotification.class)))
        .thenReturn(Uni.createFrom().item(notification));
    when(httpRequest.sendJson(any()))
        .thenReturn(Uni.createFrom().failure(new RuntimeException("Connection refused")));

    UniAssertSubscriber<Void> subscriber =
        notificationService
            .processNotification(notification, webhook)
            .subscribe()
            .withSubscriber(UniAssertSubscriber.create());

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

    when(notificationRepository.update(any(WebhookNotification.class)))
        .thenReturn(Uni.createFrom().item(notification));

    UniAssertSubscriber<Void> subscriber =
        notificationService
            .processNotification(notification, webhook)
            .subscribe()
            .withSubscriber(UniAssertSubscriber.create());

    subscriber.awaitItem();

    ArgumentCaptor<WebhookNotification> captor = ArgumentCaptor.forClass(WebhookNotification.class);
    verify(notificationRepository).update(captor.capture());
    WebhookNotification captured = captor.getValue();

    assertEquals(WebhookNotification.NotificationStatus.FAILED, captured.getStatus());
    assertEquals("Webhook is not active", captured.getLastError());
    verifyNoInteractions(webClient);
  }

  @Test
  void processNotification_shouldComplete_whenNotificationIsNotFound() {
    // given
    String notificationId = new ObjectId().toHexString();
    when(notificationRepository.findById(any(ObjectId.class)))
        .thenReturn(Uni.createFrom().nullItem());

    // when
    UniAssertSubscriber<Void> subscriber =
        notificationService
            .processNotification(notificationId)
            .subscribe()
            .withSubscriber(UniAssertSubscriber.create());

    // then
    subscriber.awaitItem();
    verify(notificationRepository).findById(new ObjectId(notificationId));
    verifyNoInteractions(webhookRepository);
    verifyNoInteractions(webClient);
  }

  @Test
  void processNotification_shouldFailNotification_whenWebhookIsNotFound() {
    // given
    WebhookNotification notification = createNotification(new ObjectId());
    when(webhookRepository.findById(notification.getWebhookId()))
        .thenReturn(Uni.createFrom().nullItem());
    when(notificationRepository.update(any(WebhookNotification.class)))
        .thenReturn(Uni.createFrom().item(notification));

    // when
    UniAssertSubscriber<Void> subscriber =
        notificationService
            .processNotification(notification)
            .subscribe()
            .withSubscriber(UniAssertSubscriber.create());

    // then
    subscriber.awaitItem();
    ArgumentCaptor<WebhookNotification> captor = ArgumentCaptor.forClass(WebhookNotification.class);
    verify(notificationRepository).update(captor.capture());
    WebhookNotification captured = captor.getValue();
    assertEquals(WebhookNotification.NotificationStatus.FAILED, captured.getStatus());
    assertEquals("Webhook not found", captured.getLastError());
    assertNotNull(captured.getCompletedAt());
    verifyNoInteractions(webClient);
  }

  @Test
  void processFailedNotifications_shouldProcessPending() {
    Webhook webhook = createWebhook();
    WebhookNotification notification = createNotification(webhook.getId());

    when(notificationRepository.findAndLockPendingNotifications(anyInt(), anyInt()))
        .thenReturn(Uni.createFrom().item(List.of(notification)));

    when(notificationRepository.findById(any(ObjectId.class)))
        .thenReturn(Uni.createFrom().item(notification));
    when(webhookRepository.findById(any(ObjectId.class)))
        .thenReturn(Uni.createFrom().item(webhook));
    when(notificationRepository.update(any(WebhookNotification.class)))
        .thenReturn(Uni.createFrom().item(notification));
    when(notificationRepository.releaseProcessingLock(any()))
        .thenReturn(Uni.createFrom().item(notification));

    when(httpRequest.sendJson(any())).thenReturn(Uni.createFrom().item(httpResponse));
    when(httpResponse.statusCode()).thenReturn(200);

    UniAssertSubscriber<Void> subscriber =
        notificationService
            .processFailedNotifications()
            .subscribe()
            .withSubscriber(UniAssertSubscriber.create());

    subscriber.awaitItem();

    verify(notificationRepository).findAndLockPendingNotifications(anyInt(), anyInt());
    verify(notificationRepository).releaseProcessingLock(any());
  }

  private Webhook createWebhook() {
    return createWebhook("http://example.com/webhook");
  }

  private Webhook createWebhook(String url) {
    Webhook webhook = new Webhook();
    webhook.setId(new ObjectId());
    webhook.setUrl(url);
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
    notification.setPayload(
        Base64.getEncoder().encodeToString("{}".getBytes(StandardCharsets.UTF_8)));
    notification.setStatus(WebhookNotification.NotificationStatus.PENDING);
    notification.setAttemptCount(0);
    return notification;
  }
}
