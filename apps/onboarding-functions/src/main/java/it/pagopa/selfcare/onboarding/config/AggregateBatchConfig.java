package it.pagopa.selfcare.onboarding.config;

import io.smallrye.config.ConfigMapping;

@ConfigMapping(prefix = "onboarding-functions.aggregate-batch")
public interface AggregateBatchConfig {

    /**
     * Number of aggregates to process in parallel within each batch.
     * Lower values reduce DB load, higher values increase throughput.
     */
    Integer size();

    /**
     * Maximum number of batches to process before calling continueAsNew to reset orchestration history.
     * Prevents history explosion in long-running orchestrations.
     */
    Integer maxBatchesBeforeContinue();

    /**
     * Delay in seconds between batch processing to avoid DB overload (TooManyRequests).
     */
    Integer delaySeconds();

}
