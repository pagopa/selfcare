package it.pagopa.selfcare.onboarding.entity;

import it.pagopa.selfcare.onboarding.crypto.utils.DataEncryptionUtils;

import java.util.Optional;

import lombok.Data;

@Data
public class Payment {
    private String holder;
    private String iban;

    public String retrieveEncryptedHolder() {
        return Optional.ofNullable(holder)
                .map(DataEncryptionUtils::decrypt)
                .orElse("");
    }

    public String retrieveEncryptedIban() {
        return Optional.ofNullable(iban)
                .map(DataEncryptionUtils::decrypt)
                .orElse("");
    }

}
