package it.pagopa.selfcare.external_api.model.onboarding;

import it.pagopa.selfcare.onboarding.common.InstitutionType;
import lombok.Data;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Data
public class PdaOnboardingData {

    private String injectionInstitutionType;
    private String institutionExternalId;
    private InstitutionType institutionType;
    private String digitalAddress;
    private String address;
    private String zipCode;
    private String origin;
    private String taxCode;
    private String productId;
    private String productName;
    private List<User> users;
    private String description;
    private Billing billing;

    private String contractPath;
    private String contractVersion;
    private Boolean sendCompleteOnboardingEmail;


    public List<User> getUsers() {
        return Optional.ofNullable(users).orElse(Collections.emptyList());
    }

}
