package it.pagopa.selfcare.user.auth;

import com.azure.core.credential.AccessToken;
import com.azure.core.credential.TokenRequestContext;
import com.azure.identity.DefaultAzureCredential;
import com.azure.identity.DefaultAzureCredentialBuilder;
import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.client.ClientRequestFilter;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.io.IOException;
import java.net.URI;
import java.util.Optional;

import static it.pagopa.selfcare.user.UserUtils.getSASToken;

@Slf4j
public class EventhubFdTokenAuthorization implements ClientRequestFilter {

    private final URI resourceUri;
    private final String keyName;
    private final String key;
    private final Optional<DefaultAzureCredential> credential;
    private AccessToken cachedToken;

    public EventhubFdTokenAuthorization(@ConfigProperty(name = "quarkus.rest-client.event-hub-fd.url") URI resourceUri,
                                        @ConfigProperty(name = "eventhubfd.rest-client.keyName") String keyName,
                                        @ConfigProperty(name = "eventhubfd.rest-client.key") String key,
                                        @ConfigProperty(name = "eventhubfd.sender.managed-identity-client-id") Optional<String> managedIdentityClientId) {
        this.resourceUri = resourceUri;
        this.keyName = keyName;
        this.key = key;
        this.credential = managedIdentityClientId.map(ci -> new DefaultAzureCredentialBuilder().managedIdentityClientId(ci).build());
        log.info("EventhubTokenAuthorization managedIdentityClientId: {}", managedIdentityClientId.orElse("not set"));
    }

    @Override
    public void filter(ClientRequestContext clientRequestContext) throws IOException {
        final String authToken = credential.map(this::getBearerToken)
          .orElse(getSASToken(resourceUri.toString(), keyName, key));
        clientRequestContext.getHeaders().add("Authorization", authToken);
    }

    private String getBearerToken(DefaultAzureCredential c) {
        if (cachedToken == null || cachedToken.isExpired()) {
            log.info("EventhubTokenAuthorization cachedToken expired: fetching new token from Azure AD");
            cachedToken = c.getTokenSync(new TokenRequestContext().addScopes("https://eventhubs.azure.net/.default"));
        }
        return "Bearer " + cachedToken.getToken();
    }

}
