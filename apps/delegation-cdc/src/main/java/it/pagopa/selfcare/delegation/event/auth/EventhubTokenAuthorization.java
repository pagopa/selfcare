package it.pagopa.selfcare.delegation.event.auth;

import com.azure.core.credential.AccessToken;
import com.azure.core.credential.TokenRequestContext;
import com.azure.identity.DefaultAzureCredential;
import com.azure.identity.DefaultAzureCredentialBuilder;
import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.client.ClientRequestFilter;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Optional;

@Slf4j
public class EventhubTokenAuthorization implements ClientRequestFilter {

    private final URI resourceUri;
    private final String keyName;
    private final String key;
    private final Optional<DefaultAzureCredential> credential;
    private AccessToken cachedToken;

    public EventhubTokenAuthorization(@ConfigProperty(name = "quarkus.rest-client.event-hub.url") URI resourceUri,
                                      @ConfigProperty(name = "eventhub.rest-client.keyName") String keyName,
                                      @ConfigProperty(name = "eventhub.rest-client.key") String key,
                                      @ConfigProperty(name = "eventhub.sender.managed-identity-client-id") Optional<String> managedIdentityClientId) {
        this.resourceUri = resourceUri;
        this.keyName = keyName;
        this.key = key;
        this.credential = managedIdentityClientId.map(ci -> new DefaultAzureCredentialBuilder().managedIdentityClientId(ci).build());
        log.info("EventhubTokenAuthorization managedIdentityClientId: {}", managedIdentityClientId.orElse("not set"));
    }

    @Override
    public void filter(ClientRequestContext clientRequestContext) throws IOException {
        try {
            final String authToken = credential.map(this::getBearerToken)
              .orElse(getSASToken(resourceUri.toString(), keyName, key));
            clientRequestContext.getHeaders().add("Authorization", authToken);
        } catch (Exception e) {
            throw new IOException("Error generating SAS token", e);
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
