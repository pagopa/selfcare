package it.pagopa.selfcare.onboarding.client.model;

import java.util.List;
import lombok.Data;

@Data
public class ContractTemplate {

    private String contractTemplatePath;
    private String contractTemplateVersion;
    private List<AttachmentTemplate> attachments;
}
