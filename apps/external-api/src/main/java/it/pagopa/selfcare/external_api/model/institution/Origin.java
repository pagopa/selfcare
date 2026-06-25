package it.pagopa.selfcare.external_api.model.institution;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;

public enum Origin {

    MOCK("static"),
    IPA("IPA"),
    INFOCAMERE("INFOCAMERE");

    private final String value;

    Origin(String value) {
        this.value = value;
    }

    @Override
    @JsonValue
    public String toString() {
        return value;
    }

    @JsonCreator
    public static Origin fromValue(String value) {
        return Arrays.stream(values())
                .filter(origin -> origin.toString().equals(value))
                .findAny()
                .orElseThrow();
    }
}
