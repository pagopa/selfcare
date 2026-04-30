package it.pagopa.selfcare.user.util;

import it.pagopa.selfcare.user.exception.InvalidRequestException;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class GeneralUtils {

    private static final String DELIMITER = ",";

    private GeneralUtils() {}

    public static List<String> formatQueryParameterList(List<String> list) {
        if(Objects.nonNull(list) && list.size() == 1 && list.get(0).contains(DELIMITER)) {
            return Arrays.asList(list.get(0).split(DELIMITER));
        }
        return list;
    }

    /**
     * Convert a list of string in a list of values of an enum.
     * Throw an InvalidRequestException when a string doesn't match a value inside the enum.
     *
     * @param values a list of strings
     * @param enumClass the enum class
     * @return a list of enum values
     * @param <T> the enum type
     */
    public static <T extends Enum<T>> List<T> parseEnumList(List<String> values, Class<T> enumClass) {
        return values != null ? values.stream().map(v -> {
            try {
                return Enum.valueOf(enumClass, v);
            } catch (IllegalArgumentException ex) {
                throw new InvalidRequestException(String.format("Invalid value %s for %s", v, enumClass.getSimpleName()));
            }
        }).toList() : Collections.emptyList();
    }

}
