package it.pagopa.selfcare.onboarding.util;

import it.pagopa.selfcare.onboarding.exception.InvalidRequestException;
import jakarta.ws.rs.core.UriInfo;
import java.util.List;
import java.util.Locale;

/**
 * Request parameter checks reproducing the 400 contract of the former Spring MVC binding:
 * missing required parameters and values that cannot be converted are bad requests.
 */
public final class RequestParams {

    private RequestParams() {
    }

    /** Spring binds repeated String query parameters as one comma-delimited value, not the first item. */
    public static String stringQuery(UriInfo uriInfo, String name, String boundValue) {
        if (uriInfo == null) {
            return boundValue;
        }
        List<String> values = uriInfo.getQueryParameters().get(name);
        return values == null || values.size() < 2 ? boundValue : String.join(",", values);
    }

    public static String requiredQuery(String name, String value) {
        if (value == null) {
            throw new InvalidRequestException(
                    "Required request parameter '" + name + "' for method parameter type String is not present");
        }
        return value;
    }

    public static String requiredHeader(String name, String value) {
        if (value == null) {
            throw new InvalidRequestException(
                    "Required request header '" + name + "' for method parameter type String is not present");
        }
        return value;
    }

    public static <T> T requiredPart(String name, T value) {
        if (value == null) {
            throw new InvalidRequestException("Required part '" + name + "' is not present.");
        }
        return value;
    }

    public static <T> T requiredBody(T value) {
        if (value == null) {
            throw new InvalidRequestException("Required request body is missing");
        }
        return value;
    }

    public static <E extends Enum<E>> E requiredEnum(String name, String value, Class<E> type) {
        if (value == null) {
            throw new InvalidRequestException(
                    "Required request parameter '" + name + "' for method parameter type "
                            + type.getSimpleName() + " is not present");
        }
        E result = optionalEnum(name, value, type);
        if (result == null) {
            throw new InvalidRequestException(
                    "Required request parameter '" + name + "' for method parameter type " + type.getSimpleName()
                            + " is present but converted to null");
        }
        return result;
    }

    public static <E extends Enum<E>> E optionalEnum(String name, String value, Class<E> type) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        try {
            return Enum.valueOf(type, value.trim());
        } catch (IllegalArgumentException e) {
            throw new InvalidRequestException(typeMismatch(name, type.getName(), value));
        }
    }

    public static Integer optionalInt(String name, String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            StringBuilder compact = new StringBuilder();
            value.chars().filter(c -> !Character.isWhitespace(c)).forEach(c -> compact.append((char) c));
            String number = compact.toString();
            int prefix = number.startsWith("-") || number.startsWith("+") ? 1 : 0;
            boolean hexadecimal = number.startsWith("0x", prefix) || number.startsWith("0X", prefix)
                    || number.startsWith("#", prefix);
            return hexadecimal ? Integer.decode(number) : Integer.valueOf(number);
        } catch (NumberFormatException e) {
            throw new InvalidRequestException(typeMismatch(name, Integer.class.getName(), value));
        }
    }

    public static Boolean optionalBoolean(String name, String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "true", "on", "yes", "1" -> Boolean.TRUE;
            case "false", "off", "no", "0" -> Boolean.FALSE;
            default -> throw new InvalidRequestException(typeMismatch(name, Boolean.class.getName(), value));
        };
    }

    private static String typeMismatch(String name, String requiredType, String value) {
        return "Method parameter '" + name + "': Failed to convert value of type 'java.lang.String' to required type '"
                + requiredType + "'; Failed to convert from type [java.lang.String] to type [" + requiredType
                + "] for value [" + value + "]";
    }
}
