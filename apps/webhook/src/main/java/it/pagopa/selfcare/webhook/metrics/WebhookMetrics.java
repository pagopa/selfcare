package it.pagopa.selfcare.webhook.metrics;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.DoubleHistogram;
import io.opentelemetry.api.metrics.LongCounter;
import io.opentelemetry.api.metrics.Meter;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Central place for every metric emitted by the webhook notification pipeline (publish, claim,
 * delivery, retry, discard/DLQ, outbox lag). Wrapping the raw OpenTelemetry {@link Meter} in a
 * dedicated CDI bean keeps instrument definitions (names, units, descriptions) in one spot and
 * lets the rest of the codebase depend on typed methods instead of building {@link Attributes} at
 * every call site; it also makes the recording calls trivially mockable/verifiable in unit tests
 * via {@code @InjectMock}.
 */
@ApplicationScoped
public class WebhookMetrics {

  private static final AttributeKey<String> OUTCOME = AttributeKey.stringKey("outcome");
  private static final AttributeKey<String> SOURCE = AttributeKey.stringKey("source");
  private static final AttributeKey<String> REASON = AttributeKey.stringKey("reason");

  private final LongCounter publishCounter;
  private final DoubleHistogram publishDuration;
  private final LongCounter claimCounter;
  private final LongCounter deliveryCounter;
  private final DoubleHistogram deliveryDuration;
  private final LongCounter discardedCounter;
  private final DoubleHistogram outboxLag;

  @Inject
  public WebhookMetrics(Meter meter) {
    this.publishCounter =
        meter
            .counterBuilder("webhook.notification.publish")
            .setDescription("Number of webhook notifications published to the Storage Queue")
            .setUnit("{notification}")
            .build();
    this.publishDuration =
        meter
            .histogramBuilder("webhook.notification.publish.duration")
            .setDescription("Latency of publishing a webhook notification to the Storage Queue")
            .setUnit("ms")
            .build();
    this.claimCounter =
        meter
            .counterBuilder("webhook.notification.claim")
            .setDescription(
                "Number of webhook notifications claimed for processing or publishing")
            .setUnit("{notification}")
            .build();
    this.deliveryCounter =
        meter
            .counterBuilder("webhook.notification.delivery")
            .setDescription("Outcome of webhook notification delivery attempts")
            .setUnit("{notification}")
            .build();
    this.deliveryDuration =
        meter
            .histogramBuilder("webhook.notification.delivery.duration")
            .setDescription("Latency of the outbound HTTP webhook delivery call")
            .setUnit("ms")
            .build();
    this.discardedCounter =
        meter
            .counterBuilder("webhook.notification.discarded")
            .setDescription("Number of Storage Queue messages discarded without a retry")
            .setUnit("{message}")
            .build();
    this.outboxLag =
        meter
            .histogramBuilder("webhook.notification.outbox.lag")
            .setDescription(
                "Time elapsed between notification creation and successful publish to the"
                    + " Storage Queue")
            .setUnit("ms")
            .build();
  }

  /** Records the outcome and latency of a single publish-to-queue attempt. */
  public void recordPublish(boolean success, long durationMs) {
    publishCounter.add(1, Attributes.of(OUTCOME, success ? "success" : "failure"));
    publishDuration.record(durationMs);
  }

  /**
   * Records a claim attempt (a worker trying to atomically lock a notification for processing or
   * publishing). {@code source} identifies the claim path (e.g. {@code queue}, {@code outbox},
   * {@code batch}); {@code claimedCount} is the number of notifications actually claimed (0 when
   * nothing was available).
   */
  public void recordClaim(String source, int claimedCount) {
    claimCounter.add(
        Math.max(claimedCount, 0),
        Attributes.of(SOURCE, source, OUTCOME, claimedCount > 0 ? "claimed" : "empty"));
  }

  /** Records the terminal outcome of a delivery attempt: {@code delivered}, {@code retry}, or
   * {@code failed} (permanent failure, max attempts exceeded). */
  public void recordDelivery(String outcome) {
    deliveryCounter.add(1, Attributes.of(OUTCOME, outcome));
  }

  /** Records the latency of the outbound HTTP call for a single delivery attempt. */
  public void recordDeliveryDuration(long durationMs) {
    deliveryDuration.record(durationMs);
  }

  /**
   * Records a Storage Queue message that was discarded (deleted) without further processing —
   * used as a proxy for a dead-letter queue, since the pipeline does not have a separate DLQ.
   */
  public void recordDiscarded(String reason) {
    discardedCounter.add(1, Attributes.of(REASON, reason));
  }

  /** Records the time elapsed between notification creation and its successful publish. */
  public void recordOutboxLag(long durationMs) {
    outboxLag.record(durationMs);
  }
}
