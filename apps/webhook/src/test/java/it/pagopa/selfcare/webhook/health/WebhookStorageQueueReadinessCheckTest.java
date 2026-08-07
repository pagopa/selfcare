package it.pagopa.selfcare.webhook.health;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.azure.storage.queue.QueueClient;
import com.azure.storage.queue.models.QueueProperties;
import io.quarkus.test.junit.QuarkusTest;
import it.pagopa.selfcare.webhook.service.WebhookNotificationConsumer;
import java.time.Duration;
import java.util.Map;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.junit.jupiter.api.Test;

@QuarkusTest
class WebhookStorageQueueReadinessCheckTest {

  private WebhookStorageQueueReadinessCheck newCheck(WebhookNotificationConsumer consumer) {
    WebhookStorageQueueReadinessCheck check = new WebhookStorageQueueReadinessCheck();
    check.consumer = consumer;
    check.enabled = true;
    check.queue = "webhook-notifications";
    return check;
  }

  private HealthCheckResponse await(WebhookStorageQueueReadinessCheck check) {
    return check.call().await().atMost(Duration.ofSeconds(5));
  }

  @Test
  void up_whenDisabled_withoutTouchingClient() {
    // given
    WebhookNotificationConsumer consumer = mock(WebhookNotificationConsumer.class);
    WebhookStorageQueueReadinessCheck check = newCheck(consumer);
    check.enabled = false;

    // when
    HealthCheckResponse response = await(check);

    // then
    assertThat(response.getStatus()).isEqualTo(HealthCheckResponse.Status.UP);
    org.mockito.Mockito.verifyNoInteractions(consumer);
  }

  @Test
  void up_whenClientPropertiesAreReachable() {
    // given
    WebhookNotificationConsumer consumer = mock(WebhookNotificationConsumer.class);
    QueueClient client = mock(QueueClient.class);
    when(consumer.getClient()).thenReturn(client);
    when(client.getProperties()).thenReturn(mock(QueueProperties.class));
    WebhookStorageQueueReadinessCheck check = newCheck(consumer);

    // when
    HealthCheckResponse response = await(check);

    // then
    assertThat(response.getStatus()).isEqualTo(HealthCheckResponse.Status.UP);
    Map<String, Object> data = response.getData().orElseThrow();
    assertThat(data)
        .containsEntry("component", "storage-queue")
        .containsEntry("queue", "webhook-notifications");
  }

  @Test
  void down_whenClientIsNotInitialized() {
    // given
    WebhookNotificationConsumer consumer = mock(WebhookNotificationConsumer.class);
    when(consumer.getClient()).thenReturn(null);
    WebhookStorageQueueReadinessCheck check = newCheck(consumer);

    // when
    HealthCheckResponse response = await(check);

    // then
    assertThat(response.getStatus()).isEqualTo(HealthCheckResponse.Status.DOWN);
  }

  @Test
  void down_whenGetPropertiesFails() {
    // given
    WebhookNotificationConsumer consumer = mock(WebhookNotificationConsumer.class);
    QueueClient client = mock(QueueClient.class);
    when(consumer.getClient()).thenReturn(client);
    when(client.getProperties()).thenThrow(new RuntimeException("queue unreachable"));
    WebhookStorageQueueReadinessCheck check = newCheck(consumer);

    // when
    HealthCheckResponse response = await(check);

    // then
    assertThat(response.getStatus()).isEqualTo(HealthCheckResponse.Status.DOWN);
  }
}
