package it.pagopa.selfcare.mscore.connector.rest;

import com.azure.core.credential.AccessToken;
import com.azure.core.credential.TokenCredential;
import it.pagopa.selfcare.commons.tenant.TenantContext;
import it.pagopa.selfcare.commons.tenant.TenantDefinition.EventHubAuthenticationType;
import it.pagopa.selfcare.commons.tenant.TenantDefinition.EventHubCredentials;
import it.pagopa.selfcare.commons.tenant.TenantRegistry;
import it.pagopa.selfcare.mscore.connector.rest.client.EventHubRestClient;
import it.pagopa.selfcare.mscore.model.DelegationNotificationToSend;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class EventHubConnectorImplTest {
    private final TenantRegistry registry = mock(TenantRegistry.class);
    private final TenantContext context = new TenantContext();
    private final EventHubRestClient client = mock(EventHubRestClient.class);
    private final EventHubConnectorImpl connector = spy(new EventHubConnectorImpl(client, registry, context));
    private final DelegationNotificationToSend notification = new DelegationNotificationToSend();

    @BeforeEach
    void normalizeTenants() {
        when(registry.normalizeAndValidate(anyString())).thenAnswer(call -> call.getArgument(0));
    }

    @Test
    void sharedAccessKeysUseTenantResourceAndSeparateCachedTokens() {
        for (String tenant : new String[] {"AR", "PNPG"}) {
            when(registry.eventHubCredentials(tenant)).thenReturn(Optional.of(new EventHubCredentials(
                    "https://" + tenant.toLowerCase() + ".test/", "delegations",
                    EventHubAuthenticationType.SHARED_ACCESS_KEY, null, tenant + "-sender", tenant + "-key")));
            assertTrue(context.withTenant(tenant, () -> connector.sendEvent(notification)));
        }
        assertTrue(context.withTenant("AR", () -> connector.sendEvent(notification)));
        ArgumentCaptor<URI> uris = ArgumentCaptor.forClass(URI.class);
        ArgumentCaptor<Map<String, String>> headers = ArgumentCaptor.forClass(Map.class);
        verify(client, times(3)).sendMessage(uris.capture(), same(notification), headers.capture());
        assertEquals(URI.create("https://ar.test/delegations"), uris.getAllValues().get(0));
        assertEquals(URI.create("https://pnpg.test/delegations"), uris.getAllValues().get(1));
        String arToken = headers.getAllValues().get(0).get("Authorization");
        String pnpgToken = headers.getAllValues().get(1).get("Authorization");
        assertTrue(arToken.startsWith("SharedAccessSignature sr="
                + URLEncoder.encode("https://ar.test/delegations", StandardCharsets.UTF_8)));
        assertTrue(arToken.endsWith("&skn=AR-sender"));
        assertTrue(pnpgToken.endsWith("&skn=PNPG-sender"));
        assertNotEquals(arToken, pnpgToken);
        assertEquals(arToken, headers.getAllValues().get(2).get("Authorization"));
        verify(connector, never()).createCredential(anyString());
    }

    @Test
    void managedIdentityCredentialsAndTokensAreCachedPerTenantAndRefreshed() {
        TokenCredential arCredential = mock(TokenCredential.class);
        TokenCredential pnpgCredential = mock(TokenCredential.class);
        doReturn(arCredential).when(connector).createCredential("identity-AR");
        doReturn(pnpgCredential).when(connector).createCredential("identity-PNPG");
        when(arCredential.getTokenSync(any())).thenReturn(
                new AccessToken("expired-ar", OffsetDateTime.now().minusMinutes(1)),
                new AccessToken("new-ar", OffsetDateTime.now().plusHours(1)));
        when(pnpgCredential.getTokenSync(any())).thenReturn(
                new AccessToken("pnpg", OffsetDateTime.now().plusHours(1)));
        for (String tenant : new String[] {"AR", "PNPG"}) {
            when(registry.eventHubCredentials(tenant)).thenReturn(Optional.of(new EventHubCredentials(
                    "https://" + tenant.toLowerCase() + ".test", "events",
                    EventHubAuthenticationType.MANAGED_IDENTITY, "identity-" + tenant, null, null)));
            assertTrue(context.withTenant(tenant, () -> connector.sendEvent(notification)));
        }
        assertTrue(context.withTenant("AR", () -> connector.sendEvent(notification)));
        assertTrue(context.withTenant("AR", () -> connector.sendEvent(notification)));
        assertTrue(context.withTenant("PNPG", () -> connector.sendEvent(notification)));
        verify(connector).createCredential("identity-AR");
        verify(connector).createCredential("identity-PNPG");
        verify(arCredential, times(2)).getTokenSync(argThat(
                request -> request.getScopes().contains("https://eventhubs.azure.net/.default")));
        verify(pnpgCredential).getTokenSync(any());
        verify(client, times(2)).sendMessage(URI.create("https://ar.test/events"),
                notification, Map.of("Authorization", "Bearer new-ar"));
        verify(client, times(2)).sendMessage(URI.create("https://pnpg.test/events"),
                notification, Map.of("Authorization", "Bearer pnpg"));
    }

    @Test
    void absentEventHubSkipsHttpAndMissingTenantOrSendFailureReportsFailure() {
        when(registry.eventHubCredentials("PNPG")).thenReturn(Optional.empty());
        assertTrue(context.withTenant("PNPG", () -> connector.sendEvent(notification)));
        assertFalse(connector.sendEvent(notification));
        verifyNoInteractions(client);
        when(registry.eventHubCredentials("AR")).thenReturn(Optional.of(new EventHubCredentials(
                "https://ar.test", "events", EventHubAuthenticationType.SHARED_ACCESS_KEY,
                null, "sender", "key")));
        doThrow(new IllegalStateException("HTTP failure")).when(client).sendMessage(any(), any(), anyMap());
        assertFalse(context.withTenant("AR", () -> connector.sendEvent(notification)));
    }
}
