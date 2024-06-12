package it.pagopa.selfcare.user.auth;

import jakarta.inject.Inject;
import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.client.ClientRequestFilter;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
public class EventhubSasTokenAuthorization implements ClientRequestFilter {

    @Inject
    @ConfigProperty(name = "rest-client.event-hub.uri")
    URI resourceUri;

    @Inject
    @ConfigProperty(name = "eventhub.rest-client.keyName")
    String keyName;

    @Inject
    @ConfigProperty(name = "eventhub.rest-client.key")
    String key;

    @Override
    public void filter(ClientRequestContext clientRequestContext) throws IOException {
        clientRequestContext.getHeaders().add("Authorization", getSASToken(resourceUri.toString(), keyName, key));

    }

    private static String getSASToken(String resourceUri, String keyName, String key) {
        long epoch = System.currentTimeMillis() / 1000L;
        int week = 60 * 60 * 24 * 7;
        String expiry = Long.toString(epoch + week);

        String sasToken = null;
        try {
            String stringToSign = URLEncoder.encode(resourceUri, "UTF-8") + "\n" + expiry;
            String signature = getHMAC256(key, stringToSign);
            sasToken = "SharedAccessSignature sr=" + URLEncoder.encode(resourceUri, "UTF-8") + "&sig=" +
                    URLEncoder.encode(signature, "UTF-8") + "&se=" + expiry + "&skn=" + keyName;
        } catch (UnsupportedEncodingException e) {

            e.printStackTrace();
        }

        return sasToken;
    }


    public static String getHMAC256(String key, String input) {
        Mac sha256_HMAC = null;
        String hash = null;
        try {
            sha256_HMAC = Mac.getInstance("HmacSHA256");
            SecretKeySpec secret_key = new SecretKeySpec(key.getBytes(), "HmacSHA256");
            sha256_HMAC.init(secret_key);
            Base64.Encoder encoder = Base64.getEncoder();

            hash = new String(encoder.encode(sha256_HMAC.doFinal(input.getBytes("UTF-8"))));

        } catch (InvalidKeyException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (IllegalStateException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        return hash;
    }



}
