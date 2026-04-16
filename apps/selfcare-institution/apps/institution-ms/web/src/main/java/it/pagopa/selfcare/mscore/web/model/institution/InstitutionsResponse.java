package it.pagopa.selfcare.mscore.web.model.institution;

import lombok.Data;

import java.util.List;

@Data
public class InstitutionsResponse {

    private List<InstitutionResponse> institutions;
}
