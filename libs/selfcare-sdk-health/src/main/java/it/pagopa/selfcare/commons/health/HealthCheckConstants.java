package it.pagopa.selfcare.commons.health;

import java.time.Duration;

/**
 * Shared constants for the Selfcare health SDK.
 *
 * <p>Keys used with {@code HealthCheckResponseBuilder.withData(...)} are standardized here so that
 * downstream tooling (Application Insights queries, Grafana dashboards, alert rules) can rely on a
 * stable schema across all Selfcare Quarkus microservices.
 */
public final class HealthCheckConstants {

    private HealthCheckConstants() {}

    /** Default per-check timeout applied to the probe {@code Uni} if the subclass does not override it. */
    public static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(2);

    /** Data key: logical component being probed (e.g. "blob-storage", "mongodb"). */
    public static final String DATA_KEY_COMPONENT = "component";

    /** Data key: exception class + message when the probe fails. */
    public static final String DATA_KEY_ERROR = "error";

    /** Data key: milliseconds spent executing the probe (best-effort). */
    public static final String DATA_KEY_LATENCY_MS = "latencyMs";

    /** Data key: Azure Blob Storage account name. */
    public static final String DATA_KEY_BLOB_ACCOUNT = "account";

    /** Data key: Azure Blob Storage container name. */
    public static final String DATA_KEY_BLOB_CONTAINER = "container";

    /** Data key: canary blob path used for the readiness probe. */
    public static final String DATA_KEY_BLOB_CANARY = "canaryBlob";

    /** Data key: MongoDB database name being pinged. */
    public static final String DATA_KEY_MONGO_DATABASE = "database";
}

