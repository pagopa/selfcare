package it.pagopa.selfcare.onboarding.model;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.ToString;

@ToString
@Data
public class RowError {
    @NotNull
    private Integer row;

    private String cf;
    private String reason;

    public RowError(Integer row, String cf, String reason) {
        this.row = row;
        this.cf = cf;
        this.reason = reason;
    }
}
