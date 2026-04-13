package it.pagopa.selfcare.onboarding.connector;


import it.pagopa.selfcare.onboarding.connector.api.UserInstitutionConnector;
import it.pagopa.selfcare.onboarding.connector.model.userInstitution.UserInstitutionRequest;
import it.pagopa.selfcare.onboarding.connector.model.userInstitution.UserInstitutionResponse;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import jakarta.enterprise.context.ApplicationScoped;
import org.openapi.quarkus.user_json.api.InstitutionControllerApi;

@ApplicationScoped
@Slf4j
@Data
class MsUserInstitutionConnectorImpl implements UserInstitutionConnector {

  private final InstitutionControllerApi userInstitutionApiClient;
    public MsUserInstitutionConnectorImpl(@RestClient InstitutionControllerApi userInstitutionApiClient) {
        this.userInstitutionApiClient = userInstitutionApiClient;
    }

  @Override
  public List<UserInstitutionResponse> getInstitutionUsersByFilter(UserInstitutionRequest userInstitutionRequest) {
    log.trace("getInstitutionUsersByFilter start");
    List<UserInstitutionResponse> userInstitutionResponses = new ArrayList<>();

    log.debug("getInstitutionUsersByFilter institutionId = {}", userInstitutionRequest.getInstitutionId());

    List<org.openapi.quarkus.user_json.model.UserInstitutionResponse> response =
        userInstitutionApiClient
            .institutionsInstitutionIdUserInstitutionsGet(
                userInstitutionRequest.getInstitutionId(),
                userInstitutionRequest.getProductRoles(),
                userInstitutionRequest.getProducts(),
                userInstitutionRequest.getRoles(),
                userInstitutionRequest.getStates(),
                userInstitutionRequest.getUserId())
            .await()
            .indefinitely();

    assert response != null;
      log.debug("Found user: {}", response.size());

      response.forEach(currentElement -> {
          UserInstitutionResponse userInstitutionResponse = UserInstitutionResponse.builder()
              .id(currentElement.getId())
              .userId(currentElement.getUserId())
              .institutionId(currentElement.getInstitutionId())
              .institutionDescription(currentElement.getInstitutionDescription())
              .institutionRootName(currentElement.getInstitutionRootName())
              .userMailUuid(currentElement.getUserMailUuid())
              .build();
        userInstitutionResponses.add(userInstitutionResponse);
      });

    log.trace("getInstitutionUsersByFilter end");
    return userInstitutionResponses;
  }
}
