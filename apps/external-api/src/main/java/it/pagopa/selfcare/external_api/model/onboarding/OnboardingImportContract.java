package it.pagopa.selfcare.external_api.model.onboarding;

import lombok.Data;

import java.time.OffsetDateTime;

@Data
public class OnboardingImportContract {

    private String fileName;
    private String filePath;
    private String contractType;
    private OffsetDateTime createdAt;
    private OffsetDateTime activatedAt;

}
