package it.pagopa.selfcare.party.registry_proxy.connector.rest.config;

import feign.RequestInterceptor;
import feign.RequestTemplate;

public class JwtTokenRequestInterceptor implements RequestInterceptor {

    private final String jwtBearer;

    public JwtTokenRequestInterceptor(String jwtBearer) {
        this.jwtBearer = jwtBearer;
    }

    @Override
    public void apply(RequestTemplate template) {
        if (!template.headers().containsKey("Authorization")) {
            template.header("Authorization", "Bearer " + jwtBearer);
        }
    }
}