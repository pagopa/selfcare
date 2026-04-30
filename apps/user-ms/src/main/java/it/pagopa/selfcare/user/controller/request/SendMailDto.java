package it.pagopa.selfcare.user.controller.request;

import it.pagopa.selfcare.user.util.product.ProductId;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

@Data
public class SendMailDto {

    @NotEmpty(message = "userMailUuid is required")
    private String userMailUuid;

    @NotEmpty(message = "institutionName is required")
    private String institutionName;

    @NotEmpty(message = "productId is required")
    @ProductId
    private String productId;

}
