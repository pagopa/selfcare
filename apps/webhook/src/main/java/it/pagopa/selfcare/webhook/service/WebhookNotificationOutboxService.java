package it.pagopa.selfcare.webhook.service;

import io.quarkus.scheduler.Scheduled;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.webhook.repository.WebhookNotificationRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@Slf4j
@ApplicationScoped
public class WebhookNotificationOutboxService {

  @Inject WebhookNotificationRepository notificationRepository;
  @Inject WebhookNotificationPublisher publisher;

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
        .transformToMulti(Multi.createFrom()::iterable)
        .onItem()
        .transformToUniAndMerge(
            notification ->
                publisher
                    .publish(notification.getId().toHexString())
                    .call(ignored -> notificationRepository.markAsPublished(notification.getId()))
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
}
