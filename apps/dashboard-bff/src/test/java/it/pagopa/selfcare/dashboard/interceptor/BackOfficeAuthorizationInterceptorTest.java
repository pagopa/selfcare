package it.pagopa.selfcare.dashboard.interceptor;

import feign.RequestTemplate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.RequestContextHolder;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class BackOfficeAuthorizationInterceptorTest {

    private final BackOfficeAuthorizationInterceptor interceptor;


    BackOfficeAuthorizationInterceptorTest() {
        interceptor = new BackOfficeAuthorizationInterceptor();
    }


    @BeforeEach
    void resetContext() {
        SecurityContextHolder.clearContext();
        RequestContextHolder.resetRequestAttributes();
    }


    @Test
    void applyEmptySubKey() {
        // given
        RequestTemplate requestTemplate = new RequestTemplate();

        // when
        interceptor.apply(requestTemplate);

        // then
        Map<String, Collection<String>> headers = requestTemplate.headers();
        assertNull(headers.get("Ocp-Apim-Subscription-Key"));
    }

    @Test
    void applySubKey() {
        // given
        RequestTemplate requestTemplate = new RequestTemplate();
        requestTemplate.header("Ocp-Apim-Subscription-Key", "1");
        // when
        interceptor.apply(requestTemplate);

        // then
        Map<String, Collection<String>> headers = requestTemplate.headers();
        assertEquals(List.of("1"), headers.get("Ocp-Apim-Subscription-Key"));
    }
}
