package it.pagopa.selfcare.party.registry_proxy.connector.rest.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.AllNestedConditions;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

@Slf4j
public class OpenDataMockEnabledCondition extends AllNestedConditions {

    public OpenDataMockEnabledCondition() {
        super(ConfigurationPhase.REGISTER_BEAN);
        log.trace("Initializing {}", OpenDataMockEnabledCondition.class.getSimpleName());
    }

    @ConditionalOnProperty(prefix = "open-data.mock", name = "enabled", havingValue = "true")
    public static class OpenDataMockEnabled {
    }

}