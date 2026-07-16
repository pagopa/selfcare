package it.pagopa.selfcare.onboarding.service;

public interface IamService {
    boolean hasIamUserPermission(String permission, String userId, String institutionId, String productId);
}
