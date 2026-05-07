package it.pagopa.selfcare.dashboard.config;

import it.pagopa.selfcare.dashboard.config.restclient.PagoPABackOfficeRestClientConfig;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;

@TestConfiguration
@Import(PagoPABackOfficeRestClientConfig.class)
public class PagoPABackOfficeRestClientTestConfig {
}