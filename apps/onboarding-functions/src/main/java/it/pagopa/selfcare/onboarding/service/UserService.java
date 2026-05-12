package it.pagopa.selfcare.onboarding.service;

public interface UserService {

  void deleteByIdAndInstitutionIdAndProductId(String institutionId, String productId);
}
