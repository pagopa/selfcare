package it.pagopa.selfcare.onboarding.connector.rest.config;

import static org.junit.jupiter.api.Assertions.*;

import feign.RequestTemplate;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.RequestContextHolder;

@ExtendWith(MockitoExtension.class)
public class ApiKeyRequestInterceptorTest {

    private final ApiKeyRequestInterceptor interceptor;

    ApiKeyRequestInterceptorTest() {
        interceptor = new ApiKeyRequestInterceptor("api-key-value");
    }

    @BeforeEach
    void resetContext() {
        SecurityContextHolder.clearContext();
        RequestContextHolder.resetRequestAttributes();
    }


    @Test
    void apply_nullAuthentication() {
        // given
        RequestTemplate requestTemplate = new RequestTemplate();

        // when
        interceptor.apply(requestTemplate);

        // then
        Map<String, Collection<String>> headers = requestTemplate.headers();
        assertNotNull(headers);
        Collection<String> headerValues = headers.get("x-api-key");
        assertNotNull(headerValues);
        assertEquals(1, headerValues.size());
        Optional<String> headerValue = headerValues.stream().findAny();
        assertTrue(headerValue.isPresent());
        assertEquals("api-key-value", headerValue.get());
    }

}



