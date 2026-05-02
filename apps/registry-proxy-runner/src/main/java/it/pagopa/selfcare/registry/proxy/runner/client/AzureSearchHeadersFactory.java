package it.pagopa.selfcare.registry.proxy.runner.client;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.ext.ClientHeadersFactory;

@ApplicationScoped
public class AzureSearchHeadersFactory implements ClientHeadersFactory {

    @ConfigProperty(name = "azure-ai-search.api-key")
    String apiKey;

    @Override
    public MultivaluedMap<String, String> update(MultivaluedMap<String, String> incomingHeaders,
                                                  MultivaluedMap<String, String> clientOutgoingHeaders) {
        MultivaluedMap<String, String> result = new MultivaluedHashMap<>(clientOutgoingHeaders);
        result.add("api-key", apiKey);
        result.add("Content-Type", "application/json");
        return result;
    }
}
