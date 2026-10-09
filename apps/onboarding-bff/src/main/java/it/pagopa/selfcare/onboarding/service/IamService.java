package it.pagopa.selfcare.onboarding.service;

import io.smallrye.mutiny.Uni;

public interface IamService {
    Uni<Boolean> hasIamUserPermission(String permission, String userId, String institutionId, String productId);
}
