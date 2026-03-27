package it.pagopa.selfcare.onboarding.connector.api;

import it.pagopa.selfcare.onboarding.connector.model.userInstitution.UserInstitutionRequest;
import it.pagopa.selfcare.onboarding.connector.model.userInstitution.UserInstitutionResponse;
import java.util.List;

public interface UserInstitutionConnector {

  List<UserInstitutionResponse> getInstitutionUsersByFilter(
      UserInstitutionRequest userInstitutionRequest);
}
