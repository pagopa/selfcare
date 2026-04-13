package it.pagopa.selfcare.onboarding.client.model.institutions;

import lombok.Data;

@Data
public class RowError {

    private Integer row;
    private String cf;
    private String reason;

}
