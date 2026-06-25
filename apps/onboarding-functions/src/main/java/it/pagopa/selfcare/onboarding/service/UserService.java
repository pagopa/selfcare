package it.pagopa.selfcare.onboarding.service;

import it.pagopa.selfcare.onboarding.dto.UserMail;
import org.openapi.quarkus.core_json.model.OnboardedProductResponse;
import org.openapi.quarkus.user_json.model.SendMailDto;
import org.openapi.quarkus.user_json.model.UserInstitutionResponse;

import java.util.List;

public interface UserService {

    void deleteByIdAndInstitutionIdAndProductId(String institutionId, String productId);

    List<UserMail> findEmailByInstitutionAndProducts(String institutionId, List<String> products);

    List<UserInstitutionResponse> getActiveManagersByInstitutionAndProduct(
            String institutionId,
            String productId,
            OnboardedProductResponse.StatusEnum status);

    void sendMailRequest(String userId, SendMailDto sendMailDto);
}
