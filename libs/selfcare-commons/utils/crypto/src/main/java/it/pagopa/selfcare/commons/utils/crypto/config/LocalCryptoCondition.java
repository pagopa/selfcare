package it.pagopa.selfcare.commons.utils.crypto.config;

import org.springframework.boot.autoconfigure.condition.AllNestedConditions;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

public class LocalCryptoCondition extends AllNestedConditions {

    public LocalCryptoCondition() {
        super(ConfigurationPhase.REGISTER_BEAN);
    }

    @ConditionalOnProperty(
            prefix = "crypto.pkcs7",
            value = "source",
            havingValue = "local"
    )
    public static class LocalCryptoEnabled {
    }
}

