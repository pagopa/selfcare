package it.pagopa.selfcare.external_api.model.onboarding;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;

@Data
public class PspDataDto {

  @Schema(description = "${swagger.external_api.institutions.model.pspData.businessRegisterNumber}")
  @JsonProperty(required = true)
  @NotBlank
  private String businessRegisterNumber;

  @Schema(description = "${swagger.external_api.institutions.model.pspData.legalRegisterName}")
  @JsonProperty(required = true)
  @NotBlank
  private String legalRegisterName;

  @Schema(description = "${swagger.external_api.institutions.model.pspData.legalRegisterNumber}")
  @JsonProperty(required = true)
  @NotBlank
  private String legalRegisterNumber;

  @Schema(description = "${swagger.external_api.institutions.model.pspData.abiCode}")
  @JsonProperty(required = true)
  @NotBlank
  private String abiCode;

  @Schema(description = "${swagger.external_api.institutions.model.pspData.vatNumberGroup}", example = "false")
  @JsonProperty(required = true)
  @NotNull
  private Boolean vatNumberGroup;

  @Schema(description = "${swagger.external_api.institutions.model.pspData.contractType}")
  private String contractType;

  @Schema(description = "${swagger.external_api.institutions.model.pspData.contractId}")
  private String contractId;

  @Schema(description = "${swagger.external_api.institutions.model.pspData.providerNames}")
  private List<String> providerNames;

  @Schema(description = "${swagger.external_api.institutions.model.pspData.dpoData}")
  @NotNull
  @Valid
  private DpoDataDto dpoData;
}
