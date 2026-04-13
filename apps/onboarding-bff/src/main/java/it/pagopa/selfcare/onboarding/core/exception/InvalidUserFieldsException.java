package it.pagopa.selfcare.onboarding.core.exception;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.ToString;

import java.io.Serializable;
import java.util.List;

@Getter
@ToString
public class InvalidUserFieldsException extends RuntimeException {

    private final List<InvalidField> invalidFields;


    public InvalidUserFieldsException() {
        this(List.of());
    }


    public InvalidUserFieldsException(List<InvalidField> invalidFields) {
        super("there are values that do not match with the certified data");
        this.invalidFields = invalidFields;
    }


    @Data
    @AllArgsConstructor
    public static class InvalidField implements Serializable {
        private String name;
        private String reason;
    }

}
