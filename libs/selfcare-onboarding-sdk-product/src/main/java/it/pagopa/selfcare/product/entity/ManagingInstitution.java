package it.pagopa.selfcare.product.entity;

public class ManagingInstitution {

    private String institutionId;

    private String description;

    /**
     * Signing step to which this institution is associated.
     * Corresponds to the 'order' field of SigningStep.
     */
    private int signingStep;

    public String getInstitutionId() {
        return institutionId;
    }

    public void setInstitutionId(String institutionId) {
        this.institutionId = institutionId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getSigningStep() {
        return signingStep;
    }

    public void setSigningStep(int signingStep) {
        this.signingStep = signingStep;
    }
}

