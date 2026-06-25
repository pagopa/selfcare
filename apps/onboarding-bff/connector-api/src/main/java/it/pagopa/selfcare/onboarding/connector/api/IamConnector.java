package it.pagopa.selfcare.onboarding.connector.api;

public interface IamConnector {

    boolean hasUserPermission(String permission, String userId, String institutionId, String productId);
}
