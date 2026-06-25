package it.pagopa.selfcare.external_api.model.onboarding;


import it.pagopa.selfcare.onboarding.generated.openapi.v1.dto.*;
import java.util.List;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class OnboardingAggregatorImportData {

  @NotEmpty(message = "productId is required")
  private String productId;

  @NotEmpty(message = "at least one user is required")
  private List<UserRequest> users;

  private List<AggregateInstitutionRequest> aggregates;

  private Boolean isAggregator;

  private String pricingPlan;

  private Boolean signContract;

  @NotNull(message = "institutionData is required")
  @Valid
  private InstitutionBaseRequest institution;

  @Valid private BillingRequest billing;
  @Valid private AdditionalInformationsDto additionalInformations;
  @Valid private GPUData gpuData;

  @Valid private OnboardingImportContract onboardingImportContract;
}
