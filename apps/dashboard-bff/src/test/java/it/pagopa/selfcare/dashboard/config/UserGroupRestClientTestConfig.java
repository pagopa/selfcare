package it.pagopa.selfcare.dashboard.config;

import it.pagopa.selfcare.dashboard.config.restclient.UserGroupRestClientConfig;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;

@TestConfiguration
@Import(UserGroupRestClientConfig.class)
public class UserGroupRestClientTestConfig {
}
