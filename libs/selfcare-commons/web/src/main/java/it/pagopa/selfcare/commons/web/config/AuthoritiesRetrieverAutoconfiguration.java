package it.pagopa.selfcare.commons.web.config;

import it.pagopa.selfcare.commons.web.security.AuthoritiesRetriever;
import it.pagopa.selfcare.commons.web.security.NoAuthoritiesRetriever;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@ConditionalOnMissingBean(AuthoritiesRetriever.class)
public class AuthoritiesRetrieverAutoconfiguration {

    @Bean
    public AuthoritiesRetriever authoritiesRetriever() {
        return new NoAuthoritiesRetriever();
    }

}
