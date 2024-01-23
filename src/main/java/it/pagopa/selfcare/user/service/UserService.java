package it.pagopa.selfcare.user.service;

import io.smallrye.mutiny.Multi;
import it.pagopa.selfcare.user.controller.response.UserProductResponse;

public interface UserService {
    Multi<UserProductResponse> getUserProductsByInstitution(String institutionId);
}
