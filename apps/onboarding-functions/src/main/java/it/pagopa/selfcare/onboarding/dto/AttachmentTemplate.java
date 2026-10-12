package it.pagopa.selfcare.onboarding.dto;

import it.pagopa.selfcare.onboarding.common.OnboardingStatus;
import it.pagopa.selfcare.onboarding.common.WorkflowType;
import java.util.List;

/** Keeps the historical Durable Functions JSON shape for attachment activity payloads. */
public class AttachmentTemplate {

  private String templatePath;
  private String templateVersion;
  private String name;
  private boolean mandatory;
  private boolean generated;
  private List<WorkflowType> workflowType;
  private OnboardingStatus workflowState;
  private int order;

  public String getTemplatePath() { return templatePath; }
  public void setTemplatePath(String templatePath) { this.templatePath = templatePath; }
  public String getTemplateVersion() { return templateVersion; }
  public void setTemplateVersion(String templateVersion) { this.templateVersion = templateVersion; }
  public String getName() { return name; }
  public void setName(String name) { this.name = name; }
  public boolean isMandatory() { return mandatory; }
  public void setMandatory(boolean mandatory) { this.mandatory = mandatory; }
  public boolean isGenerated() { return generated; }
  public void setGenerated(boolean generated) { this.generated = generated; }
  public List<WorkflowType> getWorkflowType() { return workflowType; }
  public void setWorkflowType(List<WorkflowType> workflowType) { this.workflowType = workflowType; }
  public OnboardingStatus getWorkflowState() { return workflowState; }
  public void setWorkflowState(OnboardingStatus workflowState) { this.workflowState = workflowState; }
  public int getOrder() { return order; }
  public void setOrder(int order) { this.order = order; }
}
