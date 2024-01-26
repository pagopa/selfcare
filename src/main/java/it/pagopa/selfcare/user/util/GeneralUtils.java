package it.pagopa.selfcare.user.util;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class GeneralUtils {

    private static final String DELIMITER = ",";

    public static List<String> formatQueryParameterList(List<String> list) {
        if(Objects.nonNull(list) && list.size() == 1 && list.get(0).contains(DELIMITER)) {
            return Arrays.asList(list.get(0).split(DELIMITER));
        }
        return list;
    }
}
