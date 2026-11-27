package it.pagopa.selfcare.user.controller.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class SendInstitutionMailDto {

    @NotNull(message = "type is required")
    private InstitutionEmailType type;

    @NotEmpty(message = "productIds is required")
    private List<String> productIds;

}
