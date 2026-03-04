package it.pagopa.selfcare.product.model;

import it.pagopa.selfcare.product.model.enums.ContractType;
import it.pagopa.selfcare.product.model.enums.InstitutionType;
import it.pagopa.selfcare.product.model.enums.OnboardingType;
import it.pagopa.selfcare.product.model.enums.WorkflowType;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ContractTemplateConfig {

  @Builder.Default private String contractId = UUID.randomUUID().toString();

  private OnboardingType onboardingType;
  private boolean enabled;
  private InstitutionType institutionType;
  private ContractType contractType;
  private String path;
  private String version;
  private int order;
  private boolean generated;
  private boolean mandatory;
  private String name;
  private String workflowState;
  private List<WorkflowType> workflowType;
}
