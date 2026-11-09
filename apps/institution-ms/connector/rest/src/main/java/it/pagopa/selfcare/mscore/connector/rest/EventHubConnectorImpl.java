package it.pagopa.selfcare.mscore.connector.rest;

import com.azure.core.credential.AccessToken;
import com.azure.core.credential.TokenRequestContext;
import com.azure.core.credential.TokenCredential;
import com.azure.identity.DefaultAzureCredentialBuilder;
import it.pagopa.selfcare.commons.tenant.TenantContext;
import it.pagopa.selfcare.commons.tenant.TenantDefinition.EventHubCredentials;
import it.pagopa.selfcare.commons.tenant.TenantDefinition.EventHubAuthenticationType;
import it.pagopa.selfcare.commons.tenant.TenantRegistry;
import it.pagopa.selfcare.mscore.api.EventHubConnector;
import it.pagopa.selfcare.mscore.connector.rest.client.EventHubRestClient;
import it.pagopa.selfcare.mscore.model.DelegationNotificationToSend;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Map;
import java.time.OffsetDateTime;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class EventHubConnectorImpl implements EventHubConnector {

    private final EventHubRestClient eventHubRestClient;
    private final TenantRegistry tenantRegistry;
    private final TenantContext tenantContext;
    private final Map<String, TokenCredential> credentials = new ConcurrentHashMap<>();
    private final Map<String, AccessToken> tokens = new ConcurrentHashMap<>();

    public EventHubConnectorImpl(EventHubRestClient eventHubRestClient,
                                 TenantRegistry tenantRegistry, TenantContext tenantContext) {
        this.eventHubRestClient = eventHubRestClient;
        this.tenantRegistry = tenantRegistry;
        this.tenantContext = tenantContext;
    }

    @Override
    public boolean sendEvent(DelegationNotificationToSend notification) {
        try {
            String tenantId = tenantRegistry.normalizeAndValidate(tenantContext.requiredTenantId());
            var configuration = tenantRegistry.eventHubCredentials(tenantId);
            if (configuration.isEmpty()) {
                log.info("Skipping EventHub notification for tenant {}: EventHub is not configured", tenantId);
                return true;
            }
            EventHubCredentials settings = configuration.orElseThrow();
            String resourceUri = settings.namespaceUrl().replaceAll("/+$", "") + "/" + settings.topic();
            final String authToken = authorization(tenantId, settings, resourceUri);
            eventHubRestClient.sendMessage(URI.create(resourceUri), notification, Map.of("Authorization", authToken));
            log.info("Event notification of delegation with id {} sent", notification.getDelegationId());
            return true;
        } catch (Exception ex) {
            log.error("Error sending event notification of delegation with id {}", notification.getDelegationId(), ex);
            return false;
        }
    }

    protected TokenCredential createCredential(String managedIdentityClientId) {
        return new DefaultAzureCredentialBuilder().managedIdentityClientId(managedIdentityClientId).build();
    }

    private synchronized String authorization(String tenantId, EventHubCredentials settings, String resourceUri)
            throws NoSuchAlgorithmException, InvalidKeyException {
        AccessToken token = tokens.get(tenantId);
        boolean managedIdentity = settings.type() == EventHubAuthenticationType.MANAGED_IDENTITY;
        if (token == null || token.getExpiresAt().isBefore(OffsetDateTime.now().plusMinutes(1))) {
            if (managedIdentity) {
                TokenCredential credential = credentials.computeIfAbsent(tenantId,
                        ignored -> createCredential(settings.managedIdentityClientId()));
                token = credential.getTokenSync(
                        new TokenRequestContext().addScopes("https://eventhubs.azure.net/.default"));
            } else {
                OffsetDateTime expiresAt = OffsetDateTime.now().plusDays(7);
                token = new AccessToken(getSASToken(resourceUri, settings.keyName(), settings.key(),
                        expiresAt.toEpochSecond()), expiresAt);
            }
            tokens.put(tenantId, token);
        }
        return (managedIdentity ? "Bearer " : "") + token.getToken();
    }

    private String getSASToken(String resourceUri, String keyName, String key, long expiresAt) throws NoSuchAlgorithmException, InvalidKeyException {
        final String expiry = Long.toString(expiresAt);

        final String stringToSign = URLEncoder.encode(resourceUri, StandardCharsets.UTF_8) + "\n" + expiry;
        final String signature = getHMAC256(key, stringToSign);
        return "SharedAccessSignature sr=" + URLEncoder.encode(resourceUri, StandardCharsets.UTF_8) + "&sig=" +
                URLEncoder.encode(signature, StandardCharsets.UTF_8) + "&se=" + expiry + "&skn=" + keyName;
    }

    private String getHMAC256(String key, String input) throws InvalidKeyException, NoSuchAlgorithmException {
        final Mac sha256HMAC = Mac.getInstance("HmacSHA256");
        SecretKeySpec secretKey = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        sha256HMAC.init(secretKey);
        Base64.Encoder encoder = Base64.getEncoder();
        return new String(encoder.encode(sha256HMAC.doFinal(input.getBytes(StandardCharsets.UTF_8))));
    }

}
