package it.pagopa.selfcare.external_api.model.onboarding;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;

@Data
public class ImportContractDto {

    @Schema(description = "${swagger.external_api.importContract.model.fileName}")
    @NotBlank
    private String fileName;

    @Schema(description = "${swagger.external_api.importContract.model.filePath}")
    @NotBlank
    private String filePath;

    @Schema(description = "${swagger.external_api.importContract.model.contractType}")
    @NotBlank
    private String contractType;

    @Schema(description = "${swagger.external_api.institutions.model.onboardingDate}")
    @NotNull
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private OffsetDateTime onboardingDate;

}
