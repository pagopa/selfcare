package it.pagopa.selfcare.mscore.web.model.delegation;

import it.pagopa.selfcare.mscore.constant.DelegationType;
import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Data
public class DelegationRequest {

    @NotBlank
    private String from;
    @NotBlank
    private String to;
    @NotBlank
    private String institutionFromName;
    @NotBlank
    private String institutionToName;
    @NotBlank
    private String productId;
    @NotNull
    private DelegationType type;

    private String institutionFromRootName;

}
