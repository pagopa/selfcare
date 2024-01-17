package it.pagopa.selfcare.user.service;

import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.user.controller.response.UserInfoResponse;

public interface UserInfoService {

    Uni<UserInfoResponse> findById(String id);
    Uni<UserInfoResponse> findByUserId(String userId);

}
