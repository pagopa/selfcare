package it.pagopa.selfcare.onboarding.util;

import org.owasp.encoder.Encode;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

public final class LogUtils {

    public static final Marker CONFIDENTIAL_MARKER = MarkerFactory.getMarker("CONFIDENTIAL");

    private LogUtils() {
    }

    public static String sanitize(Object value) {
        return value == null ? null : Encode.forJava(String.valueOf(value));
    }
}
