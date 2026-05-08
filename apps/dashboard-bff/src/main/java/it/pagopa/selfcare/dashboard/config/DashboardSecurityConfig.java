package it.pagopa.selfcare.dashboard.config;

import it.pagopa.selfcare.commons.web.config.SecurityConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;

@Slf4j
@Configuration
@EnableWebSecurity
@PropertySource("classpath:config/token-exchange.properties")
@Import(SecurityConfig.class)
class DashboardSecurityConfig {
}