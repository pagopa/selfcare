package it.pagopa.selfcare.user.controller.request;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

@Data
public class SendMailDto {

    @NotEmpty(message = "userMailUuid is required")
    private String userMailUuid;

    @NotEmpty(message = "institutionName is required")
    private String institutionName;

    @NotEmpty(message = "productId is required")
    private String productId;

}
