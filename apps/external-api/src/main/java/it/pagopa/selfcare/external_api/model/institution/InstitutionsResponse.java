package it.pagopa.selfcare.external_api.model.institution;

import lombok.Data;

import java.util.List;

@Data
public class InstitutionsResponse {

    private List<InstitutionResponse> institutions;
}
