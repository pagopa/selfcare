package it.pagopa.selfcare.iam.service;

import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.iam.controller.request.SaveUserRequest;
import it.pagopa.selfcare.iam.entity.UserClaims;

public interface IamService {
    Uni<String> ping();
    Uni<UserClaims> saveUser(SaveUserRequest saveUserRequest);
}
