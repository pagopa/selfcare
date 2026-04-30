package it.pagopa.selfcare.user.controller.request;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

@Data
public class UpdateDescriptionDto {

    @NotEmpty(message = "institution's description is required")
    private String institutionDescription;
    private String institutionRootName;

}
