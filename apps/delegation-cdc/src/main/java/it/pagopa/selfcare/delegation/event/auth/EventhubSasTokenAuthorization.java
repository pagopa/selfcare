package it.pagopa.selfcare.delegation.event.auth;

import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.client.ClientRequestFilter;
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

public class EventhubSasTokenAuthorization implements ClientRequestFilter {

    private final URI resourceUri;
    private final String keyName;
    private final String key;

    public EventhubSasTokenAuthorization(@ConfigProperty(name = "quarkus.rest-client.event-hub.url") URI resourceUri,
                                         @ConfigProperty(name = "eventhub.rest-client.keyName") String keyName,
                                         @ConfigProperty(name = "eventhub.rest-client.key") String key) {
        this.resourceUri = resourceUri;
        this.keyName = keyName;
        this.key = key;
    }

    @Override
    public void filter(ClientRequestContext clientRequestContext) throws IOException {
        try {
            clientRequestContext.getHeaders().add("Authorization", getSASToken(resourceUri.toString(), keyName, key));
        } catch (Exception e) {
            throw new IOException("Error generating SAS token", e);
        }
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
