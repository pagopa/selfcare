package it.pagopa.selfcare.commons.connector.rest.interceptor;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

@Slf4j
public class QueryParamsPlusEncoderInterceptor implements RequestInterceptor {

    private static final String PLUS_RAW = "+";
    private static final String PLUS_ENCODED = "%2B";

    @Override
    public void apply(RequestTemplate template) {
        final Map<String, Collection<String>> queriesPlusEncoded = new HashMap<>();
        template.queries().forEach((key, value) -> queriesPlusEncoded.put(key, value.stream()
                .map(paramValue -> paramValue.replace(PLUS_RAW, PLUS_ENCODED))
                .collect(ArrayList::new, ArrayList::add, ArrayList::addAll)));
        template.queries(null);
        template.queries(queriesPlusEncoded);
    }

}
