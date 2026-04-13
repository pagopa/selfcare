package it.pagopa.selfcare.onboarding.model;

import it.pagopa.selfcare.onboarding.client.model.institutions.AggregateResult;
import lombok.Data;

import java.util.List;

@Data
public class VerifyAggregatesResponse {
    private List<AggregateResult> aggregates ;
    private List<RowErrorResponse> errors;
}
