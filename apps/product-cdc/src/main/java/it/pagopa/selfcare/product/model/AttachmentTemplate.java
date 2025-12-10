package it.pagopa.selfcare.product.model;

import it.pagopa.selfcare.product.model.enums.OnboardingStatus;
import it.pagopa.selfcare.product.model.enums.WorkflowType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AttachmentTemplate {
    private String templatePath;
    private String templateVersion;
    private String name;
    private boolean mandatory;
    private boolean generated;
    private List<WorkflowType> workflowType;
    private OnboardingStatus workflowState;
    private int order;
}
