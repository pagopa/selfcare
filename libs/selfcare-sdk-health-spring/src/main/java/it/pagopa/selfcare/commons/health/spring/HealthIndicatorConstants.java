package it.pagopa.selfcare.commons.health.spring;

import java.time.Duration;

public final class HealthIndicatorConstants {

    public static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(2);
    public static final String DETAIL_COMPONENT = "component";
    public static final String DETAIL_ERROR = "error";
    public static final String DETAIL_LATENCY_MS = "latencyMs";
    public static final String DETAIL_BLOB_ACCOUNT = "account";
    public static final String DETAIL_BLOB_CONTAINER = "container";
    public static final String DETAIL_BLOB_PROBE_TARGET = "probeTarget";
    public static final String DETAIL_MONGO_DATABASE = "database";
    public static final String DETAIL_MONGO_HOST = "host";

    private HealthIndicatorConstants() {
    }
}
