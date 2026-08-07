package it.pagopa.selfcare.webhook.service;

import io.quarkus.scheduler.Scheduled;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.webhook.entity.WebhookNotification;
import it.pagopa.selfcare.webhook.metrics.WebhookMetrics;
import it.pagopa.selfcare.webhook.repository.WebhookNotificationRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.time.Duration;
import java.time.LocalDateTime;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@Slf4j
@ApplicationScoped
public class WebhookNotificationOutboxService {

  @Inject WebhookNotificationRepository notificationRepository;
  @Inject WebhookNotificationPublisher publisher;
  @Inject WebhookMetrics metrics;

  @ConfigProperty(name = "webhook.storage-queue.enabled", defaultValue = "false")
  boolean enabled;

  @Scheduled(every = "${webhook.storage-queue.outbox-interval:30s}")
  Uni<Void> publishUnpublishedNotifications() {
    if (!enabled) {
      return Uni.createFrom().voidItem();
    }
    return notificationRepository
        .claimUnpublishedNotifications(100, 5)
        .onItem()
        .invoke(notifications -> metrics.recordClaim("outbox", notifications.size()))
        .onItem()
        .transformToMulti(Multi.createFrom()::iterable)
        .onItem()
        .transformToUniAndMerge(
            notification ->
                publisher
                    .publish(notification.getId().toHexString())
                    .call(
                        ignored ->
                            notificationRepository
                                .markAsPublished(notification.getId())
                                .invoke(() -> recordOutboxLag(notification)))
                    .onFailure()
                    .call(
                        error ->
                            notificationRepository
                                .releasePublishingLock(notification.getId())
                                .invoke(
                                    () ->
                                        log.error(
                                            "Unable to publish webhook notification {}",
                                            notification.getId(),
                                            error))))
        .collect()
        .asList()
        .replaceWithVoid();
  }

  /**
   * Records how long a notification waited in the outbox (from creation to a successful publish
   * to the Storage Queue), so a growing lag can be spotted before it turns into a delivery delay.
   */
  private void recordOutboxLag(WebhookNotification notification) {
    if (notification.getCreatedAt() == null) {
      return;
    }
    long lagMs = Duration.between(notification.getCreatedAt(), LocalDateTime.now()).toMillis();
    metrics.recordOutboxLag(Math.max(lagMs, 0));
  }
}
