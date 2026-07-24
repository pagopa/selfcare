package it.pagopa.selfcare.onboarding.web.model;


import io.swagger.v3.oas.annotations.media.Schema;
import it.pagopa.selfcare.onboarding.common.InstitutionType;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.util.UUID;

@Data
public class CheckManagerDto {

    @Schema(description = "${swagger.onboarding.institutions.model.userId}", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull
    private UUID userId;

    @Schema(description = "${swagger.onboarding.institutions.model.institutionType}")
    private InstitutionType institutionType;

    @Schema(description = "${swagger.onboarding.institutions.model.origin}")
    private String origin;

    @Schema(description = "${swagger.onboarding.institutions.model.originId}")
    private String originId;

    @Schema(description = "${swagger.onboarding.product.model.id}", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull
    private String productId;

    @Schema(description = "${swagger.onboarding.institutions.model.taxCode}")
    private String taxCode;

    @Schema(description = "${swagger.onboarding.institutions.model.subunitCode}")
    private String subunitCode;

}
