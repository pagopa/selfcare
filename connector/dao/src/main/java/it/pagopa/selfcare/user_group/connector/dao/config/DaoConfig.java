package it.pagopa.selfcare.user_group.connector.dao.config;

import it.pagopa.selfcare.user_group.connector.dao.auditing.SpringSecurityAuditorAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.mongodb.config.EnableMongoAuditing;

@Configuration
@EnableMongoAuditing(modifyOnCreate = false)
@PropertySource("classpath:config/dao-config.properties")
class DaoConfig {

    @Bean
    public AuditorAware<String> myAuditorProvider() {
        return new SpringSecurityAuditorAware();
    }

}
