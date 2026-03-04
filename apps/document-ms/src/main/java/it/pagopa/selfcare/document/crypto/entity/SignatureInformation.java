package it.pagopa.selfcare.document.crypto.entity;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class SignatureInformation {
    private String name;
    private String location;
    private String reason;

    public SignatureInformation(String name, String location, String reason) {
        this.name = name;
        this.location = location;
        this.reason = reason;
    }

}
