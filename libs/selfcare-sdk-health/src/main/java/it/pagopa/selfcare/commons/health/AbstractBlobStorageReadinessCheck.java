package it.pagopa.selfcare.commons.health;

import java.util.HashMap;
import java.util.Map;

/**
 * Base class for readiness checks targeting an <b>Azure Blob Storage</b> container.
 *
 * <p>The check should perform a lightweight operation (typically a {@code getProperties()} /
 * HEAD-equivalent) against a stable <i>canary blob</i> that must always exist in the container.
 * The purpose is to detect:
 * <ul>
 *   <li>network partitions,</li>
 *   <li>DNS misconfiguration,</li>
 *   <li>Managed Identity / RBAC misalignment (token obtained successfully but no permissions on
 *       the storage account the application actually reads from).</li>
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
 *     @Override protected String checkName()  { return "blob-storage-product"; }
 *     @Override protected String account()    { return account; }
 *     @Override protected String container()  { return container; }
 *     @Override protected String canaryBlob() { return "products.json"; }
 *
 *     @Override
 *     protected Uni<?> probe() {
 *         return Uni.createFrom().item(() -> client.getProperties(container(), canaryBlob()))
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

    /** Path of the canary blob (must exist in the container). */
    protected abstract String canaryBlob();

    @Override
    protected Map<String, String> data() {
        final Map<String, String> data = new HashMap<>(4);
        data.put(HealthCheckConstants.DATA_KEY_COMPONENT, "blob-storage");
        data.put(HealthCheckConstants.DATA_KEY_BLOB_ACCOUNT, account());
        data.put(HealthCheckConstants.DATA_KEY_BLOB_CONTAINER, container());
        data.put(HealthCheckConstants.DATA_KEY_BLOB_CANARY, canaryBlob());
        return data;
    }
}

