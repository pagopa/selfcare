package it.pagopa.selfcare.onboarding.service.impl;

import it.pagopa.selfcare.onboarding.service.PdvUserRegistryService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.openapi.quarkus.user_registry_json.api.UserApi;
import org.openapi.quarkus.user_registry_json.model.UserResource;

@ApplicationScoped
public class PdvUserRegistryServiceImpl implements PdvUserRegistryService {

  private final UserApi userRegistryApi;

  @Inject
  public PdvUserRegistryServiceImpl(@RestClient UserApi userRegistryApi) {
    this.userRegistryApi = userRegistryApi;
  }

  @Override
  public UserResource getUserById(String fields, String userId) {
    return userRegistryApi.findByIdUsingGET(fields, userId);
  }
}
