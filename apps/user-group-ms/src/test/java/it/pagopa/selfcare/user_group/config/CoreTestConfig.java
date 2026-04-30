package it.pagopa.selfcare.user_group.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;

@TestConfiguration
@Import(CoreConfig.class)
public class CoreTestConfig {
}
