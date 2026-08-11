package it.pagopa.selfcare.webhook.health;

import io.smallrye.health.api.HealthGroup;
import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.commons.health.AbstractAsyncReadinessCheck;
import it.pagopa.selfcare.commons.health.HealthCheckConstants;
import it.pagopa.selfcare.webhook.entity.WebhookNotification;
import it.pagopa.selfcare.webhook.repository.WebhookNotificationRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * Verifies that notifications are not piling up in the outbox (i.e. staying unpublished to the
 * Storage Queue for too long). Reports {@code DOWN} when the oldest {@code PENDING} notification
 * that has never been published is older than {@code
 * webhook.storage-queue.outbox-lag-threshold-seconds}, which usually indicates the scheduled
 * {@code WebhookNotificationOutboxService} job has stalled or the Storage Queue is unreachable.
 *
 * <p>When {@code webhook.storage-queue.enabled} is {@code false}, the outbox is not in use, so the
 * check reports {@code UP} without querying MongoDB.
 *
 * <p>This check is deliberately <b>not</b> a {@code @Readiness} check: the outbox lag is a global
 * condition, so a backlog would take every replica out of the load balancer at the same time, the
 * REST API would answer {@code 503} and new webhook notifications could no longer be accepted —
 * making the backlog worse. It is exposed as a non-gating diagnostics group on {@code
 * /q/health/group/diagnostics} instead, and is meant to be alerted on together with the {@code
 * webhook.notification.outbox.lag} metric.
 */
@HealthGroup(WebhookOutboxLagCheck.DIAGNOSTICS_GROUP)
@ApplicationScoped
public class WebhookOutboxLagCheck extends AbstractAsyncReadinessCheck {

  /** Non-gating health group: exposed on {@code /q/health/group/diagnostics}, not on the probe. */
  public static final String DIAGNOSTICS_GROUP = "diagnostics";

  @Inject WebhookNotificationRepository notificationRepository;

  @ConfigProperty(name = "webhook.storage-queue.enabled", defaultValue = "false")
  boolean enabled;

  @ConfigProperty(name = "webhook.storage-queue.outbox-lag-threshold-seconds", defaultValue = "300")
  long lagThresholdSeconds;

  @Override
  protected String checkName() {
    return "webhook-outbox-lag";
  }

  @Override
  protected Map<String, String> data() {
    Map<String, String> data = new HashMap<>(2);
    data.put(HealthCheckConstants.DATA_KEY_COMPONENT, "outbox");
    data.put("lagThresholdSeconds", String.valueOf(lagThresholdSeconds));
    return data;
  }

  @Override
  protected Uni<?> probe() {
    if (!enabled) {
      return Uni.createFrom().voidItem();
    }
    return notificationRepository
        .findOldestUnpublishedNotification()
        .onItem()
        .transformToUni(this::checkLag);
  }

  private Uni<Void> checkLag(WebhookNotification oldestUnpublished) {
    if (oldestUnpublished == null || oldestUnpublished.getCreatedAt() == null) {
      return Uni.createFrom().voidItem();
    }
    long ageSeconds =
        Duration.between(
                oldestUnpublished.getCreatedAt().atZone(ZoneOffset.UTC).toInstant(), Instant.now())
            .getSeconds();
    if (ageSeconds > lagThresholdSeconds) {
      return Uni.createFrom()
          .failure(
              new IllegalStateException(
                  "Oldest unpublished notification is "
                      + ageSeconds
                      + "s old (threshold "
                      + lagThresholdSeconds
                      + "s)"));
    }
    return Uni.createFrom().voidItem();
  }
}
