package it.pagopa.selfcare.mscore.connector.rest.interceptor;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.springframework.beans.factory.annotation.Value;

public class OneMailAuthInterceptor implements RequestInterceptor {

    private static final String API_KEY_HEADER = "x-api-key";

    private final String apiKey;

    public OneMailAuthInterceptor(@Value("${rest-client.one-mail.x-api-key:}") String apiKey) {
        this.apiKey = apiKey;
    }

    @Override
    public void apply(RequestTemplate template) {
        template.header(API_KEY_HEADER, apiKey);
    }
}
