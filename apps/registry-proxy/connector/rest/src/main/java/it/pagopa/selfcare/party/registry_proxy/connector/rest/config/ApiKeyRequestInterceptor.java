package it.pagopa.selfcare.party.registry_proxy.connector.rest.config;

import feign.RequestInterceptor;
import feign.RequestTemplate;

public class ApiKeyRequestInterceptor implements RequestInterceptor {

    private final String apiKey;

    public ApiKeyRequestInterceptor(String apiKey) {
        this.apiKey = apiKey;
    }

    @Override
    public void apply(RequestTemplate template) {
        if (!template.headers().containsKey("x-api-key")) {
            template.header("x-api-key", apiKey);
        }
    }
}