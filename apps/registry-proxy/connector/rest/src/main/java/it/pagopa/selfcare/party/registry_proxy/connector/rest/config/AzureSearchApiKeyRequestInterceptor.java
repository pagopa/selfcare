package it.pagopa.selfcare.party.registry_proxy.connector.rest.config;

import feign.RequestInterceptor;
import feign.RequestTemplate;

public class AzureSearchApiKeyRequestInterceptor implements RequestInterceptor {

    private final String apiKey;

    public AzureSearchApiKeyRequestInterceptor(String apiKey) {
        this.apiKey = apiKey;
    }

    @Override
    public void apply(RequestTemplate template) {
        if (!template.headers().containsKey("api-key")) {
            template.header("api-key", apiKey);
        }
    }
}