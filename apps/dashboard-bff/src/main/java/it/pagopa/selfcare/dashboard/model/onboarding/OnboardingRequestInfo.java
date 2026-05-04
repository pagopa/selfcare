package it.pagopa.selfcare.dashboard.model.onboarding;

import it.pagopa.selfcare.dashboard.model.institution.InstitutionInfo;
import it.pagopa.selfcare.dashboard.model.user.UserInfo;
import lombok.Data;

import java.util.List;

@Data
public class OnboardingRequestInfo {

    private InstitutionInfo institutionInfo;
    private UserInfo manager;
    private List<UserInfo> admins;
    private String productId;

}
