package it.pagopa.selfcare.onboarding.controller.request;

import it.pagopa.selfcare.onboarding.common.InstitutionType;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.util.UUID;

@Data
public class CheckManagerRequest {

    @NotEmpty(message = "productId is required")
    private String productId;

    private InstitutionType institutionType;

    private String subunitCode;

    private String origin;

    private String originId;

    private String taxCode;

    @Schema(type = SchemaType.STRING, format = "uuid", description = "User UUID")
    @NotNull
    private UUID userId;

}
