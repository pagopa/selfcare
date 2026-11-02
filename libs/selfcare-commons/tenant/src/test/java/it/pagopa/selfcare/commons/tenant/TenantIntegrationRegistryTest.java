package it.pagopa.selfcare.commons.tenant;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.env.MockEnvironment;

import static org.junit.jupiter.api.Assertions.*;

class TenantIntegrationRegistryTest {

    private static final String REGISTRY = """
            {
              "AR": {
                "oneMail": {"url":"https://mail.ar.test","sender":"mail@ar.test","apiKeyEnvVar":"MAIL_AR"},
                "pec": {"host":"smtp.ar.test","port":465,"sender":"pec@ar.test",
                        "usernameEnvVar":"SMTP_USR_AR","passwordEnvVar":"SMTP_PSW_AR"},
                "eventHub": {"namespaceUrl":"https://ar.servicebus.windows.net/","topic":"delegations",
                  "authentication":{"type":"MANAGED_IDENTITY","managedIdentityClientIdEnvVar":"IDENTITY_AR"}}
              },
              "PNPG": {
                "oneMail": {"url":"https://mail.pnpg.test","sender":"mail@pnpg.test","apiKeyEnvVar":"MAIL_PNPG"},
                "pec": {"host":"smtp.pnpg.test","port":587,"sender":"pec@pnpg.test",
                        "usernameEnvVar":"SMTP_USR_PNPG","passwordEnvVar":"SMTP_PSW_PNPG"},
                "eventHub": {"namespaceUrl":"https://pnpg.servicebus.windows.net/","topic":"other",
                  "authentication":{"type":"SHARED_ACCESS_KEY","keyName":"sender","keyEnvVar":"KEY_PNPG"}}
              }
            }
            """;

    @Test
    void resolvesAllIntegrationsPerTenantAndRedactsSecrets() {
        TenantRegistry registry = registry(environment(), REGISTRY, "AR,PNPG", "AR,PNPG");
        registry.initialize();
        for (String tenant : registry.supportedTenantIds()) {
            String domain = tenant.toLowerCase();
            var mail = registry.oneMailCredentials(tenant);
            assertEquals("https://mail." + domain + ".test", mail.url());
            assertEquals("mail@" + domain + ".test", mail.sender());
            assertEquals("mail-key-" + tenant, mail.apiKey());
            assertEquals("OneMailCredentials[REDACTED]", mail.toString());
            var pec = registry.pecCredentials(tenant);
            assertEquals("smtp." + domain + ".test", pec.host());
            assertEquals(tenant.equals("AR") ? 465 : 587, pec.port());
            assertEquals("pec@" + domain + ".test", pec.sender());
            assertEquals("user-" + tenant, pec.username());
            assertEquals("password-" + tenant, pec.password());
            assertEquals("PecCredentials[REDACTED]", pec.toString());
            assertEquals("EventHubCredentials[REDACTED]",
                    registry.eventHubCredentials(tenant).orElseThrow().toString());
        }
        var arHub = registry.eventHubCredentials("AR").orElseThrow();
        assertEquals(TenantDefinition.EventHubAuthenticationType.MANAGED_IDENTITY, arHub.type());
        assertEquals("identity-ar", arHub.managedIdentityClientId());
        var pnpgHub = registry.eventHubCredentials("PNPG").orElseThrow();
        assertEquals(TenantDefinition.EventHubAuthenticationType.SHARED_ACCESS_KEY, pnpgHub.type());
        assertEquals("sender", pnpgHub.keyName());
        assertEquals("event-key", pnpgHub.key());
    }

    @Test
    void optionalDimensionsRemainCompatibleAndFailClosedOnAccess() {
        TenantRegistry registry = registry(environment(), "{\"AR\":{}}", "", "");
        registry.initialize();
        assertTrue(registry.eventHubCredentials("AR").isEmpty());
        assertThrows(IllegalStateException.class, () -> registry.oneMailCredentials("AR"));
        assertThrows(IllegalStateException.class, () -> registry.pecCredentials("AR"));
        assertThrows(UnknownTenantException.class, () -> registry.eventHubCredentials("UNKNOWN"));
    }

