package it.pagopa.selfcare.onboarding.service.impl;

import it.pagopa.selfcare.onboarding.common.PartyRole;
import it.pagopa.selfcare.onboarding.service.UserInstitutionRestService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.List;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.openapi.quarkus.core_json.model.OnboardedProductResponse;
import org.openapi.quarkus.user_json.api.InstitutionApi;
import org.openapi.quarkus.user_json.model.UserInstitutionResponse;

@ApplicationScoped
public class UserInstitutionRestServiceImpl implements UserInstitutionRestService {

  private final InstitutionApi userInstitutionApi;

  @Inject
  public UserInstitutionRestServiceImpl(@RestClient InstitutionApi userInstitutionApi) {
    this.userInstitutionApi = userInstitutionApi;
  }

  @Override
  public List<UserInstitutionResponse> getActiveManagersByInstitutionAndProduct(
    String institutionId,
    String productId,
    OnboardedProductResponse.StatusEnum status) {
    return userInstitutionApi.retrieveUserInstitutions(
      institutionId,
      null,
      List.of(productId),
      List.of(String.valueOf(PartyRole.MANAGER)),
      List.of(String.valueOf(status)),
      null);
  }
}
