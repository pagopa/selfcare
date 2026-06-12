package it.pagopa.selfcare.dashboard.config;

import it.pagopa.selfcare.dashboard.config.restclient.UserRegistryRestClientConfig;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;

@TestConfiguration
@Import(UserRegistryRestClientConfig.class)
public class UserRegistryRestClientTestConfig {
}
