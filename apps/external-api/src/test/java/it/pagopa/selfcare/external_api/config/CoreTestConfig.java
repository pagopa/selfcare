package it.pagopa.selfcare.external_api.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;

@TestConfiguration
@Import({CoreConfig.class})
public class CoreTestConfig {
    public CoreTestConfig() {
    }
}
