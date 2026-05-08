package it.pagopa.selfcare.dashboard.utils;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.UUID;

public class EncodingUtils {

    private EncodingUtils() {}

    public static boolean isUrlEncoded(String value) {
        if (value == null || value.isEmpty()) {
            return true;
        }

        try {
            final String decoded = URLDecoder.decode(value, StandardCharsets.UTF_8);
            final String reencoded = URLEncoder.encode(decoded, StandardCharsets.UTF_8);
            final String normalizedValue = value.replace("%20", "+");
            return normalizedValue.equals(reencoded);
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }

    public static boolean isUUID(String value) {
        return Optional.ofNullable(value).map(v -> {
            try {
                UUID.fromString(v);
                return true;
            } catch (IllegalArgumentException ex) {
                return false;
            }
        }).orElse(false);
    }

    public static UUID toUUIDOrNull(String value) {
        return Optional.ofNullable(value).map(v -> {
            try {
                return UUID.fromString(v);
            } catch (IllegalArgumentException ex) {
                return null;
            }
        }).orElse(null);
    }

}
