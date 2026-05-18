package it.pagopa.selfcare.onboarding.service;

import java.util.List;
import org.openapi.quarkus.core_json.model.OnboardedProductResponse;
import org.openapi.quarkus.user_json.model.UserInstitutionResponse;

public interface UserInstitutionRestService {
  List<UserInstitutionResponse> getActiveManagersByInstitutionAndProduct(
    String institutionId,
    String productId,
    OnboardedProductResponse.StatusEnum status);
}
