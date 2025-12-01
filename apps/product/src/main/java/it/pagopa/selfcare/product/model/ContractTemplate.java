package it.pagopa.selfcare.product.model;

import it.pagopa.selfcare.product.model.enums.OnboardingType;
import it.pagopa.selfcare.product.model.enums.WorkflowType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ContractTemplate {
    private String contractId;
    private OnboardingType onboardingType;
    private boolean enabled;
    private String institutionType;
    private String path;
    private String version;
    private int order;
    private boolean generated;
    private boolean mandatory;
    private String name;
    private String workflowState;
    private List<WorkflowType> workflowType;
}
