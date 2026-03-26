package it.pagopa.selfcare.onboarding.connector.rest.config;

import feign.RequestInterceptor;
import feign.RequestTemplate;

public class ApiKeyRequestInterceptor implements RequestInterceptor {

    private final String apiKey;

    public ApiKeyRequestInterceptor(String apiKey) {
        this.apiKey = apiKey;
    }

    @Override
    public void apply(RequestTemplate template) {
        template.header("x-api-key", apiKey);
    }
}