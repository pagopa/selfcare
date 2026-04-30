package it.pagopa.selfcare.mscore.connector.email.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Configuration
@PropertySource("classpath:config/email.properties")
public class EmailBaseConfig {
}
