package it.pagopa.selfcare.external_api.client.config;

import it.pagopa.selfcare.commons.tenant.TenantConfiguration;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;

@TestConfiguration
@Import({UserRegistryRestClientConfig.class, TenantConfiguration.class})
public class UserRegistryRestClientTestConfig {
}
