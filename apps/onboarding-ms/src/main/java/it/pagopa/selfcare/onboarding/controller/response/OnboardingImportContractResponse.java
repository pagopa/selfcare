package it.pagopa.selfcare.onboarding.controller.response;

import lombok.Data;

import java.time.OffsetDateTime;

@Data
public class OnboardingImportContractResponse {
    private String fileName;
    private String filePath;
    private String contractType;
    private OffsetDateTime createdAt;
}
