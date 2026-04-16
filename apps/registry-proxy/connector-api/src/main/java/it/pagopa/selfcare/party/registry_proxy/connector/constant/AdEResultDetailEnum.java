package it.pagopa.selfcare.party.registry_proxy.connector.constant;

import com.fasterxml.jackson.annotation.JsonValue;

public enum AdEResultDetailEnum {
    XX00("XX00"),

    XX01("XX01"),

    XX02("XX02"),

    XX0A("XX0A"),

    XX03("XX03"),

    XX04("XX04"),

    XXXX("XXXX");

    private final String value;

    AdEResultDetailEnum(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    public static AdEResultDetailEnum fromValue(String code) {
        for (AdEResultDetailEnum b : AdEResultDetailEnum.values()) {
            if (b.value.equals(code)) {
                return b;
            }
        }
        return AdEResultDetailEnum.XXXX;
    }

}
