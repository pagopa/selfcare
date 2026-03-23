package it.pagopa.selfcare.party.registry_proxy.connector.rest.interceptor;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class IPAOpenDataInterceptor implements RequestInterceptor {

    public IPAOpenDataInterceptor() {
        log.trace("Initializing {}", IPAOpenDataInterceptor.class.getSimpleName());
    }


    @Override
    public void apply(RequestTemplate template) {
        log.trace("apply start");
        log.debug("RequestTemplate template = {}", template);
        template.query("bom", "True");
        log.trace("apply end");
    }

}
