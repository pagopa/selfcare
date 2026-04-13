package it.pagopa.selfcare.onboarding.client.model.institutions;

import lombok.Data;

import java.util.List;

@Data
public class VerifyAggregateResult {
    private List<AggregateResult> aggregates ;
    private List<RowError> errors;
}
