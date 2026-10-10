package it.pagopa.selfcare.onboarding.client.model;

import it.pagopa.selfcare.onboarding.common.OnboardingStatus;
import it.pagopa.selfcare.onboarding.common.WorkflowType;
import java.util.List;
import lombok.Data;

@Data
public class AttachmentTemplate {

    private StorageOrigin storageOrigin;
    private OnboardingStatus workflowState;
    private String name;
    private boolean mandatory;
    private boolean generated;
    private List<WorkflowType> workflowType;
    private int order;
    private String templatePath;
    private String templateVersion;
}
