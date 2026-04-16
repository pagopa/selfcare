package it.pagopa.selfcare.party.registry_proxy.web.config;

import it.pagopa.selfcare.onboarding.crypto.utils.DataEncryptionUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

@Component
public class DataEncryptionConfig {

    @Value("${app.data.encryption.key}")
    String key;

    @Value("${app.data.encryption.iv}")
    String iv;

    @PostConstruct
    void init() {
        DataEncryptionUtils.setDefaultKey(key);
        DataEncryptionUtils.setDefaultIv(iv);
    }
}

