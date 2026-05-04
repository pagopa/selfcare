package it.pagopa.selfcare.mscore.model.onboarding;

import lombok.Data;

@Data
public class ContractImported {
    private String fileName;
    private String filePath;
    private String contractType;
}
