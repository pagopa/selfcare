package it.pagopa.selfcare.auth.util;

import it.pagopa.selfcare.auth.exception.ForbiddenException;
import it.pagopa.selfcare.auth.exception.InternalException;
import it.pagopa.selfcare.auth.exception.InvalidRequestException;
import it.pagopa.selfcare.auth.exception.ResourceNotFoundException;
import jakarta.ws.rs.WebApplicationException;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeoutException;

public class GeneralUtils {

  private static final String DELIMITER = ",";

  private GeneralUtils() {}

  public static List<String> formatQueryParameterList(List<String> list) {
    if (Objects.nonNull(list) && list.size() == 1 && list.get(0).contains(DELIMITER)) {
      return Arrays.asList(list.get(0).split(DELIMITER));
    }
    return list;
  }

  /**
   * Convert a list of string in a list of values of an enum. Throw an InvalidRequestException when
   * a string doesn't match a value inside the enum.
   *
   * @param values a list of strings
   * @param enumClass the enum class
   * @return a list of enum values
   * @param <T> the enum type
   */
  public static <T extends Enum<T>> List<T> parseEnumList(List<String> values, Class<T> enumClass) {
    return values != null
        ? values.stream()
            .map(
                v -> {
                  try {
                    return Enum.valueOf(enumClass, v);
                  } catch (IllegalArgumentException ex) {
                    throw new InvalidRequestException(
                        String.format("Invalid value %s for %s", v, enumClass.getSimpleName()));
                  }
                })
            .toList()
        : Collections.emptyList();
  }

  public static boolean checkIfIsRetryableException(Throwable throwable) {
    return throwable instanceof TimeoutException
        || (throwable instanceof WebApplicationException webApplicationException
            && webApplicationException.getResponse().getStatus() == 429);
  }

  public static Exception extractExceptionFromWebAppException(Throwable throwable) {
    if (throwable instanceof WebApplicationException webApplicationException) {
      return switch (webApplicationException.getResponse().getStatus()) {
        case 404 ->
            new ResourceNotFoundException("Not Found:" + webApplicationException.getMessage());
        case 403 -> new ForbiddenException("Forbidden:" + webApplicationException.getMessage());
        default ->
            new InternalException("Internal server error:" + webApplicationException.getMessage());
      };
    }
    return new InternalException("Internal server error:" + throwable.getMessage());
  }
}
