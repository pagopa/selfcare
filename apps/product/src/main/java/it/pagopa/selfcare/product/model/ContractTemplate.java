package it.pagopa.selfcare.product.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ContractTemplate {
    private String contractTemplatePath;
    private String contractTemplateVersion;
    private List<AttachmentTemplate> attachments;
}
