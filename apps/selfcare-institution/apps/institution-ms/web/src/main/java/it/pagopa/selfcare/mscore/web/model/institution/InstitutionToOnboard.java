package it.pagopa.selfcare.mscore.web.model.institution;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class InstitutionToOnboard {

    @JsonProperty("cfImpresa")
    private String id;

    @JsonProperty("denominazione")
    private String description;
}
