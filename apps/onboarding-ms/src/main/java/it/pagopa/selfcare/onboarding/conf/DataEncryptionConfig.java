package it.pagopa.selfcare.onboarding.conf;

import io.quarkus.runtime.Startup;
import it.pagopa.selfcare.onboarding.crypto.utils.DataEncryptionUtils;
import jakarta.annotation.PostConstruct;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import jakarta.inject.Singleton;

@Singleton
@Startup
public class DataEncryptionConfig {
    @ConfigProperty(name = "onboarding-ms.data.encryption.key")
    String key;

    @ConfigProperty(name = "onboarding-ms.data.encryption.iv")
    String iv;

    @PostConstruct
    void init() {
        DataEncryptionUtils.setDefaultKey(key);
        DataEncryptionUtils.setDefaultIv(iv);
    }
}