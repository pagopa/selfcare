package it.pagopa.selfcare.external_api.model.onboarding;

import it.pagopa.selfcare.onboarding.common.InstitutionType;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Data
@NoArgsConstructor
public class OnboardingImportData {

    private String institutionExternalId;
    private String productId;
    private String productName;
    private List<User> users;
    private OnboardingImportContract contractImported;
    private String contractPath;
    private String contractVersion;
    private Billing billing;
    private InstitutionUpdate institutionUpdate;
    private InstitutionType institutionType;
    private String origin;
    private String pricingPlan;

    public List<User> getUsers() {
        return Optional.ofNullable(users).orElse(Collections.emptyList());
    }
}
