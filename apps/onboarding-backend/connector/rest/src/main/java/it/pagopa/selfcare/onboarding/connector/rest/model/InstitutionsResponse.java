package it.pagopa.selfcare.onboarding.connector.rest.model;

import lombok.Data;

import java.util.List;

@Data
public class InstitutionsResponse {

    private List<InstitutionResponse> institutions;
}
