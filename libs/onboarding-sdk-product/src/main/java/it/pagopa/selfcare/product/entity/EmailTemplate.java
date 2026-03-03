package it.pagopa.selfcare.product.entity;


import it.pagopa.selfcare.onboarding.common.OnboardingStatus;

public class EmailTemplate {

    private String path;

    private String version;

    private OnboardingStatus status;

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public OnboardingStatus getStatus() {
        return status;
    }

    public void setStatus(OnboardingStatus status) {
        this.status = status;
    }
}
