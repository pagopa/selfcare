package it.pagopa.selfcare.user.controller.request;

import it.pagopa.selfcare.onboarding.common.PartyRole;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SendMailDto {

    @NotEmpty(message = "userMailUuid is required")
    private String userMailUuid;

    @NotEmpty(message = "institutionName is required")
    private String institutionName;

    @NotEmpty(message = "productId is required")
    private String productId;

    @NotNull(message = "role is required")
    private PartyRole role;

    @NotNull(message = "userRequestUid is required")
    private String userRequestUid;

}
