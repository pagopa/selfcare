package it.pagopa.selfcare.onboarding.core;

public interface IamService {

    boolean hasIamUserPermission(String permission, String userId, String institutionId, String productId);
}
