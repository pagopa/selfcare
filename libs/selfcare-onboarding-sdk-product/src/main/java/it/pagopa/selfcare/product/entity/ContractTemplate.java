package it.pagopa.selfcare.product.entity;

import java.util.List;

public class ContractTemplate {

  private String contractTemplatePath;
  private String contractTemplateVersion;
  private List<AttachmentTemplate> attachments;

  public List<AttachmentTemplate> getAttachments() {
    return attachments;
  }

  public void setAttachments(List<AttachmentTemplate> attachments) {
    this.attachments = attachments;
  }

  public String getContractTemplatePath() {
    return contractTemplatePath;
  }

  public void setContractTemplatePath(String contractTemplatePath) {
    this.contractTemplatePath = contractTemplatePath;
  }

  public String getContractTemplateVersion() {
    return contractTemplateVersion;
  }

  public void setContractTemplateVersion(String contractTemplateVersion) {
    this.contractTemplateVersion = contractTemplateVersion;
  }
}
