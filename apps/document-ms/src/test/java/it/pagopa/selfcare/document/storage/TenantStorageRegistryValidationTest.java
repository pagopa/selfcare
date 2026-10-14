package it.pagopa.selfcare.document.storage;

import it.pagopa.selfcare.tenant.StorageAuthenticationType;
import it.pagopa.selfcare.tenant.TenantDefinition;
import it.pagopa.selfcare.tenant.TenantRegistry;
import it.pagopa.selfcare.tenant.UnknownStorageException;
import it.pagopa.selfcare.tenant.UnknownTenantException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static it.pagopa.selfcare.document.storage.TenantRegistryFixtures.binding;
import static it.pagopa.selfcare.document.storage.TenantRegistryFixtures.connectionString;
import static it.pagopa.selfcare.document.storage.TenantRegistryFixtures.managedIdentity;
import static it.pagopa.selfcare.document.storage.TenantRegistryFixtures.registry;
import static it.pagopa.selfcare.document.storage.TenantRegistryFixtures.tenant;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Startup validation of the {@code storages} section of the tenant registry, with the mandatory keys configured
 * for document-ms ({@code contracts}, {@code user-attachments}).
 */
class TenantStorageRegistryValidationTest {

    private static final String MANDATORY_KEYS = "contracts,user-attachments";
    private static final String MI_ENV = "DMS05_VALIDATION_MI_CLIENT_ID";
    private static final String CS_ENV = "DMS05_VALIDATION_CONNECTION_STRING";
    private static final String MISSING_ENV = "DMS05_VALIDATION_MISSING_ENV";
    private static final String MI_SECRET = "mi-client-id-secret-value";
    private static final String CS_SECRET = "AccountName=acc;AccountKey=connection-string-secret-value";

    private final List<String> properties = new ArrayList<>();

    @AfterEach
    void clearProperties() {
        properties.forEach(System::clearProperty);
    }

    @Test
    void initialize_shouldAcceptManagedIdentityAndConnectionStringBindings() throws Exception {
        define(MI_ENV, MI_SECRET);
        define(CS_ENV, CS_SECRET);

        TenantRegistry registry = registry(
                "{\"AR\":" + tenant(
                        binding("stdocs", "documents", "", managedIdentity(MI_ENV)),
                        binding("stattach", "attachments", "ar", connectionString(CS_ENV))) + "}",
                "AR",
                MANDATORY_KEYS);

        TenantDefinition.StorageDefinition contracts = registry.storage("AR", StorageKeys.CONTRACTS);
        TenantDefinition.StorageDefinition userAttachments = registry.storage("AR", StorageKeys.USER_ATTACHMENTS);
        assertThat(contracts.authentication().type()).isEqualTo(StorageAuthenticationType.MANAGED_IDENTITY);
        assertThat(userAttachments.authentication().type()).isEqualTo(StorageAuthenticationType.CONNECTION_STRING);
        assertThat(registry.storageManagedIdentityClientId("AR", StorageKeys.CONTRACTS)).contains(MI_SECRET);
        assertThat(registry.storageConnectionString("AR", StorageKeys.USER_ATTACHMENTS)).contains(CS_SECRET);
        assertThat(userAttachments.pathPrefix()).isEqualTo("ar");
    }

    @Test
    void initialize_shouldAcceptManagedIdentityWithoutClientIdReference() throws Exception {
        TenantRegistry registry = registry(
                "{\"AR\":" + tenant(
                        binding("stdocs", "documents", "", managedIdentity(null)),
                        binding("stattach", "attachments", "", managedIdentity(null))) + "}",
                "AR",
                MANDATORY_KEYS);

        assertThat(registry.storageManagedIdentityClientId("AR", StorageKeys.CONTRACTS)).isEmpty();
    }

    @Test
    void storage_shouldResolveLogicalKeyCaseInsensitively() throws Exception {
        TenantRegistry registry = validRegistry();

        assertThat(registry.storage("ar", "CONTRACTS")).isSameAs(registry.storage("AR", "contracts"));
    }

