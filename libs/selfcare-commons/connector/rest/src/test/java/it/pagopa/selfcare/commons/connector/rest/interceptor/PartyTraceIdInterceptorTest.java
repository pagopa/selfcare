package it.pagopa.selfcare.commons.connector.rest.interceptor;

import feign.RequestTemplate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.web.context.request.RequestContextHolder;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class PartyTraceIdInterceptorTest {

    private final PartyTraceIdInterceptor interceptor;


    PartyTraceIdInterceptorTest() {
        this.interceptor = new PartyTraceIdInterceptor();
    }


    @BeforeEach
    void resetContext() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void applyMDCHeaders() {
        //given
        MDC.put("traceId", UUID.randomUUID().toString());

        RequestTemplate requestTemplate = new RequestTemplate();
        //when
        interceptor.apply(requestTemplate);
        //then
        Map<String, Collection<String>> headers = requestTemplate.headers();
        Collection<String> spanId = headers.get("X-Correlation-Id");

        assertNotNull(spanId);
    }

    @Test
    void nullTraceId() {
        //given
        RequestTemplate requestTemplate = new RequestTemplate();
        //when
        interceptor.apply(requestTemplate);
        //then
        Map<String, Collection<String>> headers = requestTemplate.headers();
        assertNull(headers.get("X-Correlation-Id"));

    }

}