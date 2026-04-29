package it.pagopa.selfcare.mscore.model.onboarding;

import it.pagopa.selfcare.mscore.constant.TokenType;
import it.pagopa.selfcare.mscore.model.institution.Billing;
import it.pagopa.selfcare.mscore.model.institution.InstitutionUpdate;
import it.pagopa.selfcare.mscore.model.user.UserToOnboard;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.List;

@Data
public class OnboardingRequest {

    private String productId;
    private String productName;
    private List<UserToOnboard> users;
    private String institutionExternalId;
    private InstitutionUpdate institutionUpdate;
    private String pricingPlan;
    private Billing billingRequest;
    private Contract contract;

    private Boolean signContract;
    private TokenType tokenType;
    private String contractFilePath;
    private OffsetDateTime contractCreatedAt;
    private OffsetDateTime contractActivatedAt;
    private Boolean sendCompleteOnboardingEmail;

}
