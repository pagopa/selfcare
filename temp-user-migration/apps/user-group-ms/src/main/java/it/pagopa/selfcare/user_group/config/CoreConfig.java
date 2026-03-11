package it.pagopa.selfcare.user_group.config;

import it.pagopa.selfcare.user_group.auditing.SpringSecurityAuditorAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.data.domain.AuditorAware;

@Configuration
@PropertySource("classpath:config/core-config.properties")
public class CoreConfig {
    @Bean
    public AuditorAware<String> myAuditorProvider() {
        return new SpringSecurityAuditorAware();
    }
}
