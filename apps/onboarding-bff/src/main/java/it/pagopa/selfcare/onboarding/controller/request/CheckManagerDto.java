package it.pagopa.selfcare.onboarding.controller.request;

import io.swagger.v3.oas.annotations.media.Schema;
import it.pagopa.selfcare.onboarding.common.InstitutionType;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.util.UUID;

@Data
public class CheckManagerDto {

    @Schema(description = "${openapi.onboarding.institutions.model.userId}", required = true)
    @NotNull
    private UUID userId;

    @Schema(description = "${openapi.onboarding.institutions.model.institutionType}")
    private InstitutionType institutionType;

    @Schema(description = "${openapi.onboarding.institutions.model.origin}")
    private String origin;

    @Schema(description = "${openapi.onboarding.institutions.model.originId}")
    private String originId;

    @Schema(description = "${openapi.onboarding.product.model.id}", required = true)
    @NotNull
    private String productId;

    @Schema(description = "${openapi.onboarding.institutions.model.taxCode}")
    private String taxCode;

    @Schema(description = "${openapi.onboarding.institutions.model.subunitCode}")
    private String subunitCode;

}
