package it.pagopa.selfcare.onboarding.controller.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class GetInstitutionRequest {
    @NotNull @NotEmpty
    private List<String> institutionIds;
}
