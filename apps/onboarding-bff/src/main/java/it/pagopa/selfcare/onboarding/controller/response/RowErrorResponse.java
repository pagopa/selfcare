package it.pagopa.selfcare.onboarding.controller.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class RowErrorResponse {

    @JsonProperty("riga")
    private Integer row;

    @JsonProperty("codice fiscale")
    private String cf;

    @JsonProperty("errore")
    private String reason;
}
