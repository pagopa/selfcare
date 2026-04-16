package it.pagopa.selfcare.onboarding.model;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class VerifyAggregateResponse {
    private List<Aggregate> aggregates = new ArrayList<>();
    private List<RowError> errors = new ArrayList<>();


}
