package it.pagopa.selfcare.product.model.dto.response;

import it.pagopa.selfcare.product.model.enums.OnboardingDecisionReason;
import it.pagopa.selfcare.product.model.enums.WorkflowType;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Outcome of an onboarding decision. When {@code allowed} is {@code false} only {@code reason} and
 * {@code message} are set.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OnboardingDecisionResponse {
  private boolean allowed;
  private OnboardingDecisionReason reason;
  private String message;
  private WorkflowType workflowType;
  private List<RequiredDocumentResponse> requiredDocuments;
  private Integer expirationDays;
  private ProductResponse product;
}
