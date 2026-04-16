package it.pagopa.selfcare.party.registry_proxy.connector.rest.interceptor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import java.io.IOException;


@Slf4j
@Component
public class IvassInterceptor implements ClientHttpRequestInterceptor {
    @Override
    @NonNull
    public ClientHttpResponse intercept(@NonNull HttpRequest request, @NonNull byte[] body, @NonNull ClientHttpRequestExecution execution) throws IOException {
        log.info("Request: " + request.getURI());
        log.info("Headers: " + request.getHeaders());
        log.info("Method: " + request.getMethod());
        ClientHttpResponse response = execution.execute(request, body);
        log.info("Response Status: " + response.getStatusCode());
        log.info("Response Headers: " + response.getHeaders());
        return response;
    }
}

