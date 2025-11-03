package it.pagopa.selfcare.iam.util;

import io.quarkus.runtime.Startup;
import it.pagopa.selfcare.onboarding.crypto.utils.DataEncryptionUtils;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Singleton;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.util.Optional;

@Singleton
@Startup
public class DataEncryptionConfig {
    @ConfigProperty(name = "selfcare.data.encryption.key")
    String key;

    @ConfigProperty(name = "selfcare.data.encryption.iv")
    String iv;

    @PostConstruct
    void init() {
        DataEncryptionUtils.setDefaultKey(key);
        DataEncryptionUtils.setDefaultIv(iv);
    }

    public static String encrypt(String value) {
      return Optional.ofNullable(value)
        .filter(v -> !v.isEmpty())
        .map(DataEncryptionUtils::encrypt)
        .orElse("");
    }

  public static String decrypt(String value) {
    return Optional.ofNullable(value)
      .filter(v -> !v.isEmpty())
      .map(DataEncryptionUtils::decrypt)
      .orElse("");
  }
}