package it.pagopa.selfcare.onboarding.util;

import it.pagopa.selfcare.onboarding.common.InstitutionType;
import it.pagopa.selfcare.onboarding.exception.InvalidRequestException;
import jakarta.ws.rs.core.UriInfo;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

/**
 * Request parameter checks reproducing the 400 contract of the former Spring MVC binding:
 * missing required parameters and values that cannot be converted are bad requests.
 */
@Slf4j
public final class RequestParams {

    private static final String SPRING_WEB_MODEL = "it.pagopa.selfcare.onboarding.web.model.";
    private static final String SPRING_QUERY_PARAMETER =
            "@io.swagger.v3.oas.annotations.Parameter @org.springframework.web.bind.annotation.RequestParam ";
    private static final int CONVERSION_VALUE_LIMIT = 100;

    private RequestParams() {
    }

    /**
     * Maps the type names of the relocated DTOs to the names the Spring BFF published in its binding errors,
     * also when they are nested in a generic descriptor.
     */
    public static String springTypeName(String className) {
        return className
                .replace("it.pagopa.selfcare.onboarding.model.dto.request.", SPRING_WEB_MODEL)
                .replace("it.pagopa.selfcare.onboarding.model.dto.response.", SPRING_WEB_MODEL)
                .replace("it.pagopa.selfcare.onboarding.model.AggregateInstitution", SPRING_WEB_MODEL + "AggregateInstitution")
                .replace("it.pagopa.selfcare.onboarding.model.VerifyType", SPRING_WEB_MODEL + "VerifyType")
                .replace("it.pagopa.selfcare.onboarding.client.model.GPUData",
                        "it.pagopa.selfcare.onboarding.connector.model.onboarding.GPUData");
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

    public static <T> T requiredPart(String name, T value) {
        if (value == null) {
            throw new InvalidRequestException("Required part '" + name + "' is not present.");
        }
        return value;
    }

    public static <T> T requiredBody(T value, String publishedMethod) {
        if (value == null) {
            throw new InvalidRequestException("Required request body is missing: " + publishedMethod);
        }
        return value;
    }

    public static <E extends Enum<E>> E requiredEnum(String name, String value, Class<E> type) {
        return requiredEnum(name, value, type, null);
    }

    public static <E extends Enum<E>> E requiredEnum(String name, String value, Class<E> type, UriInfo uriInfo) {
        if (value == null) {
            throw new InvalidRequestException(
                    "Required request parameter '" + name + "' for method parameter type "
                            + type.getSimpleName() + " is not present");
        }
        E result = optionalEnum(name, value, type, uriInfo);
        if (result == null) {
            throw new InvalidRequestException(
                    "Required request parameter '" + name + "' for method parameter type " + type.getSimpleName()
                            + " is present but converted to null");
        }
        return result;
    }

    public static <E extends Enum<E>> E optionalEnum(String name, String value, Class<E> type) {
        return optionalEnum(name, value, type, null);
    }

    public static <E extends Enum<E>> E optionalEnum(String name, String value, Class<E> type, UriInfo uriInfo) {
        // Spring converts the Optional<InstitutionType> query parameter element-wise: only an empty value is
        // absent, while a blank one is rejected like any other unknown constant.
        boolean optionalParameter = type == InstitutionType.class;
        if (value == null || (optionalParameter ? value.isEmpty() : value.trim().isEmpty())) {
            return null;
        }
        try {
            return Enum.valueOf(type, value.trim());
        } catch (IllegalArgumentException e) {
            log.warn("Invalid constant for request parameter '{}'", name);
            String enumType = springTypeName(type.getName());
            throw new InvalidRequestException(conversionFailure(name, uriInfo) + (optionalParameter ? "java.util.Optional" : enumType)
                    + "'; Failed to convert from type [java.lang.String] to type [" + SPRING_QUERY_PARAMETER + enumType
                    + "] for value [" + conciseValue(value) + "]");
        }
    }

    public static Integer optionalInt(String name, String value) {
        return optionalInt(name, value, null);
    }

    public static Integer optionalInt(String name, String value, UriInfo uriInfo) {
        if (value == null || value.isBlank()) {
            return null;
        }
        if (repeatedQuery(uriInfo, name)) {
            try {
                return integerValue(value);
            } catch (NumberFormatException e) {
                // Spring retries the editor against the comma-delimited array after its first element fails.
                value = stringQuery(uriInfo, name, value);
            }
        }
        try {
            return integerValue(value);
        } catch (NumberFormatException e) {
            log.warn("Invalid number for request parameter '{}'", name);
            throw new InvalidRequestException(conversionFailure(name, uriInfo) + Integer.class.getName() + "'; " + e.getMessage());
        }
    }

    private static Integer integerValue(String value) {
        String number = StringUtils.deleteWhitespace(value);
        int prefix = number.startsWith("-") ? 1 : 0;
        boolean hexadecimal = number.startsWith("0x", prefix) || number.startsWith("0X", prefix)
                || number.startsWith("#", prefix);
        return hexadecimal ? Integer.decode(number) : Integer.valueOf(number);
    }

    private static boolean repeatedQuery(UriInfo uriInfo, String name) {
        List<String> values = uriInfo == null ? null : uriInfo.getQueryParameters().get(name);
        return values != null && values.size() > 1;
    }

    private static String conversionFailure(String name, UriInfo uriInfo) {
        return "Failed to convert value of type '" + (repeatedQuery(uriInfo, name) ? "java.lang.String[]" : "java.lang.String")
                + "' to required type '";
    }

    // Spring truncates the value it echoes in a conversion failure
    private static String conciseValue(String value) {
        return value.length() > CONVERSION_VALUE_LIMIT
                ? value.substring(0, CONVERSION_VALUE_LIMIT) + " (truncated)..." : value;
    }
}