    @Test
    void initialize_shouldRejectManagedIdentityTogetherWithConnectionString() {
        define(MI_ENV, MI_SECRET);
        define(CS_ENV, CS_SECRET);
        String both = "{\"type\":\"MANAGED_IDENTITY\",\"managedIdentityClientIdEnvVar\":\"" + MI_ENV
                + "\",\"connectionStringEnvVar\":\"" + CS_ENV + "\"}";

        assertThatThrownBy(() -> registry(arWithContracts(binding("docs", "documents", "", both)), "AR", MANDATORY_KEYS))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Invalid MANAGED_IDENTITY storage authentication for tenant AR and key contracts");
    }

    @Test
    void initialize_shouldRejectManagedIdentityTypeWithOnlyConnectionStringReference() {
        define(CS_ENV, CS_SECRET);
        String auth = "{\"type\":\"MANAGED_IDENTITY\",\"connectionStringEnvVar\":\"" + CS_ENV + "\"}";

        assertThatThrownBy(() -> registry(arWithContracts(binding("docs", "documents", "", auth)), "AR", MANDATORY_KEYS))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Invalid MANAGED_IDENTITY storage authentication");
    }

    @Test
    void initialize_shouldRejectConnectionStringTogetherWithManagedIdentityClientId() {
        define(MI_ENV, MI_SECRET);
        define(CS_ENV, CS_SECRET);
        String both = "{\"type\":\"CONNECTION_STRING\",\"connectionStringEnvVar\":\"" + CS_ENV
                + "\",\"managedIdentityClientIdEnvVar\":\"" + MI_ENV + "\"}";

        assertThatThrownBy(() -> registry(arWithContracts(binding("docs", "documents", "", both)), "AR", MANDATORY_KEYS))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Invalid CONNECTION_STRING storage authentication for tenant AR and key contracts");
    }

    @Test
    void initialize_shouldRejectConnectionStringTypeWithoutConnectionStringReference() {
        assertThatThrownBy(() -> registry(
                arWithContracts(binding("docs", "documents", "", "{\"type\":\"CONNECTION_STRING\"}")),
                "AR",
                MANDATORY_KEYS))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Invalid CONNECTION_STRING storage authentication");
    }

