package it.pagopa.selfcare.contractscrm.client.auth;

import com.azure.core.credential.AccessToken;
import com.azure.core.credential.TokenRequestContext;
import com.azure.identity.DefaultAzureCredential;
import com.azure.identity.DefaultAzureCredentialBuilder;
import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.client.ClientRequestFilter;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Optional;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * Authorizes Event Hub REST calls with a managed identity bearer token when a client id is
 * configured, with a SAS token otherwise.
 */
@Slf4j
public class EventhubTokenAuthorization implements ClientRequestFilter {

  private static final long SAS_TOKEN_VALIDITY_SECONDS = 60L * 60 * 24 * 7;

  private final URI resourceUri;
  private final String keyName;
  private final String key;
  private final Optional<DefaultAzureCredential> credential;
  private AccessToken cachedToken;

  public EventhubTokenAuthorization(
      @ConfigProperty(name = "quarkus.rest-client.event-hub.url") URI resourceUri,
      @ConfigProperty(name = "eventhub.rest-client.keyName") String keyName,
      @ConfigProperty(name = "eventhub.rest-client.key") String key,
      @ConfigProperty(name = "eventhub.sender.managed-identity-client-id")
          Optional<String> managedIdentityClientId) {
    this.resourceUri = resourceUri;
    this.keyName = keyName;
    this.key = key;
    this.credential =
        managedIdentityClientId.map(
            clientId -> new DefaultAzureCredentialBuilder().managedIdentityClientId(clientId).build());
  }

  @Override
  public void filter(ClientRequestContext requestContext) throws IOException {
    try {
      String authToken =
          credential.isPresent()
              ? getBearerToken(credential.get())
              : getSasToken(resourceUri.toString(), keyName, key);
      requestContext.getHeaders().add("Authorization", authToken);
    } catch (Exception e) {
      throw new IOException("Error generating Event Hub authorization token", e);
    }
  }

  private synchronized String getBearerToken(DefaultAzureCredential azureCredential) {
    if (cachedToken == null || cachedToken.isExpired()) {
      cachedToken =
          azureCredential.getTokenSync(
              new TokenRequestContext().addScopes("https://eventhubs.azure.net/.default"));
    }
    return "Bearer " + cachedToken.getToken();
  }

  static String getSasToken(String resourceUri, String keyName, String key)
      throws NoSuchAlgorithmException, InvalidKeyException {
    String expiry = Long.toString(System.currentTimeMillis() / 1000L + SAS_TOKEN_VALIDITY_SECONDS);
    String encodedUri = URLEncoder.encode(resourceUri, StandardCharsets.UTF_8);
    String signature = getHmac256(key, encodedUri + "\n" + expiry);
    return "SharedAccessSignature sr="
        + encodedUri
        + "&sig="
        + URLEncoder.encode(signature, StandardCharsets.UTF_8)
        + "&se="
        + expiry
        + "&skn="
        + keyName;
  }

  private static String getHmac256(String key, String input)
      throws NoSuchAlgorithmException, InvalidKeyException {
    Mac sha256Hmac = Mac.getInstance("HmacSHA256");
    sha256Hmac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
    return Base64.getEncoder()
        .encodeToString(sha256Hmac.doFinal(input.getBytes(StandardCharsets.UTF_8)));
  }
}
