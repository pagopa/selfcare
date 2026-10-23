package it.pagopa.selfcare.product.model.dto.request;

import it.pagopa.selfcare.product.model.enums.InstitutionType;
import it.pagopa.selfcare.product.model.enums.OnboardingChannel;
import it.pagopa.selfcare.product.model.enums.Origin;
import it.pagopa.selfcare.product.model.enums.WorkflowType;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OnboardingDecisionRequest {
  @NotNull private InstitutionType institutionType;
  @NotNull private Origin origin;
  @NotNull private OnboardingChannel channel;

  /** Whether the institution onboards as an aggregator. */
  private Boolean aggregator;

  /**
   * Workflow imposed by the operation (e.g. CONFIRMATION, IMPORT). When absent the workflow is
   * resolved from the product configuration.
   */
  private WorkflowType workflowType;
}
