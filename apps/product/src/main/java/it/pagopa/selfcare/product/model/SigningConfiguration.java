package it.pagopa.selfcare.product.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
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
}
