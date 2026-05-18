package it.pagopa.selfcare.onboarding.service;

import it.pagopa.selfcare.onboarding.dto.UserMail;

import java.util.List;

public interface UserService {

  void deleteByIdAndInstitutionIdAndProductId(String institutionId, String productId);

  List<UserMail> findEmailByInstitutionAndProducts(String institutionId, List<String> products);
}
