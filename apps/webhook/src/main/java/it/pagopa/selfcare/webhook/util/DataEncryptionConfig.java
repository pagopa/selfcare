package it.pagopa.selfcare.webhook.util;

import io.quarkus.runtime.Startup;
import it.pagopa.selfcare.onboarding.crypto.utils.DataEncryptionUtils;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Singleton;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.eclipse.microprofile.config.inject.ConfigProperty;

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

  public static Map<String, String> encrypt(Map<String, String> input) {
    if (input == null || input.isEmpty()) {
      return Map.of();
    }
    return input.entrySet().stream()
        .collect(
            Collectors.toUnmodifiableMap(Map.Entry::getKey, entry -> encrypt(entry.getValue())));
  }

  public static Map<String, String> decrypt(Map<String, String> input) {
    if (input == null || input.isEmpty()) {
      return Map.of();
    }
    return input.entrySet().stream()
        .collect(
            Collectors.toUnmodifiableMap(Map.Entry::getKey, entry -> decrypt(entry.getValue())));
  }
}
