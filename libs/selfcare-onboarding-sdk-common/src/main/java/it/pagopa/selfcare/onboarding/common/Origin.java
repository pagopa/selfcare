package it.pagopa.selfcare.onboarding.common;

import java.util.Arrays;
import java.util.Objects;

public enum Origin {
    MOCK("MOCK"),
    IPA("IPA"),
    SELC("SELC"),
    ANAC("ANAC"),
    UNKNOWN("UNKNOWN"),
    ADE("ADE"),
    INFOCAMERE("INFOCAMERE"),
    IVASS("IVASS"),
    PDND_INFOCAMERE("PDND_INFOCAMERE");

    private final String value;

    Origin(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return value;
    }


    public static Origin fromValue(String value) {
        if(Objects.nonNull(value)) {
            return Arrays.stream(values())
                    .filter(origin -> origin.toString().equals(value))
                    .findAny()
                    .orElseThrow(() -> new IllegalArgumentException("Valid value for Origin are: IPA, INFOCAMERE, SELC or static"));
        }else{
            return Origin.UNKNOWN;
        }
    }
    
}