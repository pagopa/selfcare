package it.pagopa.selfcare.product.entity;

import java.util.List;

public class SigningConfiguration {
    /**
     * Total number of signatures required to complete the onboarding.
     */
    private int requiredSignatures;

    /**
     * If true, the system does NOT verify the signer's identity (who signed),
     * but only the technical validity of the signature (format, digest, valid certificate).
     */
    private boolean skipSignerIdentityCheck;

    /**
     * Ordered list of signing steps. The order defines the expected sequence
     */
    private List<SigningStep> steps;

    public int getRequiredSignatures() {
        return requiredSignatures;
    }

    public void setRequiredSignatures(int requiredSignatures) {
        this.requiredSignatures = requiredSignatures;
    }

    public boolean isSkipSignerIdentityCheck() {
        return skipSignerIdentityCheck;
    }

    public void setSkipSignerIdentityCheck(boolean skipSignerIdentityCheck) {
        this.skipSignerIdentityCheck = skipSignerIdentityCheck;
    }

    public List<SigningStep> getSteps() {
        return steps;
    }

    public void setSteps(List<SigningStep> steps) {
        this.steps = steps;
    }
}

