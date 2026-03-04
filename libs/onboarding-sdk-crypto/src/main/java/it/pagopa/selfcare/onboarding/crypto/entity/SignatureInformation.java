package it.pagopa.selfcare.onboarding.crypto.entity;

public class SignatureInformation {
    private String name;
    private String location;
    private String reason;

    public SignatureInformation(String name, String location, String reason) {
        this.name = name;
        this.location = location;
        this.reason = reason;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
