package it.pagopa.selfcare.mscore.web.model.delegation;

import it.pagopa.selfcare.mscore.constant.DelegationType;
import it.pagopa.selfcare.mscore.web.util.EncryptIfTaxCode;
import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Data
public class DelegationRequestFromTaxcode {

    @EncryptIfTaxCode
    @NotBlank
    private String fromTaxCode;
    @EncryptIfTaxCode
    @NotBlank
    private String toTaxCode;
    @NotBlank
    private String institutionFromName;
    @NotBlank
    private String institutionToName;
    @NotBlank
    private String productId;
    @NotNull
    private DelegationType type;
    private String fromSubunitCode;
    private String toSubunitCode;

}
