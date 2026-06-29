package it.pagopa.selfcare.onboarding.connector.model.product;

import lombok.Data;

@Data
public class RequiredDocumentModel {

    private String id;
    private String name;
    private String labelKey;
    private Boolean required;
    private String mimeType;
    private Integer maxDocumentsRequired;
}
