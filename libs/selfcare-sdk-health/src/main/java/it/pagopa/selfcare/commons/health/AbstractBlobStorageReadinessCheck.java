package it.pagopa.selfcare.commons.health;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Base class for readiness checks targeting an <b>Azure Blob Storage</b> container.
 *
 * <p>The check should perform a lightweight operation (a {@code getProperties()} HEAD-equivalent
 * on a specific stable blob, or a list with an unlikely-to-exist prefix returning an empty page)
 * that verifies:
 * <ul>
 *   <li>network reachability,</li>
 *   <li>DNS resolution,</li>
 *   <li>Managed Identity / RBAC alignment (token obtained AND permissions valid on the storage
 *       account the application actually reads from).</li>
 * </ul>
 *
 * <p>This base class is intentionally decoupled from the Azure SDK types so consumers can pick
 * their preferred client (blocking {@code BlobServiceClient} wrapped with
 * {@code Uni.createFrom().item(...).runSubscriptionOn(...)}, reactive
 * {@code BlobAsyncClient}, or the Selfcare {@code AzureBlobClient} wrapper).
 *
 * <p>Example (using the Selfcare {@code AzureBlobClient} wrapper):
 * <pre>{@code
 * @Readiness
 * @ApplicationScoped
 * public class ProductBlobReadinessCheck extends AbstractBlobStorageReadinessCheck {
 *
 *     @Inject AzureBlobClient client;
 *
 *     @ConfigProperty(name = "onboarding-ms.blob-storage.account-name-product") String account;
 *     @ConfigProperty(name = "onboarding-ms.blob-storage.container-product")    String container;
 *
 *     @Override protected String checkName()   { return "blob-storage-product"; }
 *     @Override protected String account()     { return account; }
 *     @Override protected String container()   { return container; }
 *     @Override protected String probeTarget() { return "products.json"; }
 *
 *     @Override
 *     protected Uni<?> probe() {
 *         return Uni.createFrom().item(() -> client.getProperties(container(), probeTarget()))
 *                 .runSubscriptionOn(Infrastructure.getDefaultWorkerPool());
 *     }
 * }
 * }</pre>
 */
public abstract class AbstractBlobStorageReadinessCheck extends AbstractAsyncReadinessCheck {

    /** Storage account name. Surfaced in the response data for troubleshooting. */
    protected abstract String account();

    /** Container name that the probe targets. */
    protected abstract String container();

    /**
     * What the probe is actually targeting: a specific blob path (e.g. {@code "products.json"}),
     * an unlikely-to-exist prefix used as a marker for list-based probes
     * (e.g. {@code "__healthcheck_probe__/"}), or any other identifier chosen by the subclass.
     *
     * <p>Default is an empty string, in which case the {@code probeTarget} key is <b>not</b>
     * added to the payload &mdash; useful for container-level probes that have no specific target.
     */
    protected String probeTarget() {
        return "";
    }

    @Override
    protected Map<String, String> data() {
        final Map<String, String> data = new HashMap<>(4);
        data.put(HealthCheckConstants.DATA_KEY_COMPONENT, "blob-storage");
        data.put(HealthCheckConstants.DATA_KEY_BLOB_ACCOUNT, account());
        data.put(HealthCheckConstants.DATA_KEY_BLOB_CONTAINER, container());
        final String pt = probeTarget();
        if (Objects.nonNull(pt) && !pt.isBlank()) {
            data.put(HealthCheckConstants.DATA_KEY_BLOB_PROBE_TARGET, pt);
        }
        return data;
    }
}
