package it.pagopa.selfcare.onboarding.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AggregatesBatchOrchestratorInput {

    private String onboardingId;
    private List<String> aggregateOrchestratorInputs;
    private int currentIndex;
    /** Aggregates failed so far; carried across continueAsNew (defaults to 0 for in-flight inputs). */
    private int failedCount;

    public AggregatesBatchOrchestratorInput(String onboardingId, List<String> aggregateOrchestratorInputs, int currentIndex) {
        this(onboardingId, aggregateOrchestratorInputs, currentIndex, 0);
    }

}
