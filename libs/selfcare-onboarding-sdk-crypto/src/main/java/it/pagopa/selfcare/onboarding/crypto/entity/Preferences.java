package it.pagopa.selfcare.onboarding.crypto.entity;

public class Preferences {
    public String hashAlgorithm;

    public Preferences(String hashAlgorithm) {
        this.hashAlgorithm = hashAlgorithm;
    }

    public String getHashAlgorithm() {
        return hashAlgorithm;
    }

    public void setHashAlgorithm(String hashAlgorithm) {
        this.hashAlgorithm = hashAlgorithm;
    }
}