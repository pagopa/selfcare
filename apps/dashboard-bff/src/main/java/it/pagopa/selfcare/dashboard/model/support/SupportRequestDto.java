package it.pagopa.selfcare.dashboard.model.support;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import it.pagopa.selfcare.dashboard.validator.UrlEncoded;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SupportRequestDto {

    @Schema(description = "${swagger.dashboard.support.model.email}")
    @JsonProperty(required = true)
    @Email @NotBlank
    private String email;

    @Schema(description = "${swagger.dashboard.support.model.productId}")
    private String productId;

    @Schema(description = "${swagger.dashboard.support.model.userId}")
    private String userId;

    @Schema(description = "${swagger.dashboard.support.model.institutionId}")
    private String institutionId;

    @Schema(description = "${swagger.dashboard.support.model.data}")
    @UrlEncoded
    private String data;

}
