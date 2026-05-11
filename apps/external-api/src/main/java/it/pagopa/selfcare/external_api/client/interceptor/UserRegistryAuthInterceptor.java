package it.pagopa.selfcare.external_api.client.interceptor;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;

@Slf4j
public class UserRegistryAuthInterceptor implements RequestInterceptor {

    private final String apiKey;

    public UserRegistryAuthInterceptor(@Value("${rest-client.user-registry.x-api-key:}") String apiKey) {
        this.apiKey = apiKey;
    }

    @Override
    public void apply(RequestTemplate template) {
        template.header("x-api-key", apiKey);
    }

}
