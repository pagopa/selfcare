package it.pagopa.selfcare.document.crypto.entity;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class Preferences {
    public String hashAlgorithm;

    public Preferences(String hashAlgorithm) {
        this.hashAlgorithm = hashAlgorithm;
    }

}