    @Test
    void initialize_shouldRejectConnectionStringEnvVarThatIsNotSet() {
        assertThatThrownBy(() -> registry(
                arWithContracts(binding("docs", "documents", "", connectionString(MISSING_ENV))),
                "AR",
                MANDATORY_KEYS))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Missing storage connection string environment variable " + MISSING_ENV
                        + " for tenant AR and key contracts");
    }

    @Test
    void initialize_shouldRejectConnectionStringEnvVarThatIsBlank() {
        define(CS_ENV, "   ");

        assertThatThrownBy(() -> registry(
                arWithContracts(binding("docs", "documents", "", connectionString(CS_ENV))),
                "AR",
                MANDATORY_KEYS))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Missing storage connection string environment variable " + CS_ENV);
    }

    @Test
    void initialize_shouldRejectManagedIdentityClientIdEnvVarThatIsNotSet() {
        assertThatThrownBy(() -> registry(
                arWithContracts(binding("docs", "documents", "", managedIdentity(MISSING_ENV))),
                "AR",
                MANDATORY_KEYS))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Missing managed identity client id environment variable " + MISSING_ENV
                        + " for tenant AR and key contracts");
    }

    @Test
    void initialize_shouldRejectMissingMandatoryStorageKey() {
        assertThatThrownBy(() -> registry(
                "{\"AR\":{\"storages\":{\"contracts\":" + binding("docs", "documents", "", managedIdentity(null)) + "}}}",
                "AR",
                MANDATORY_KEYS))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Missing mandatory storage 'user-attachments' for tenant AR");
    }

    @Test
    void initialize_shouldRejectTenantWithoutStorages() {
        assertThatThrownBy(() -> registry("{\"AR\":{}}", "AR", MANDATORY_KEYS))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Missing mandatory storage");
    }

    @Test
    void initialize_shouldRequireMandatoryKeysOfEverySupportedTenant() {
        String ar = tenant(
                binding("ardocs", "documents", "", managedIdentity(null)),
                binding("arattach", "attachments", "", managedIdentity(null)));
        String pnpgWithoutUserAttachments = "{\"storages\":{\"contracts\":"
                + binding("pnpgdocs", "documents", "", managedIdentity(null)) + "}}";

        assertThatThrownBy(() -> registry(
                "{\"AR\":" + ar + ",\"PNPG\":" + pnpgWithoutUserAttachments + "}",
                "AR,PNPG",
                MANDATORY_KEYS))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Missing mandatory storage 'user-attachments' for tenant PNPG");
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("incompleteBindings")
    void initialize_shouldRejectIncompleteBinding(String description, String contractsBinding) {
        assertThatThrownBy(() -> registry(arWithContracts(contractsBinding), "AR", MANDATORY_KEYS))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Incomplete storage configuration for tenant AR and key contracts");
    }

    static Stream<Arguments> incompleteBindings() {
        return Stream.of(
                Arguments.of("blank account", binding("", "documents", "", managedIdentity(null))),
                Arguments.of("blank container", binding("docs", " ", "", managedIdentity(null))),
                Arguments.of("missing authentication", "{\"account\":\"docs\",\"container\":\"documents\"}"),
                Arguments.of("missing authentication type", binding("docs", "documents", "", "{}")));
    }

    @Test
    void initialize_shouldRejectNullBinding() {
        assertThatThrownBy(() -> registry(arWithContracts("null"), "AR", MANDATORY_KEYS))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Invalid tenant registry configuration");
    }

    @Test
    void initialize_shouldRejectUnknownAuthenticationType() {
        assertThatThrownBy(() -> registry(
                arWithContracts(binding("docs", "documents", "", "{\"type\":\"ACCOUNT_KEY\"}")),
                "AR",
                MANDATORY_KEYS))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Invalid tenant registry configuration");
    }

    @Test
    void initialize_shouldRejectLogicalKeysDuplicatedAfterNormalization() {
        String auth = managedIdentity(null);
        String duplicated = "{\"AR\":{\"storages\":{\"contracts\":" + binding("docs", "documents", "", auth)
                + ",\"Contracts\":" + binding("other", "documents", "", auth)
                + ",\"user-attachments\":" + binding("attach", "attachments", "", auth) + "}}}";

        assertThatThrownBy(() -> registry(duplicated, "AR", MANDATORY_KEYS))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Invalid tenant registry configuration");
    }

    @Test
    void storage_shouldRejectUnknownLogicalKey() throws Exception {
        TenantRegistry registry = validRegistry();

        assertThatThrownBy(() -> registry.storage("AR", "unknown"))
                .isInstanceOf(UnknownStorageException.class)
                .hasMessage("Unknown storage 'unknown' for tenant AR");
        assertThatThrownBy(() -> registry.storageConnectionString("AR", "unknown"))
                .isInstanceOf(UnknownStorageException.class);
        assertThatThrownBy(() -> registry.storageManagedIdentityClientId("AR", "unknown"))
                .isInstanceOf(UnknownStorageException.class);
    }

    @Test
    void storage_shouldRejectBlankLogicalKey() throws Exception {
        TenantRegistry registry = validRegistry();

        assertThatThrownBy(() -> registry.storage("AR", " "))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> registry.storage("AR", null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void storage_shouldRejectTenantThatIsNotSupported() throws Exception {
        TenantRegistry registry = validRegistry();

        assertThatThrownBy(() -> registry.storage("PNPG", StorageKeys.CONTRACTS))
                .isInstanceOf(UnknownTenantException.class);
    }

    @Test
    void storage_shouldNotFallBackToAnotherTenantBinding() throws Exception {
        String ar = tenant(
                binding("ardocs", "documents", "", managedIdentity(null)),
                binding("arattach", "attachments", "", managedIdentity(null)));
        String pnpgContractsOnly = "{\"storages\":{\"contracts\":"
                + binding("pnpgdocs", "documents", "", managedIdentity(null)) + "}}";
        TenantRegistry registry = registry(
                "{\"AR\":" + ar + ",\"PNPG\":" + pnpgContractsOnly + "}", "AR,PNPG", StorageKeys.CONTRACTS);

        assertThat(registry.storage("AR", StorageKeys.USER_ATTACHMENTS).account()).isEqualTo("arattach");
        assertThatThrownBy(() -> registry.storage("PNPG", StorageKeys.USER_ATTACHMENTS))
                .isInstanceOf(UnknownStorageException.class)
                .hasMessage("Unknown storage 'user-attachments' for tenant PNPG");
    }

    @Test
    void validation_shouldNeverExposeSecretValuesInExceptionsOrDefinitions() throws Exception {
        define(MI_ENV, MI_SECRET);
        define(CS_ENV, CS_SECRET);
        String bothMi = "{\"type\":\"MANAGED_IDENTITY\",\"managedIdentityClientIdEnvVar\":\"" + MI_ENV
                + "\",\"connectionStringEnvVar\":\"" + CS_ENV + "\"}";
        String bothCs = "{\"type\":\"CONNECTION_STRING\",\"connectionStringEnvVar\":\"" + CS_ENV
                + "\",\"managedIdentityClientIdEnvVar\":\"" + MI_ENV + "\"}";
        List<String> invalidRegistries = List.of(
                arWithContracts(binding("docs", "documents", "", bothMi)),
                arWithContracts(binding("docs", "documents", "", bothCs)),
                arWithContracts(binding("docs", "documents", "", connectionString(MISSING_ENV))),
                arWithContracts(binding("", "documents", "", managedIdentity(MI_ENV))));

        for (String json : invalidRegistries) {
            Throwable failure = null;
            try {
                registry(json, "AR", MANDATORY_KEYS);
            } catch (Exception e) {
                failure = e;
            }
            assertThat(failure).isNotNull();
            for (Throwable t = failure; t != null; t = t.getCause()) {
                assertThat(String.valueOf(t.getMessage())).doesNotContain(MI_SECRET, CS_SECRET);
            }
        }

        TenantRegistry valid = registry(
                "{\"AR\":" + tenant(
                        binding("docs", "documents", "", managedIdentity(MI_ENV)),
                        binding("attach", "attachments", "", connectionString(CS_ENV))) + "}",
                "AR",
                MANDATORY_KEYS);
        assertThat(valid.storage("AR", StorageKeys.CONTRACTS).toString()).doesNotContain(MI_SECRET, CS_SECRET);
        assertThat(valid.storage("AR", StorageKeys.USER_ATTACHMENTS).toString()).doesNotContain(MI_SECRET, CS_SECRET);
        assertThatThrownBy(() -> valid.storage("AR", "unknown"))
                .hasMessageNotContaining(MI_SECRET)
                .hasMessageNotContaining(CS_SECRET);
    }

    private TenantRegistry validRegistry() throws Exception {
        return registry(
                "{\"AR\":" + tenant(
                        binding("docs", "documents", "", managedIdentity(null)),
                        binding("attach", "attachments", "", managedIdentity(null))) + "}",
                "AR",
                MANDATORY_KEYS);
    }

    private static String arWithContracts(String contractsBinding) {
        return "{\"AR\":{\"storages\":{\"contracts\":" + contractsBinding
                + ",\"user-attachments\":" + binding("attach", "attachments", "", managedIdentity(null)) + "}}}";
    }

    private void define(String name, String value) {
        System.setProperty(name, value);
        properties.add(name);
    }
}
