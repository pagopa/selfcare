package it.pagopa.selfcare.onboarding.service;

import java.util.List;

public interface UserService {

  List<String> findByInstitutionAndProduct(String institutionId, String productId);

  void deleteByIdAndInstitutionIdAndProductId(String institutionId, String productId);
}
