package it.pagopa.selfcare.product.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SigningStep {
    /**
     * Progression number of the step (1, 2, 3, ...).
     * Determines the order in which signatures must be applied.
     */
    private int order;

    private String label;

    /**
     * Indicates whether this is the last step in the signing flow.
     * Only the step with isFinal=true causes the transition to COMPLETED.
     */
    private boolean isFinal;
}
