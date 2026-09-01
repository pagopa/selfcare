package it.pagopa.selfcare.commons.connector.soap.aruba.sign.config;

import org.springframework.boot.autoconfigure.condition.AllNestedConditions;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

public class ArubaCryptoCondition extends AllNestedConditions {

    public ArubaCryptoCondition() {
        super(ConfigurationPhase.REGISTER_BEAN);
    }

    @ConditionalOnProperty(
            prefix = "crypto.pkcs7",
            value = "source",
            havingValue = "aruba"
    )
    public static class ArubaCryptoEnabled {
    }
}

