package it.pagopa.selfcare.onboarding.controller.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class OnboardingImportRequest {

  @NotNull(message = "institutionData is required")
  @Valid
  private InstitutionImportRequest institution;

  @NotEmpty(message = "productId is required")
  private String productId;

  private List<UserRequest> users;

  @NotNull
  private OnboardingImportContract contractImported;

  private BillingPaRequest billing;

  private boolean forceImport;

  private Boolean sendMailForImport;

}
