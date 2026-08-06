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

    /**
     * Data key: what the Blob Storage probe is actually targeting — a specific blob path,
     * an unlikely-to-exist prefix used as a marker for list-based probes, or any other
     * identifier chosen by the subclass. Only included in the payload when the check reports
     * a non-blank value via {@code AbstractBlobStorageReadinessCheck.probeTarget()}.
     */
    public static final String DATA_KEY_BLOB_PROBE_TARGET = "probeTarget";

    /** Data key: MongoDB database name being pinged. */
    public static final String DATA_KEY_MONGO_DATABASE = "database";

    /**
     * Data key: MongoDB host(s) the pod is connected to (comma-joined {@code host:port} pairs).
     * Only included in the payload when the check reports a non-blank value via
     * {@link AbstractMongoReadinessCheck#host()}.
     */
    public static final String DATA_KEY_MONGO_HOST = "host";
}

