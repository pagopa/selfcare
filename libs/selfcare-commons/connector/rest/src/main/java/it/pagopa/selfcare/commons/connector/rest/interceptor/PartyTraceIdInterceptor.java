package it.pagopa.selfcare.commons.connector.rest.interceptor;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;

import java.util.Optional;

@Slf4j
public class PartyTraceIdInterceptor implements RequestInterceptor {

    private static final String PARTY_TRACE_ID = "X-Correlation-Id";

    @Override
    public void apply(RequestTemplate template) {
        Optional.ofNullable(MDC.get("traceId"))
                .ifPresent(traceId -> template.header(PARTY_TRACE_ID, traceId));
    }
}
