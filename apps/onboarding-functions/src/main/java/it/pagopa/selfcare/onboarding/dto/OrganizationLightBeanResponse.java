package it.pagopa.selfcare.onboarding.dto;

public class OrganizationLightBeanResponse {
    private boolean alreadyRegistered;
    private OrganizationResponse organization;

    public boolean isAlreadyRegistered() {
        return alreadyRegistered;
    }

    public void setAlreadyRegistered(boolean alreadyRegistered) {
        this.alreadyRegistered = alreadyRegistered;
    }

    public OrganizationResponse getOrganization() {
        return organization;
    }

    public void setOrganization(OrganizationResponse organization) {
        this.organization = organization;
    }

    @Override
    public String toString() {
        return "OrganizationLightBeanResponse{" +
                "alreadyRegistered=" + alreadyRegistered +
                ", organization=" + organization +
                '}';
    }
}
