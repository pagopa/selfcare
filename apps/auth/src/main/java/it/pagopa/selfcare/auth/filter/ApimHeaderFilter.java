package it.pagopa.selfcare.auth.filter;

import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.client.ClientRequestFilter;
import jakarta.ws.rs.ext.Provider;
import org.eclipse.microprofile.config.ConfigProvider;

import java.util.Optional;

@Provider
public class ApimHeaderFilter implements ClientRequestFilter {

    private static final String HEADER_NAME = "Ocp-Apim-Subscription-Key";
    private static final String CONFIG_PREFIX = "internal-api-key.";
    private static final String CONFIG_DEFAULT = "internal-api-key.default";

    @Override
    public void filter(ClientRequestContext requestContext) {
        String interfaceName = getClientInterfaceName(requestContext);
        String configKey = CONFIG_PREFIX + interfaceName;

        Optional<String> apiKey = getConfigValue(configKey)
                .or(() -> getConfigValue(CONFIG_DEFAULT));

        apiKey.ifPresent(value ->
                requestContext.getHeaders().putSingle(HEADER_NAME, value)
        );
    }

    private String getClientInterfaceName(ClientRequestContext context) {
        Object iface = context.getConfiguration().getProperty("org.eclipse.microprofile.rest.client.interface");
        return iface != null ? iface.toString() : "unknown";
    }

    private Optional<String> getConfigValue(String key) {
        try {
            return Optional.of(ConfigProvider.getConfig().getValue(key, String.class));
        } catch (Exception ignored) {
            return Optional.empty();
        }
    }
}
