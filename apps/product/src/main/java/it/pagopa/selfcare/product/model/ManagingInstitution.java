package it.pagopa.selfcare.product.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ManagingInstitution {

    private String institutionId;

    private String description;

    /**
     * Signing step to which this institution is associated.
     * Corresponds to the 'order' field of SigningStep.
     */
    private int signingStep;
}
