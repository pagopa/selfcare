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
import it.pagopa.selfcare.webhook.entity.WebhookNotificationAttempt;
import it.pagopa.selfcare.webhook.repository.WebhookNotificationAttemptRepository;
import it.pagopa.selfcare.webhook.repository.WebhookNotificationRepository;
import it.pagopa.selfcare.webhook.repository.WebhookRepository;
import it.pagopa.selfcare.webhook.util.DataEncryptionConfig;
import jakarta.inject.Inject;
import java.lang.reflect.Field;
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

  @InjectMock WebhookNotificationAttemptRepository notificationAttemptRepository;

  @InjectMock WebhookJwtService webhookJwtService;

  @InjectMock it.pagopa.selfcare.webhook.metrics.WebhookMetrics metrics;

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
    when(notificationAttemptRepository.persist(any(WebhookNotificationAttempt.class)))
        .thenAnswer(
            invocation -> {
              WebhookNotificationAttempt attempt = invocation.getArgument(0);
              return Uni.createFrom().item(attempt);
            });

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
    verify(metrics).recordDelivery("delivered");
    verify(metrics).recordDeliveryDuration(anyLong());
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
  void processNotification_shouldTruncateOversizedHttpErrorBody() {
    Webhook webhook = createWebhook();
    WebhookNotification notification = createNotification(webhook.getId());

    when(notificationRepository.update(any(WebhookNotification.class)))
        .thenReturn(Uni.createFrom().item(notification));
    when(httpRequest.sendJson(any())).thenReturn(Uni.createFrom().item(httpResponse));
    when(httpResponse.statusCode()).thenReturn(500);
    when(httpResponse.bodyAsString()).thenReturn("x".repeat(2000));

    notificationService
        .processNotification(notification, webhook)
        .subscribe()
        .withSubscriber(UniAssertSubscriber.create())
        .awaitItem();

    ArgumentCaptor<WebhookNotification> captor = ArgumentCaptor.forClass(WebhookNotification.class);
    verify(notificationRepository, atLeastOnce()).update(captor.capture());
    String lastError = captor.getValue().getLastError();
    assertEquals(WebhookNotification.NotificationStatus.RETRY, captor.getValue().getStatus());
    org.junit.jupiter.api.Assertions.assertTrue(lastError.startsWith("HTTP error 500: "));
    org.junit.jupiter.api.Assertions.assertTrue(lastError.endsWith("...(truncated)"));
    org.junit.jupiter.api.Assertions.assertTrue(
        lastError.length()
            < "HTTP error 500: ".length()
                + WebhookNotificationService.MAX_ERROR_BODY_CHARS
                + "...(truncated)".length()
                + 1);
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
    verify(metrics).recordDelivery("retry");
    verify(metrics).recordDeliveryDuration(anyLong());
  }

  @Test
  void processNotification_shouldRecordAttemptHistory_onSuccess() {
    Webhook webhook = createWebhook();
    WebhookNotification notification = createNotification(webhook.getId());

    when(notificationRepository.update(any(WebhookNotification.class)))
        .thenReturn(Uni.createFrom().item(notification));
    when(httpRequest.sendJson(any())).thenReturn(Uni.createFrom().item(httpResponse));
    when(httpResponse.statusCode()).thenReturn(200);

    notificationService
        .processNotification(notification, webhook)
        .subscribe()
        .withSubscriber(UniAssertSubscriber.create())
        .awaitItem();

    ArgumentCaptor<WebhookNotificationAttempt> captor =
        ArgumentCaptor.forClass(WebhookNotificationAttempt.class);
    verify(notificationAttemptRepository).persist(captor.capture());
    WebhookNotificationAttempt attempt = captor.getValue();

    assertEquals(notification.getId(), attempt.getNotificationId());
    assertEquals(1, attempt.getAttemptNumber());
    assertEquals(WebhookNotification.NotificationStatus.DELIVERED, attempt.getOutcome());
    assertEquals(200, attempt.getStatusCode());
    assertNotNull(attempt.getFinishedAt());
  }

  @Test
  void processNotification_shouldNotDowngradeDeliveredNotification_whenPersistFailsAfter2xx() {
    // Regression guard: a failure raised *after* a successful 2xx (here the MongoDB update) must
    // not be re-routed into the failure handling path. Doing so would record the delivery twice,
    // overwrite the already DELIVERED status with RETRY/FAILED and append a duplicate attempt
    // record for the same attempt number.
    Webhook webhook = createWebhook();
    WebhookNotification notification = createNotification(webhook.getId());

    when(notificationRepository.update(any(WebhookNotification.class)))
        .thenReturn(Uni.createFrom().item(notification))
        .thenReturn(Uni.createFrom().failure(new RuntimeException("mongo unavailable")));
    when(httpRequest.sendJson(any())).thenReturn(Uni.createFrom().item(httpResponse));
    when(httpResponse.statusCode()).thenReturn(200);

    notificationService
        .processNotification(notification, webhook)
        .subscribe()
        .withSubscriber(UniAssertSubscriber.create())
        .awaitFailure();

    assertEquals(WebhookNotification.NotificationStatus.DELIVERED, notification.getStatus());
    verify(notificationAttemptRepository, times(1)).persist(any(WebhookNotificationAttempt.class));
    verify(metrics, times(1)).recordDelivery("delivered");
    verify(metrics, never()).recordDelivery("retry");
    verify(metrics, never()).recordDelivery("failed");
    verify(metrics, times(1)).recordDeliveryDuration(anyLong());
  }

  @Test
  void processNotification_shouldAppendAttemptHistory_acrossMultipleRetries() {
    // Simulates the same notification document being reprocessed twice (e.g. two consecutive
    // Storage Queue redeliveries), which is exactly the scenario where the notification's own
    // lastError/lastAttemptAt fields get overwritten on every retry.
    Webhook webhook = createWebhook();
    webhook.getRetryPolicy().setMaxAttempts(5);
    WebhookNotification notification = createNotification(webhook.getId());

    when(notificationRepository.update(any(WebhookNotification.class)))
        .thenReturn(Uni.createFrom().item(notification));
    when(httpRequest.sendJson(any()))
        .thenReturn(Uni.createFrom().failure(new RuntimeException("Connection refused")))
        .thenReturn(Uni.createFrom().failure(new RuntimeException("Timeout")));

    notificationService
        .processNotification(notification, webhook)
        .subscribe()
        .withSubscriber(UniAssertSubscriber.create())
        .awaitItem();
    notificationService
        .processNotification(notification, webhook)
        .subscribe()
        .withSubscriber(UniAssertSubscriber.create())
        .awaitItem();

    ArgumentCaptor<WebhookNotificationAttempt> captor =
        ArgumentCaptor.forClass(WebhookNotificationAttempt.class);
    verify(notificationAttemptRepository, org.mockito.Mockito.times(2)).persist(captor.capture());
    List<WebhookNotificationAttempt> attempts = captor.getAllValues();

    // Both attempts are persisted as distinct, immutable records: the second retry does not
    // erase the first one's error/outcome, unlike the notification document itself.
    assertEquals(1, attempts.get(0).getAttemptNumber());
    org.junit.jupiter.api.Assertions.assertTrue(
        attempts.get(0).getErrorMessage().contains("Connection refused"));
    assertEquals(2, attempts.get(1).getAttemptNumber());
    org.junit.jupiter.api.Assertions.assertTrue(attempts.get(1).getErrorMessage().contains("Timeout"));
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
    verify(metrics).recordDelivery("failed");
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
    verify(metrics).recordClaim("batch", 1);
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
    notification.setPayload(DataEncryptionConfig.encrypt("{}"));
    notification.setStatus(WebhookNotification.NotificationStatus.PENDING);
    notification.setAttemptCount(0);
    return notification;
  }
}
