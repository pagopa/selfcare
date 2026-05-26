package it.pagopa.selfcare.onboarding.service;

import org.openapi.quarkus.user_registry_json.model.UserResource;

public interface PdvUserRegistryService {
    UserResource getUserById(String fields, String userId);
}
