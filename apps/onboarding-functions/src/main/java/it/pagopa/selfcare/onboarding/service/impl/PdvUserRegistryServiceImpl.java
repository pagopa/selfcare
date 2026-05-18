package it.pagopa.selfcare.onboarding.service.impl;

import it.pagopa.selfcare.onboarding.service.PdvUserRegistryService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.openapi.quarkus.user_registry_json.api.UserApi;
import org.openapi.quarkus.user_registry_json.model.UserResource;

@ApplicationScoped
@Slf4j
public class PdvUserRegistryServiceImpl implements PdvUserRegistryService {

  private final UserApi userRegistryApi;

  @Inject
  public PdvUserRegistryServiceImpl(@RestClient UserApi userRegistryApi) {
    this.userRegistryApi = userRegistryApi;
  }

  @Override
  public UserResource getUserById(String fields, String userId) {
    log.debug("Retrieving PDV user: userId={}, fields={}", userId, fields);
    return userRegistryApi.findByIdUsingGET(fields, userId);
  }
}
