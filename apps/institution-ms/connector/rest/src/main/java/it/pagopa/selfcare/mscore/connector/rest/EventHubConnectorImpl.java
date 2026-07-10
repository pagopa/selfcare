package it.pagopa.selfcare.mscore.connector.rest;

import com.azure.core.credential.AccessToken;
import com.azure.core.credential.TokenRequestContext;
import com.azure.identity.DefaultAzureCredential;
import com.azure.identity.DefaultAzureCredentialBuilder;
import it.pagopa.selfcare.mscore.api.EventHubConnector;
import it.pagopa.selfcare.mscore.connector.rest.client.EventHubRestClient;
import it.pagopa.selfcare.mscore.model.DelegationNotificationToSend;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;

@Service
@Slf4j
public class EventHubConnectorImpl implements EventHubConnector {

    private final EventHubRestClient eventHubRestClient;
    private final String resourceUri;
    private final String keyName;
    private final String key;
    private final Optional<DefaultAzureCredential> credential;
    private AccessToken cachedToken;

    public EventHubConnectorImpl(EventHubRestClient eventHubRestClient,
                                 @Value("${rest-client.event-hub.base-url}") String resourceUri,
                                 @Value("${rest-client.event-hub.keyName}") String keyName,
                                 @Value("${rest-client.event-hub.key}") String key,
                                 @Value("${rest-client.event-hub.sender.managed-identity-client-id}") String managedIdentityClientId) {
        this.eventHubRestClient = eventHubRestClient;
        this.resourceUri = resourceUri;
        this.keyName = keyName;
        this.key = key;
        this.credential = Optional.ofNullable(managedIdentityClientId)
          .filter(ci -> !ci.isBlank())
          .map(ci -> new DefaultAzureCredentialBuilder().managedIdentityClientId(ci).build());
        log.info("EventhubTokenAuthorization managedIdentityClientId: {}", managedIdentityClientId);
    }

    @Override
    public boolean sendEvent(DelegationNotificationToSend notification) {
        try {
            final String authToken = credential.map(this::getBearerToken)
              .orElse(getSASToken(resourceUri, keyName, key));
            eventHubRestClient.sendMessage(notification, Map.of("Authorization", authToken));
            log.info("Event notification of delegation with id {} sent", notification.getDelegationId());
            return true;
        } catch (Exception ex) {
            log.error("Error sending event notification of delegation with id {}", notification.getDelegationId(), ex);
            return false;
        }
    }

    private String getBearerToken(DefaultAzureCredential c) {
        if (cachedToken == null || cachedToken.isExpired()) {
            log.info("EventhubTokenAuthorization cachedToken expired: fetching new token from Azure AD");
            cachedToken = c.getTokenSync(new TokenRequestContext().addScopes("https://eventhubs.azure.net/.default"));
        }
        return "Bearer " + cachedToken.getToken();
    }

    private String getSASToken(String resourceUri, String keyName, String key) throws NoSuchAlgorithmException, InvalidKeyException {
        final long epoch = System.currentTimeMillis() / 1000L;
        final int week = 60 * 60 * 24 * 7;
        final String expiry = Long.toString(epoch + week);

        final String stringToSign = URLEncoder.encode(resourceUri, StandardCharsets.UTF_8) + "\n" + expiry;
        final String signature = getHMAC256(key, stringToSign);
        return "SharedAccessSignature sr=" + URLEncoder.encode(resourceUri, StandardCharsets.UTF_8) + "&sig=" +
                URLEncoder.encode(signature, StandardCharsets.UTF_8) + "&se=" + expiry + "&skn=" + keyName;
    }

    private String getHMAC256(String key, String input) throws InvalidKeyException, NoSuchAlgorithmException {
        final Mac sha256HMAC = Mac.getInstance("HmacSHA256");
        SecretKeySpec secretKey = new SecretKeySpec(key.getBytes(), "HmacSHA256");
        sha256HMAC.init(secretKey);
        Base64.Encoder encoder = Base64.getEncoder();
        return new String(encoder.encode(sha256HMAC.doFinal(input.getBytes(StandardCharsets.UTF_8))));
    }

}
