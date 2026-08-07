package it.pagopa.selfcare.webhook.health;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import it.pagopa.selfcare.commons.health.AbstractAsyncReadinessCheck;
import it.pagopa.selfcare.commons.health.HealthCheckConstants;
import it.pagopa.selfcare.webhook.service.WebhookNotificationConsumer;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.HashMap;
import java.util.Map;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.health.Readiness;

/**
 * Verifies that the Azure Storage Queue used for webhook notification delivery is actually
 * reachable, reusing the {@link WebhookNotificationConsumer}'s already-initialized {@code
 * QueueClient} (avoiding a second client/connection just for the health probe).
 *
 * <p>When {@code webhook.storage-queue.enabled} is {@code false}, the queue integration is not
 * used at all, so the check reports {@code UP} without touching any client.
 */
@Readiness
@ApplicationScoped
public class WebhookStorageQueueReadinessCheck extends AbstractAsyncReadinessCheck {

  @Inject WebhookNotificationConsumer consumer;

  @ConfigProperty(name = "webhook.storage-queue.enabled", defaultValue = "false")
  boolean enabled;

  @ConfigProperty(name = "webhook.storage-queue.queue", defaultValue = "webhook-notifications")
  String queue;

  @Override
  protected String checkName() {
    return "storage-queue-webhook-notifications";
  }

  @Override
  protected Map<String, String> data() {
    Map<String, String> data = new HashMap<>(3);
    data.put(HealthCheckConstants.DATA_KEY_COMPONENT, "storage-queue");
    data.put("queue", queue);
    data.put("enabled", String.valueOf(enabled));
    return data;
  }

  @Override
  protected Uni<?> probe() {
    if (!enabled) {
      return Uni.createFrom().voidItem();
    }
    var client = consumer.getClient();
    if (client == null) {
      return Uni.createFrom()
          .failure(new IllegalStateException("Storage Queue client is not initialized"));
    }
    return Uni.createFrom()
        .item(client::getProperties)
        .runSubscriptionOn(Infrastructure.getDefaultWorkerPool());
  }
}