    @Test
    void mandatoryDimensionsFailStartupWhenAbsentOrUnsupported() {
        assertThrows(IllegalStateException.class,
                () -> registry(environment(), "{\"AR\":{}}", "AR", "").initialize());
        assertThrows(IllegalStateException.class,
                () -> registry(environment(), "{\"AR\":{}}", "", "AR").initialize());
        assertThrows(UnknownTenantException.class,
                () -> registry(environment(), "{\"AR\":{}}", "PNPG", "").initialize());
    }

    @ParameterizedTest
    @ValueSource(strings = {"MAIL_AR", "MAIL_PNPG", "SMTP_USR_AR", "SMTP_PSW_AR",
            "SMTP_USR_PNPG", "SMTP_PSW_PNPG", "IDENTITY_AR", "KEY_PNPG"})
    void missingOrBlankSecretsFailStartupAndRuntime(String name) {
        for (String value : new String[] {null, "", "   "}) {
            MockEnvironment environment = environment();
            TenantRegistry registry = registry(environment, REGISTRY, "", "");
            registry.initialize();
            if (value == null) {
                ((org.springframework.core.env.MapPropertySource) environment.getPropertySources()
                        .get("mockProperties")).getSource().remove(name);
            } else {
                environment.setProperty(name, value);
            }
            assertThrows(IllegalStateException.class, registry::initialize);
            if (name.startsWith("MAIL_")) {
                assertThrows(IllegalStateException.class,
                        () -> registry.oneMailCredentials(name.substring(5)));
            } else if (name.startsWith("SMTP_")) {
                assertThrows(IllegalStateException.class,
                        () -> registry.pecCredentials(name.substring(9)));
            } else {
                assertThrows(IllegalStateException.class,
                        () -> registry.eventHubCredentials(name.equals("IDENTITY_AR") ? "AR" : "PNPG"));
            }
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "\"oneMail\":{\"url\":\"https://mail.test\",\"sender\":\"mail@test\",\"apiKeyEnvVar\":\"invalid-name\"}",
            "\"oneMail\":{\"sender\":\"mail@test\",\"apiKeyEnvVar\":\"MAIL_AR\"}",
            "\"pec\":{\"host\":\"smtp.test\",\"port\":0,\"sender\":\"mail@test\",\"usernameEnvVar\":\"SMTP_USR_AR\",\"passwordEnvVar\":\"SMTP_PSW_AR\"}",
            "\"eventHub\":{\"namespaceUrl\":\"https://hub.test\",\"topic\":\"events\"}",
            "\"eventHub\":{\"namespaceUrl\":\"https://hub.test\",\"topic\":\"events\",\"authentication\":{\"type\":\"SHARED_ACCESS_KEY\",\"keyEnvVar\":\"KEY_PNPG\"}}"
    })
    void rejectsIncompleteOrInvalidDefinitions(String dimension) {
        assertThrows(IllegalStateException.class,
                () -> registry(environment(), "{\"AR\":{" + dimension + "}}", "", "").initialize());
    }

    private TenantRegistry registry(MockEnvironment environment, String json, String mail, String pec) {
        return new TenantRegistry(new ObjectMapper(), environment, json, "*", "", "", "", mail, pec);
    }

    private MockEnvironment environment() {
        return new MockEnvironment().withProperty("tenant.mongodb.required", "false")
                .withProperty("MAIL_AR", "mail-key-AR").withProperty("MAIL_PNPG", "mail-key-PNPG")
                .withProperty("SMTP_USR_AR", "user-AR").withProperty("SMTP_PSW_AR", "password-AR")
                .withProperty("SMTP_USR_PNPG", "user-PNPG").withProperty("SMTP_PSW_PNPG", "password-PNPG")
                .withProperty("IDENTITY_AR", "identity-ar").withProperty("KEY_PNPG", "event-key");
    }
}
