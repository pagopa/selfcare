package it.pagopa.selfcare.onboarding.service;

import java.util.List;

public interface UserService {

  void deleteByIdAndInstitutionIdAndProductId(String institutionId, String productId);

  List<String> findEmailUuidByInstitutionAndProducts(String institutionId, List<String> products);
}
