package it.pagopa.selfcare.party.registry_proxy.connector.rest.interceptor;

import feign.RequestTemplate;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class IPAOpenDataInterceptorTest {

    private final IPAOpenDataInterceptor interceptor = new IPAOpenDataInterceptor();

    @Test
    void apply() {
        // given
        RequestTemplate requestTemplate = new RequestTemplate();
        requestTemplate.query("key1", "value1");
        final int queryParamsCount = requestTemplate.queries().size();
        // when
        interceptor.apply(requestTemplate);
        // then
        assertEquals(queryParamsCount + 1, requestTemplate.queries().size());
        assertTrue(requestTemplate.queries().containsKey("bom"));
        assertNotNull(requestTemplate.queries().get("bom"));
        assertEquals(1, requestTemplate.queries().get("bom").size());
        assertEquals("True", requestTemplate.queries().get("bom").iterator().next());
    }
}