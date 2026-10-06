package it.pagopa.selfcare.onboarding.connector.model.product;

import lombok.Data;

import java.util.List;

@Data
public class ContractTemplate {

    private String contractTemplatePath;
    private String contractTemplateVersion;
    private List<AttachmentTemplate> attachments;
}

