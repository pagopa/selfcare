package it.pagopa.selfcare.dashboard.model.delegation;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;

@Data
public class DelegationId {
    @NotBlank
    private String id;

}
