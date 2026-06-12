package it.pagopa.selfcare.external_api.model.onboarding;

import lombok.Data;

import java.util.List;

@Data
public class OnboardingImportInstitutionRequest {

    private String productId;
    private String productName;
    private List<User> users;
    private String institutionExternalId;
    private InstitutionUpdate institutionUpdate;
    private String pricingPlan;
    private Billing billing;
    private OnboardingImportContract contractImported;
    private OnboardingContract contract;
    private Boolean sendCompleteOnboardingEmail;

}
