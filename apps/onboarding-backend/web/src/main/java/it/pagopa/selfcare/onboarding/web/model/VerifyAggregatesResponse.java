package it.pagopa.selfcare.onboarding.web.model;

import it.pagopa.selfcare.onboarding.connector.model.institutions.AggregateResult;
import lombok.Data;

import java.util.List;

@Data
public class VerifyAggregatesResponse {
    private List<AggregateResult> aggregates ;
    private List<RowErrorResponse> errors;
}
