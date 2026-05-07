package it.pagopa.selfcare.dashboard.config;

import it.pagopa.selfcare.dashboard.config.WebConfig;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;

@TestConfiguration
@Import(WebConfig.class)
public class WebTestConfig {
}