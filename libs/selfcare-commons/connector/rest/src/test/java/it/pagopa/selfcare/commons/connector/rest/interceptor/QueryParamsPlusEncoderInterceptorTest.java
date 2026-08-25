package it.pagopa.selfcare.commons.connector.rest.interceptor;

import feign.RequestTemplate;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Optional;

class QueryParamsPlusEncoderInterceptorTest {

    private static final String PLUS_RAW = "+";
    private static final String PLUS_ENCODED = "%2B";
    private static final String KEY = "key";
    private static final String VALUE_TEMPLATE = "value%svalue";

    private final QueryParamsPlusEncoderInterceptor interceptor = new QueryParamsPlusEncoderInterceptor();


    @Test
    void apply() {
        // given
        RequestTemplate requestTemplate = new RequestTemplate();
        HashMap<String, Collection<String>> queries = new HashMap<>();
        queries.put(KEY, Collections.singletonList(String.format(VALUE_TEMPLATE, PLUS_RAW)));
        requestTemplate.queries(queries);
        // when
        interceptor.apply(requestTemplate);
        // then
        Assertions.assertNotNull(requestTemplate.queries());
        Assertions.assertEquals(1, requestTemplate.queries().size());
        Assertions.assertTrue(requestTemplate.queries().containsKey(KEY));
        Assertions.assertEquals(1, requestTemplate.queries().get(KEY).size());
        Optional<String> value = requestTemplate.queries().get(KEY).stream().findAny();
        Assertions.assertTrue(value.isPresent());
        Assertions.assertEquals(String.format(VALUE_TEMPLATE, PLUS_ENCODED), value.get());
    }
}