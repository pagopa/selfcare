package it.pagopa.selfcare.webhook.health;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.webhook.entity.WebhookNotification;
import it.pagopa.selfcare.webhook.repository.WebhookNotificationRepository;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.junit.jupiter.api.Test;

@QuarkusTest
class WebhookOutboxReadinessCheckTest {

  private WebhookOutboxReadinessCheck newCheck(WebhookNotificationRepository repository) {
    WebhookOutboxReadinessCheck check = new WebhookOutboxReadinessCheck();
    check.notificationRepository = repository;
    check.enabled = true;
    check.lagThresholdSeconds = 300L;
    return check;
  }

  private HealthCheckResponse await(WebhookOutboxReadinessCheck check) {
    return check.call().await().atMost(Duration.ofSeconds(5));
  }

  @Test
  void up_whenDisabled_withoutQueryingRepository() {
    // given
    WebhookNotificationRepository repository = mock(WebhookNotificationRepository.class);
    WebhookOutboxReadinessCheck check = newCheck(repository);
    check.enabled = false;

    // when
    HealthCheckResponse response = await(check);

    // then
    assertThat(response.getStatus()).isEqualTo(HealthCheckResponse.Status.UP);
    org.mockito.Mockito.verifyNoInteractions(repository);
  }

  @Test
  void up_whenNoUnpublishedNotificationExists() {
    // given
    WebhookNotificationRepository repository = mock(WebhookNotificationRepository.class);
    when(repository.findOldestUnpublishedNotification()).thenReturn(Uni.createFrom().nullItem());
    WebhookOutboxReadinessCheck check = newCheck(repository);

    // when
    HealthCheckResponse response = await(check);

    // then
    assertThat(response.getStatus()).isEqualTo(HealthCheckResponse.Status.UP);
    Map<String, Object> data = response.getData().orElseThrow();
    assertThat(data).containsEntry("component", "outbox").containsEntry("lagThresholdSeconds", "300");
  }

  @Test
  void up_whenOldestUnpublishedNotificationIsWithinThreshold() {
    // given
    WebhookNotification notification = new WebhookNotification();
    notification.setCreatedAt(LocalDateTime.now().minusSeconds(10));
    WebhookNotificationRepository repository = mock(WebhookNotificationRepository.class);
    when(repository.findOldestUnpublishedNotification()).thenReturn(Uni.createFrom().item(notification));
    WebhookOutboxReadinessCheck check = newCheck(repository);

    // when
    HealthCheckResponse response = await(check);

    // then
    assertThat(response.getStatus()).isEqualTo(HealthCheckResponse.Status.UP);
  }

  @Test
  void down_whenOldestUnpublishedNotificationExceedsThreshold() {
    // given
    WebhookNotification notification = new WebhookNotification();
    notification.setCreatedAt(LocalDateTime.now().minusSeconds(600));
    WebhookNotificationRepository repository = mock(WebhookNotificationRepository.class);
    when(repository.findOldestUnpublishedNotification()).thenReturn(Uni.createFrom().item(notification));
    WebhookOutboxReadinessCheck check = newCheck(repository);

    // when
    HealthCheckResponse response = await(check);

    // then
    assertThat(response.getStatus()).isEqualTo(HealthCheckResponse.Status.DOWN);
  }

  @Test
  void down_whenRepositoryQueryFails() {
    // given
    WebhookNotificationRepository repository = mock(WebhookNotificationRepository.class);
    when(repository.findOldestUnpublishedNotification())
        .thenReturn(Uni.createFrom().failure(new RuntimeException("mongo unreachable")));
    WebhookOutboxReadinessCheck check = newCheck(repository);

    // when
    HealthCheckResponse response = await(check);

    // then
    assertThat(response.getStatus()).isEqualTo(HealthCheckResponse.Status.DOWN);
  }
}
