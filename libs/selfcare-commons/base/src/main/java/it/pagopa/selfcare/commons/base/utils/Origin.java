package it.pagopa.selfcare.commons.base.utils;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import org.springframework.util.StringUtils;

import java.util.Arrays;

public enum Origin {
    MOCK("MOCK"),
    IPA("IPA"),
    SELC("SELC"),
    UNKNOWN("UNKNOWN"),
    ADE("ADE"),
    INFOCAMERE("INFOCAMERE"),
    ANAC("ANAC"),
    IVASS("IVASS");

    private final String value;

    Origin(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    @Override
    @JsonValue
    public String toString() {
        return value;
    }

    @JsonCreator
    public static Origin fromValue(String value) {
        if (StringUtils.hasText(value)) {
            return Arrays.stream(values())
                    .filter(origin -> origin.toString().equals(value))
                    .findAny()
                    .orElseThrow(() -> new IllegalArgumentException("Valid value for Origin are: IPA, INFOCAMERE, SELC or static"));
        } else {
            return Origin.UNKNOWN;
        }
    }
}
