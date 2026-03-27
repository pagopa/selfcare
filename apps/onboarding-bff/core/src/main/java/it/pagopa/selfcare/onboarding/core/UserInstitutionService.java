package it.pagopa.selfcare.onboarding.core;

public interface UserInstitutionService {

 boolean verifyAllowedUserInstitution(String institutionId, String product, String uid);

}
