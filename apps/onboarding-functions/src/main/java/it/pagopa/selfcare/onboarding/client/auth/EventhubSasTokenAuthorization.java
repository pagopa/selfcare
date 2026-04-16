package it.pagopa.selfcare.onboarding.client.auth;

import it.pagopa.selfcare.onboarding.config.NotificationConfig;
import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.client.ClientRequestFilter;
import jakarta.ws.rs.core.Context;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Objects;

public class EventhubSasTokenAuthorization implements ClientRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(EventhubSasTokenAuthorization.class);
    private final URI resourceUri;
    private final NotificationConfig notificationConfig;

    public EventhubSasTokenAuthorization(@Context @ConfigProperty(name = "rest-client.event-hub.uri") URI resourceUri,
                                         @Context NotificationConfig notificationConfig) {
        this.resourceUri = resourceUri;
        this.notificationConfig = notificationConfig;
    }

    @Override
    public void filter(ClientRequestContext clientRequestContext) {
        final String[] paths = clientRequestContext.getUri().getPath().split("/");
        final String topic = paths[1];
        NotificationConfig.Consumer consumerConfiguration = notificationConfig.consumers().values().stream()
                .filter(consumer -> consumer.topic().equals(topic))
                .findFirst().orElse(null);
        if (Objects.nonNull(consumerConfiguration)) {
            clientRequestContext.getHeaders().add("Authorization", getSASToken(resourceUri.toString(), consumerConfiguration.name(), consumerConfiguration.key()));
        }
    }

    private static String getSASToken(String resourceUri, String keyName, String key) {
        final long epoch = System.currentTimeMillis() / 1000L;
        final int week = 60 * 60 * 24 * 7;
        String expiry = Long.toString(epoch + week);

        String sasToken;
        final String stringToSign = URLEncoder.encode(resourceUri, StandardCharsets.UTF_8) + "\n" + expiry;
        final String signature = getHMAC256(key, stringToSign);
        sasToken = "SharedAccessSignature sr=" + URLEncoder.encode(resourceUri, StandardCharsets.UTF_8) + "&sig=" +
                URLEncoder.encode(signature, StandardCharsets.UTF_8) + "&se=" + expiry + "&skn=" + keyName;

        return sasToken;
    }

    private static String getHMAC256(String key, String input) {
        Mac sha256HMAC;
        String hash = null;
        try {
            sha256HMAC = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(key.getBytes(), "HmacSHA256");
            sha256HMAC.init(secretKey);
            Base64.Encoder encoder = Base64.getEncoder();

            hash = new String(encoder.encode(sha256HMAC.doFinal(input.getBytes(StandardCharsets.UTF_8))));

        } catch (Exception e) {
            log.warn("Impossible to sign token for event hub rest client. Error: {}", e.getMessage(), e);
        }
        return hash;
    }
}
