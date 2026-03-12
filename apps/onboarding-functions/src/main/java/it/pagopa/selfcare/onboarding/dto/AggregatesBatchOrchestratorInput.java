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

}
