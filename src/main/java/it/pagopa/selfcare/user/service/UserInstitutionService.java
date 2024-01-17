package it.pagopa.selfcare.user.service;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.user.controller.response.UserInstitutionResponse;

public interface UserInstitutionService {

    Uni<UserInstitutionResponse> findById(String id);
    Uni<UserInstitutionResponse> findByInstitutionId(String institutionId);
    Multi<UserInstitutionResponse> findByUserId(String userId);

}
