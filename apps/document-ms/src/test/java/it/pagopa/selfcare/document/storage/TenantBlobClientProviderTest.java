package it.pagopa.selfcare.document.storage;

import io.quarkus.runtime.StartupEvent;
import it.pagopa.selfcare.azurestorage.AzureBlobClient;
import it.pagopa.selfcare.document.exception.InvalidRequestException;
import it.pagopa.selfcare.document.model.StorageOrigin;
import it.pagopa.selfcare.tenant.StorageAuthenticationType;
import it.pagopa.selfcare.tenant.TenantContext;
import it.pagopa.selfcare.tenant.TenantDefinition;
import it.pagopa.selfcare.tenant.TenantRegistry;
import it.pagopa.selfcare.tenant.UnknownStorageException;
import it.pagopa.selfcare.tenant.UnknownTenantException;
import it.pagopa.selfcare.tenant.UnresolvedTenantException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static it.pagopa.selfcare.document.storage.TenantRegistryFixtures.binding;
import static it.pagopa.selfcare.document.storage.TenantRegistryFixtures.connectionString;
import static it.pagopa.selfcare.document.storage.TenantRegistryFixtures.managedIdentity;
import static it.pagopa.selfcare.document.storage.TenantRegistryFixtures.registry;
import static it.pagopa.selfcare.document.storage.TenantRegistryFixtures.tenant;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class TenantBlobClientProviderTest {

    private static final String AR_MI_ENV = "DMS05_PROVIDER_AR_MI_CLIENT_ID";
    private static final String PNPG_CS_ENV = "DMS05_PROVIDER_PNPG_CONNECTION_STRING";
    private static final String AR_MI_SECRET = "ar-managed-identity-secret-value";
    private static final String PNPG_CS_SECRET =
            "DefaultEndpointsProtocol=https;AccountName=pnpgdocs;AccountKey=cG5wZy1zZWNyZXQta2V5;EndpointSuffix=core.windows.net";

    private final TenantContext context = mock(TenantContext.class);

    @BeforeEach
    void defineSecrets() {
        System.setProperty(AR_MI_ENV, AR_MI_SECRET);
        System.setProperty(PNPG_CS_ENV, PNPG_CS_SECRET);
    }

    @AfterEach
    void clearSecrets() {
        System.clearProperty(AR_MI_ENV);
        System.clearProperty(PNPG_CS_ENV);
    }

    // ---- tenant isolation and client cache ----

    @Test
    void clientFor_shouldResolveDistinctAccountContainerAndPrefixPerTenant() throws Exception {
        RecordingProvider provider = new RecordingProvider(twoTenantRegistry(), context);

        AzureBlobClient arContracts = provider.clientFor("AR", StorageKeys.CONTRACTS);
        AzureBlobClient pnpgContracts = provider.clientFor("PNPG", StorageKeys.CONTRACTS);

        assertThat(provider.created).hasSize(2);
        Created ar = provider.createdFor("AR", StorageKeys.CONTRACTS);
        Created pnpg = provider.createdFor("PNPG", StorageKeys.CONTRACTS);
        assertThat(ar.storage().account()).isEqualTo("stardocs");
        assertThat(ar.storage().container()).isEqualTo("ar-documents");
        assertThat(ar.storage().authentication().type()).isEqualTo(StorageAuthenticationType.MANAGED_IDENTITY);
        assertThat(pnpg.storage().account()).isEqualTo("stpnpgdocs");
        assertThat(pnpg.storage().container()).isEqualTo("pnpg-documents");
        assertThat(pnpg.storage().authentication().type()).isEqualTo(StorageAuthenticationType.CONNECTION_STRING);
        assertThat(ar.client()).isNotSameAs(pnpg.client());

        arContracts.getFileAsPdf("docs/a.pdf");
        pnpgContracts.getFileAsPdf("docs/a.pdf");

        verify(ar.client()).getFileAsPdf("tenants/ar/docs/a.pdf");
        verify(pnpg.client()).getFileAsPdf("tenants/pnpg/docs/a.pdf");
    }

    @Test
    void clientFor_shouldNeverReachAnotherTenantClientFromTheSameLogicalPath() throws Exception {
        RecordingProvider provider = new RecordingProvider(twoTenantRegistry(), context);

        AzureBlobClient ar = provider.clientFor("AR", StorageKeys.CONTRACTS);
        provider.clientFor("PNPG", StorageKeys.CONTRACTS);

        ar.uploadFilePath("docs/a.pdf", new byte[]{1});
        ar.getFile("docs/a.pdf");

        verifyNoInteractions(provider.createdFor("PNPG", StorageKeys.CONTRACTS).client());
    }

    @Test
    void clientFor_shouldResolveDistinctBindingsPerLogicalKeyOfTheSameTenant() throws Exception {
        RecordingProvider provider = new RecordingProvider(twoTenantRegistry(), context);

        provider.clientFor("AR", StorageKeys.CONTRACTS).getFile("a.pdf");
        provider.clientFor("AR", StorageKeys.USER_ATTACHMENTS).getFile("a.pdf");

        Created contracts = provider.createdFor("AR", StorageKeys.CONTRACTS);
        Created userAttachments = provider.createdFor("AR", StorageKeys.USER_ATTACHMENTS);
        assertThat(contracts.storage().container()).isEqualTo("ar-documents");
        assertThat(userAttachments.storage().container()).isEqualTo("ar-attachments");
        verify(contracts.client()).getFile("tenants/ar/a.pdf");
        verify(userAttachments.client()).getFile("a.pdf");
    }

    @Test
    void clientFor_shouldCreateEachUnderlyingClientOnce() throws Exception {
        RecordingProvider provider = new RecordingProvider(twoTenantRegistry(), context);

        provider.clientFor("AR", StorageKeys.CONTRACTS);
        provider.clientFor("AR", StorageKeys.CONTRACTS);
        provider.clientFor("ar", "CONTRACTS");

        assertThat(provider.created).hasSize(1);
    }

    @Test
    void clientFor_shouldShareTransportButNotPathPrefixBetweenTenantsOnTheSameContainer() throws Exception {
        String shared = binding("stshared", "shared-documents", "tenants/ar", managedIdentity(AR_MI_ENV));
        String sharedPnpg = binding("stshared", "shared-documents", "tenants/pnpg", managedIdentity(AR_MI_ENV));
        TenantRegistry registry = registry(
                "{\"AR\":" + tenant(shared, shared) + ",\"PNPG\":" + tenant(sharedPnpg, sharedPnpg) + "}",
                "AR,PNPG",
                "contracts,user-attachments");
        RecordingProvider provider = new RecordingProvider(registry, context);

        AzureBlobClient ar = provider.clientFor("AR", StorageKeys.CONTRACTS);
        AzureBlobClient pnpg = provider.clientFor("PNPG", StorageKeys.CONTRACTS);
        ar.uploadFilePath("docs/a.pdf", new byte[]{1});
        pnpg.uploadFilePath("docs/a.pdf", new byte[]{2});

        assertThat(provider.created).hasSize(1);
        AzureBlobClient underlying = provider.created.get(0).client();
        verify(underlying).uploadFilePath("tenants/ar/docs/a.pdf", new byte[]{1});
        verify(underlying).uploadFilePath("tenants/pnpg/docs/a.pdf", new byte[]{2});
        assertThatThrownBy(() -> pnpg.getFile("../ar/docs/a.pdf")).isInstanceOf(InvalidRequestException.class);
        assertThatThrownBy(() -> pnpg.getFile("/tenants/ar/docs/a.pdf")).isInstanceOf(InvalidRequestException.class);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("bindingsThatMustNotShareAClient")
    void clientFor_shouldKeepClientsSeparateWhenAccountContainerOrCredentialDiffer(
            String description, String arBinding, String pnpgBinding) throws Exception {
        TenantRegistry registry = registry(
                "{\"AR\":" + tenant(arBinding, arBinding) + ",\"PNPG\":" + tenant(pnpgBinding, pnpgBinding) + "}",
                "AR,PNPG",
                "contracts,user-attachments");
        RecordingProvider provider = new RecordingProvider(registry, context);

        provider.clientFor("AR", StorageKeys.CONTRACTS).getFile("a.pdf");
        provider.clientFor("PNPG", StorageKeys.CONTRACTS).getFile("a.pdf");

        assertThat(provider.created).hasSize(2);
        assertThat(provider.createdFor("AR", StorageKeys.CONTRACTS).client())
                .isNotSameAs(provider.createdFor("PNPG", StorageKeys.CONTRACTS).client());
    }

    static Stream<Arguments> bindingsThatMustNotShareAClient() {
        String mi = managedIdentity(AR_MI_ENV);
        return Stream.of(
                Arguments.of("different account",
                        binding("staccounta", "documents", "", mi),
                        binding("staccountb", "documents", "", mi)),
                Arguments.of("different container",
                        binding("stsame", "container-a", "", mi),
                        binding("stsame", "container-b", "", mi)),
                Arguments.of("different managed identity",
                        binding("stsame", "documents", "", managedIdentity(AR_MI_ENV)),
                        binding("stsame", "documents", "", managedIdentity(null))),
                Arguments.of("managed identity vs connection string",
                        binding("stsame", "documents", "", mi),
                        binding("stsame", "documents", "", connectionString(PNPG_CS_ENV))));
    }

    @Test
    void clientFor_shouldCreateOneClientPerBindingUnderConcurrentAccess() throws Exception {
        RecordingProvider provider = new RecordingProvider(twoTenantRegistry(), context);
        int threads = 16;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        try {
            List<Future<?>> futures = new ArrayList<>();
            for (int i = 0; i < threads; i++) {
                String tenantId = i % 2 == 0 ? "AR" : "PNPG";
                futures.add(executor.submit(() -> {
                    start.await();
                    provider.clientFor(tenantId, StorageKeys.CONTRACTS);
                    return null;
                }));
            }
            start.countDown();
            for (Future<?> future : futures) {
                future.get();
            }
        } finally {
            executor.shutdownNow();
        }

        assertThat(provider.created).hasSize(2);
    }

    @Test
    void clientFor_shouldNotCacheAFailedCreation() throws Exception {
        List<Integer> attempts = new CopyOnWriteArrayList<>();
        AzureBlobClient healthy = mock(AzureBlobClient.class);
        TenantBlobClientProvider provider = new TenantBlobClientProvider(twoTenantRegistry(), context) {
            @Override
            protected AzureBlobClient createClient(
                    String tenantId, String logicalStorageKey, TenantDefinition.StorageDefinition storage) {
                attempts.add(attempts.size());
                if (attempts.size() == 1) {
                    throw new IllegalStateException("storage temporarily unavailable");
                }
                return healthy;
            }
        };

        assertThatThrownBy(() -> provider.clientFor("AR", StorageKeys.CONTRACTS))
                .isInstanceOf(IllegalStateException.class);
        provider.clientFor("AR", StorageKeys.CONTRACTS).getFile("a.pdf");

        assertThat(attempts).hasSize(2);
        verify(healthy).getFile("tenants/ar/a.pdf");
    }

    // ---- routing by current tenant and storage origin ----

    @Test
    void clientForCurrentTenant_shouldFollowTheTenantContext() throws Exception {
        RecordingProvider provider = new RecordingProvider(twoTenantRegistry(), context);
        when(context.requiredTenantId()).thenReturn("AR", "PNPG");

        provider.clientForCurrentTenant(StorageKeys.CONTRACTS).getFile("a.pdf");
        provider.clientForCurrentTenant(StorageKeys.CONTRACTS).getFile("a.pdf");

        verify(provider.createdFor("AR", StorageKeys.CONTRACTS).client()).getFile("tenants/ar/a.pdf");
        verify(provider.createdFor("PNPG", StorageKeys.CONTRACTS).client()).getFile("tenants/pnpg/a.pdf");
    }

    @Test
    void clientForCurrentTenant_shouldRouteStorageOriginToTheMatchingBinding() throws Exception {
        RecordingProvider provider = new RecordingProvider(twoTenantRegistry(), context);
        when(context.requiredTenantId()).thenReturn("AR");

        provider.clientForCurrentTenant(StorageOrigin.SYSTEM).getFile("system.pdf");
        provider.clientForCurrentTenant(StorageOrigin.USER).getFile("user.pdf");
        provider.clientForCurrentTenant((StorageOrigin) null).getFile("legacy.pdf");

        AzureBlobClient contracts = provider.createdFor("AR", StorageKeys.CONTRACTS).client();
        AzureBlobClient userAttachments = provider.createdFor("AR", StorageKeys.USER_ATTACHMENTS).client();
        verify(contracts).getFile("tenants/ar/system.pdf");
        verify(contracts).getFile("tenants/ar/legacy.pdf");
        verify(userAttachments).getFile("user.pdf");
    }

    @Test
    void clientForCurrentTenant_shouldFailClosedWithoutTenantContext() throws Exception {
        RecordingProvider provider = new RecordingProvider(twoTenantRegistry(), context);
        when(context.requiredTenantId()).thenThrow(new UnresolvedTenantException());

        assertThatThrownBy(() -> provider.clientForCurrentTenant(StorageKeys.CONTRACTS))
                .isInstanceOf(UnresolvedTenantException.class);
        assertThatThrownBy(() -> provider.clientForCurrentTenant(StorageOrigin.USER))
                .isInstanceOf(UnresolvedTenantException.class);
        assertThat(provider.created).isEmpty();
    }

    // ---- unknown tenant / key ----

    @Test
    void clientFor_shouldRejectUnknownStorageKeyWithoutCreatingAClient() throws Exception {
        RecordingProvider provider = new RecordingProvider(twoTenantRegistry(), context);

        assertThatThrownBy(() -> provider.clientFor("AR", "unknown"))
                .isInstanceOf(UnknownStorageException.class)
                .hasMessage("Unknown storage 'unknown' for tenant AR");
        assertThatThrownBy(() -> provider.clientFor("AR", "templates"))
                .isInstanceOf(UnknownStorageException.class);
        assertThat(provider.created).isEmpty();
    }

    @Test
    void clientFor_shouldRejectTenantThatIsNotSupported() throws Exception {
        TenantRegistry registry = registry(
                "{\"AR\":" + arOnlyTenant() + ",\"PNPG\":" + arOnlyTenant() + "}", "AR", "contracts,user-attachments");
        RecordingProvider provider = new RecordingProvider(registry, context);

        assertThatThrownBy(() -> provider.clientFor("PNPG", StorageKeys.CONTRACTS))
                .isInstanceOf(UnknownTenantException.class);
        assertThatThrownBy(() -> provider.clientFor("OTHER", StorageKeys.CONTRACTS))
                .isInstanceOf(UnknownTenantException.class);
        assertThat(provider.created).isEmpty();
    }

    // ---- eager initialization ----

    @Test
    void initialize_shouldCreateEveryMandatoryBindingOfEverySupportedTenant() throws Exception {
        RecordingProvider provider = new RecordingProvider(twoTenantRegistry(), context);
        provider.eagerInit = true;

        provider.initialize();

        assertThat(provider.created).hasSize(4);
        assertThat(provider.created)
                .extracting(created -> created.tenantId() + ":" + created.logicalKey())
                .containsExactlyInAnyOrder(
                        "AR:contracts", "AR:user-attachments", "PNPG:contracts", "PNPG:user-attachments");
    }

    @Test
    void initialize_shouldDoNothingWhenEagerInitIsDisabled() throws Exception {
        RecordingProvider provider = new RecordingProvider(twoTenantRegistry(), context);
        provider.eagerInit = false;

        provider.initialize();

        assertThat(provider.created).isEmpty();
    }

    @Test
    void initialize_shouldFailStartupWhenAClientCannotBeCreated() throws Exception {
        TenantBlobClientProvider provider = new TenantBlobClientProvider(twoTenantRegistry(), context) {
            @Override
            protected AzureBlobClient createClient(
                    String tenantId, String logicalStorageKey, TenantDefinition.StorageDefinition storage) {
                throw new IllegalStateException("Missing storage connection string for tenant " + tenantId);
            }
        };
        provider.eagerInit = true;

        assertThatThrownBy(provider::initialize).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void initialize_shouldFailStartupWhenARegistryPathPrefixEscapesTheContainer() throws Exception {
        String traversal = binding("stardocs", "ar-documents", "../other-tenant", managedIdentity(null));
        String ok = binding("starattach", "ar-attachments", "", managedIdentity(null));
        RecordingProvider provider = new RecordingProvider(
                registry("{\"AR\":" + tenant(traversal, ok) + "}", "AR", "contracts,user-attachments"), context);
        provider.eagerInit = true;

        assertThatThrownBy(provider::initialize).isInstanceOf(InvalidRequestException.class);
    }

    @Test
    void startupInitializer_shouldInitializeTheProviderWhenTheApplicationStarts() throws Exception {
        RecordingProvider provider = new RecordingProvider(twoTenantRegistry(), context);
        provider.eagerInit = true;

        new TenantBlobClientsStartupInitializer().onStart(new StartupEvent(), provider);

        assertThat(provider.created).hasSize(4);
    }

    @Test
    void startupInitializer_shouldCreateNothingWhenEagerInitIsDisabled() throws Exception {
        RecordingProvider provider = new RecordingProvider(twoTenantRegistry(), context);
        provider.eagerInit = false;

        new TenantBlobClientsStartupInitializer().onStart(new StartupEvent(), provider);

        assertThat(provider.created).isEmpty();
    }

    @Test
    void startupInitializer_shouldFailStartupWhenARegistryPathPrefixEscapesTheContainer() throws Exception {
        String traversal = binding("stardocs", "ar-documents", "../other-tenant", managedIdentity(null));
        String ok = binding("starattach", "ar-attachments", "", managedIdentity(null));
        RecordingProvider provider = new RecordingProvider(
                registry("{\"AR\":" + tenant(traversal, ok) + "}", "AR", "contracts,user-attachments"), context);
        provider.eagerInit = true;

        assertThatThrownBy(() -> new TenantBlobClientsStartupInitializer().onStart(new StartupEvent(), provider))
                .isInstanceOf(InvalidRequestException.class);
    }

    // ---- paths seen through the provider-returned client ----

    @Test
    void clientFor_shouldApplyTheRegistryPathPrefixToEveryOperation() throws Exception {
        RecordingProvider provider = new RecordingProvider(twoTenantRegistry(), context);
        AzureBlobClient client = provider.clientFor("AR", StorageKeys.CONTRACTS);
        AzureBlobClient delegate = provider.createdFor("AR", StorageKeys.CONTRACTS).client();
        when(delegate.uploadFilePath("tenants/ar/docs/a.pdf", new byte[]{1})).thenReturn("tenants/ar/docs/a.pdf");
        when(delegate.uploadFile("tenants/ar/docs", "b.pdf", new byte[]{2})).thenReturn("tenants/ar/docs/b.pdf");
        when(delegate.getFiles("tenants/ar/docs")).thenReturn(List.of("tenants/ar/docs/a.pdf"));

        client.retrieveFile("docs/a.pdf");
        client.getFile("docs/a.pdf");
        client.getFileAsText("docs/a.html");
        client.getFileAsPdf("docs/a.pdf");
        client.getProperties("docs/a.pdf");
        client.removeFile("docs/a.pdf");

        assertThat(client.uploadFilePath("docs/a.pdf", new byte[]{1})).isEqualTo("docs/a.pdf");
        assertThat(client.uploadFile("docs", "b.pdf", new byte[]{2})).isEqualTo("docs/b.pdf");
        assertThat(client.getFiles("docs")).containsExactly("docs/a.pdf");

        verify(delegate).retrieveFile("tenants/ar/docs/a.pdf");
        verify(delegate).getFile("tenants/ar/docs/a.pdf");
        verify(delegate).getFileAsText("tenants/ar/docs/a.html");
        verify(delegate).getFileAsPdf("tenants/ar/docs/a.pdf");
        verify(delegate).getProperties("tenants/ar/docs/a.pdf");
        verify(delegate).removeFile("tenants/ar/docs/a.pdf");
    }

    @Test
    void clientFor_shouldNotPrefixPathsWhenTheBindingHasNoPathPrefix() throws Exception {
        RecordingProvider provider = new RecordingProvider(twoTenantRegistry(), context);

        provider.clientFor("AR", StorageKeys.USER_ATTACHMENTS).getFile("docs//./a.pdf");

        verify(provider.createdFor("AR", StorageKeys.USER_ATTACHMENTS).client()).getFile("docs/a.pdf");
    }

    @Test
    void clientFor_shouldNormalizeTheConfiguredPathPrefix() throws Exception {
        String prefixed = binding("stardocs", "ar-documents", "tenants//ar/", managedIdentity(null));
        String ok = binding("starattach", "ar-attachments", "", managedIdentity(null));
        RecordingProvider provider = new RecordingProvider(
                registry("{\"AR\":" + tenant(prefixed, ok) + "}", "AR", "contracts,user-attachments"), context);

        provider.clientFor("AR", StorageKeys.CONTRACTS).getFile("a.pdf");

        verify(provider.createdFor("AR", StorageKeys.CONTRACTS).client()).getFile("tenants/ar/a.pdf");
    }

    @ParameterizedTest(name = "[{index}] rejects {0}")
    @MethodSource("maliciousPaths")
    void clientFor_shouldRejectMaliciousPathsOnEveryOperationBeforeReachingStorage(String path) throws Exception {
        RecordingProvider provider = new RecordingProvider(twoTenantRegistry(), context);

        for (String key : List.of(StorageKeys.CONTRACTS, StorageKeys.USER_ATTACHMENTS)) {
            AzureBlobClient client = provider.clientFor("AR", key);
            AzureBlobClient delegate = provider.createdFor("AR", key).client();

            for (Map.Entry<String, Consumer<String>> operation : operations(client).entrySet()) {
                assertThatThrownBy(() -> operation.getValue().accept(path))
                        .as("%s on %s with path %s", operation.getKey(), key, path)
                        .isInstanceOf(InvalidRequestException.class);
            }
            verifyNoInteractions(delegate);
        }
    }

    static Stream<String> maliciousPaths() {
        return Stream.of(
                "..",
                "../other",
                "docs/../../other",
                "docs/..",
                "..\\other",
                "docs\\..\\..\\other",
                "/etc/passwd",
                "//server/share",
                "\\windows\\system32",
                "C:/windows",
                "docs/a\u0000.pdf",
                "docs/a\n.pdf",
                "docs/a\r.pdf",
                "docs/a\t.pdf",
                "docs/a\u007f.pdf");
    }

    @Test
    void clientFor_shouldRejectNullPathsOnEveryOperation() throws Exception {
        RecordingProvider provider = new RecordingProvider(twoTenantRegistry(), context);
        AzureBlobClient client = provider.clientFor("AR", StorageKeys.CONTRACTS);

        for (Map.Entry<String, Consumer<String>> operation : operations(client).entrySet()) {
            assertThatThrownBy(() -> operation.getValue().accept(null))
                    .as(operation.getKey())
                    .isInstanceOf(InvalidRequestException.class);
        }
        verifyNoInteractions(provider.createdFor("AR", StorageKeys.CONTRACTS).client());
    }

    @ParameterizedTest
    @ValueSource(strings = {"..hidden/a.pdf", "docs/a..b.pdf", "docs/.../a.pdf", "docs/%2e%2e/a.pdf"})
    void clientFor_shouldAcceptNamesThatOnlyContainDots(String path) throws Exception {
        RecordingProvider provider = new RecordingProvider(twoTenantRegistry(), context);

        provider.clientFor("AR", StorageKeys.USER_ATTACHMENTS).getFile(path);

        verify(provider.createdFor("AR", StorageKeys.USER_ATTACHMENTS).client()).getFile(path);
    }

    @Test
    void clientFor_shouldListOnlyBlobsOfTheTenantPrefix() throws Exception {
        RecordingProvider provider = new RecordingProvider(twoTenantRegistry(), context);
        AzureBlobClient client = provider.clientFor("AR", StorageKeys.CONTRACTS);
        AzureBlobClient delegate = provider.createdFor("AR", StorageKeys.CONTRACTS).client();
        when(delegate.getFiles(anyString())).thenReturn(List.of("tenants/ar/a.pdf", "tenants/ar2/leak.pdf"));

        assertThat(client.getFiles()).containsExactly("a.pdf");
        assertThat(client.getFiles("")).containsExactly("a.pdf");
        verify(delegate, times(2)).getFiles("tenants/ar/");
    }

    // ---- secrets ----

    @Test
    void createClient_shouldFailWithoutLeakingSecretsWhenTheConnectionStringIsNoLongerAvailable() {
        TenantRegistry registry = mock(TenantRegistry.class);
        TenantDefinition.StorageDefinition storage = new TenantDefinition.StorageDefinition(
                "stpnpgdocs",
                "pnpg-documents",
                "",
                new TenantDefinition.StorageAuthentication(
                        StorageAuthenticationType.CONNECTION_STRING, null, PNPG_CS_ENV));
        when(registry.storage("PNPG", StorageKeys.CONTRACTS)).thenReturn(storage);
        when(registry.storageConnectionString("PNPG", StorageKeys.CONTRACTS)).thenReturn(Optional.empty());
        TenantBlobClientProvider provider = new TenantBlobClientProvider(registry, context);

        assertThatThrownBy(() -> provider.clientFor("PNPG", StorageKeys.CONTRACTS))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Missing storage connection string for tenant PNPG and key contracts")
                .hasMessageNotContaining(PNPG_CS_SECRET);
    }

    @Test
    void clients_shouldNeverExposeCredentialsInToString() throws Exception {
        RecordingProvider provider = new RecordingProvider(twoTenantRegistry(), context);

        for (String tenantId : Set.of("AR", "PNPG")) {
            assertThat(provider.clientFor(tenantId, StorageKeys.CONTRACTS).toString())
                    .doesNotContain(AR_MI_SECRET, PNPG_CS_SECRET, "AccountKey");
            assertThat(provider.createdFor(tenantId, StorageKeys.CONTRACTS).storage().toString())
                    .doesNotContain(AR_MI_SECRET, PNPG_CS_SECRET, "AccountKey");
        }
    }

    // ---- helpers ----

    private static Map<String, Consumer<String>> operations(AzureBlobClient client) {
        Map<String, Consumer<String>> operations = new LinkedHashMap<>();
        operations.put("retrieveFile", client::retrieveFile);
        operations.put("getFile", client::getFile);
        operations.put("getFileAsText", client::getFileAsText);
        operations.put("getFileAsPdf", client::getFileAsPdf);
        operations.put("getProperties", client::getProperties);
        operations.put("removeFile", client::removeFile);
        operations.put("uploadFilePath", path -> client.uploadFilePath(path, new byte[]{1}));
        operations.put("uploadFile(path)", path -> client.uploadFile(path, "file.pdf", new byte[]{1}));
        operations.put("uploadFile(filename)", path -> client.uploadFile("docs", path, new byte[]{1}));
        operations.put("getFiles(path)", client::getFiles);
        return operations;
    }

    private TenantRegistry twoTenantRegistry() throws Exception {
        String ar = tenant(
                binding("stardocs", "ar-documents", "tenants/ar", managedIdentity(AR_MI_ENV)),
                binding("starattach", "ar-attachments", "", managedIdentity(AR_MI_ENV)));
        String pnpg = tenant(
                binding("stpnpgdocs", "pnpg-documents", "tenants/pnpg", connectionString(PNPG_CS_ENV)),
                binding("stpnpgattach", "pnpg-attachments", "", connectionString(PNPG_CS_ENV)));
        return registry("{\"AR\":" + ar + ",\"PNPG\":" + pnpg + "}", "AR,PNPG", "contracts,user-attachments");
    }

    private static String arOnlyTenant() {
        return tenant(
                binding("stardocs", "ar-documents", "", managedIdentity(null)),
                binding("starattach", "ar-attachments", "", managedIdentity(null)));
    }

    private record Created(
            String tenantId,
            String logicalKey,
            TenantDefinition.StorageDefinition storage,
            AzureBlobClient client) {
    }

    private static final class RecordingProvider extends TenantBlobClientProvider {

        final List<Created> created = new CopyOnWriteArrayList<>();

        RecordingProvider(TenantRegistry registry, TenantContext context) {
            super(registry, context);
            this.eagerInit = false;
        }

        @Override
        protected AzureBlobClient createClient(
                String tenantId, String logicalStorageKey, TenantDefinition.StorageDefinition storage) {
            AzureBlobClient client = mock(AzureBlobClient.class);
            created.add(new Created(tenantId, logicalStorageKey, storage, client));
            return client;
        }

        Created createdFor(String tenantId, String logicalKey) {
            return created.stream()
                    .filter(c -> c.tenantId().equalsIgnoreCase(tenantId) && c.logicalKey().equalsIgnoreCase(logicalKey))
                    .findFirst()
                    .orElseThrow(() -> new AssertionError("No client created for " + tenantId + ":" + logicalKey));
        }
    }
}
