package it.pagopa.selfcare.party.registry_proxy.connector.rest.config;

import it.pagopa.selfcare.party.registry_proxy.connector.rest.decoder.RestTemplateErrorHandler;
import it.pagopa.selfcare.party.registry_proxy.connector.rest.interceptor.IvassInterceptor;
import java.util.List;
import org.apache.hc.client5.http.cookie.BasicCookieStore;
import org.apache.hc.client5.http.cookie.CookieStore;
import org.apache.hc.client5.http.impl.DefaultRedirectStrategy;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.protocol.RedirectStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

@Configuration
public class RestTemplateConfig {
    private final IvassInterceptor ivassInterceptor;
    private final RestTemplateErrorHandler restTemplateErrorHandler;

    public RestTemplateConfig(IvassInterceptor ivassInterceptor, RestTemplateErrorHandler restTemplateErrorHandler) {
        this.ivassInterceptor = ivassInterceptor;
        this.restTemplateErrorHandler = restTemplateErrorHandler;
    }

    @Bean
    public RestTemplate ivassRestTemplate() {
        CookieStore cookieStore = new BasicCookieStore();
        RedirectStrategy redirectStrategy = new DefaultRedirectStrategy();
        CloseableHttpClient httpClient = HttpClients.custom()
                .setDefaultCookieStore(cookieStore)
                .setRedirectStrategy(redirectStrategy)
                .build();

        HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory(httpClient);

        RestTemplate restTemplate = new RestTemplate(factory);
        restTemplate.setInterceptors(List.of(ivassInterceptor));
        restTemplate.setErrorHandler(restTemplateErrorHandler);
        return restTemplate;
    }

}
