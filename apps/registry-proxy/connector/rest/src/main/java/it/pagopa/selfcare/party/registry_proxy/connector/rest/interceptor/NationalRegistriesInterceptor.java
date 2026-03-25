package it.pagopa.selfcare.party.registry_proxy.connector.rest.interceptor;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class NationalRegistriesInterceptor implements RequestInterceptor {
    @Override
    public void apply(RequestTemplate requestTemplate) {
        log.trace("apply start");
        log.debug("RequestTemplate template = {}", requestTemplate);
        log.trace("apply end");
    }
}
